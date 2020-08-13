package org.yah.tools.index.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.*;
import org.yah.tools.index.lucene.LuceneIndexReader.ReaderInstance;
import org.yah.tools.index.lucene.mapper.DocumentMapper;
import org.yah.tools.index.query.IndexCursor;
import org.yah.tools.index.query.ScoredElement;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class LuceneIndexCursor<T> implements IndexCursor<T>, Spliterator<T> {

    public static <T> IndexCursor<T> create(ReaderInstance readerInstance,
                                            DocumentMapper<T> documentMapper,
                                            LuceneIndexQuery query, int batchSize) {
        BiFunction<ScoreDoc, Document, T> mapper = (ignore, document) -> documentMapper.toElement(document);
        return new LuceneIndexCursor<>(readerInstance,
                query, query.createLuceneSort(documentMapper),
                mapper, false, batchSize);
    }

    public static <T> IndexCursor<ScoredElement<T>> createScored(ReaderInstance readerInstance,
                                                                 DocumentMapper<T> documentMapper,
                                                                 LuceneIndexQuery query, int batchSize) {
        BiFunction<ScoreDoc, Document, ScoredElement<T>> mapper = (scoreDoc, document) ->
                new ScoredElement<>(documentMapper.toElement(document), scoreDoc.score);
        return new LuceneIndexCursor<>(readerInstance,
                query, query.createLuceneSort(documentMapper),
                mapper, true, batchSize);
    }

    private final ReaderInstance readerInstance;

    private final LuceneIndexQuery query;
    private final Sort sort;
    private final boolean doScores;

    private final BiFunction<ScoreDoc, Document, T> mapper;
    private final int batchSize;

    private ScoreDoc[] results;
    private TotalHits totalHits;
    private int index;
    private int remaining;

    LuceneIndexCursor(ReaderInstance readerInstance,
                      LuceneIndexQuery query,
                      Sort luceneSort,
                      BiFunction<ScoreDoc, Document, T> mapper,
                      boolean doScores,
                      int batchSize) {
        this.readerInstance = readerInstance;
        this.query = query;
        this.sort = luceneSort;
        this.batchSize = batchSize;
        this.mapper = mapper;
        this.doScores = doScores;
        remaining = query.getLimit();
        results = search();
    }

    @Override
    public Stream<T> stream() {
        return StreamSupport.stream(this, false);
    }

    @Override
    public long getTotalHits() {
        if (totalHits.relation == TotalHits.Relation.EQUAL_TO)
            return totalHits.value;

        try {
            return readerInstance.searcher.count(query.getQuery());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public long getMinTotalHits() {
        return totalHits.value;
    }

    @Override
    public void close() {
        readerInstance.close();
    }

    @Override
    public boolean hasNext() {
        return results.length > 0;
    }

    @Override
    public T next() {
        if (!hasNext()) throw new NoSuchElementException();

        final ScoreDoc scoreDoc = results[index];
        final IndexSearcher searcher = readerInstance.searcher;

        final Document document;
        try {
            final Set<String> projection = query.getProjection();
            if (projection.isEmpty())
                document = searcher.doc(scoreDoc.doc);
            else
                document = searcher.doc(scoreDoc.doc, projection);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        final T element = mapper.apply(scoreDoc, document);
        index++;
        if (index == results.length) {
            results = search();
            index = 0;
        }
        return element;
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        IndexCursor.super.forEachRemaining(action);
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (hasNext()) {
            action.accept(next());
            return true;
        }
        return false;
    }

    @Override
    public Spliterator<T> trySplit() {

        return null;
    }

    @Override
    public long estimateSize() {
        return Math.min(getMinTotalHits(), remaining);
    }

    @Override
    public int characteristics() {
        return Spliterator.DISTINCT |
                Spliterator.IMMUTABLE |
                Spliterator.ORDERED;
    }

    private ScoreDoc[] search() {
        try {
            if (remaining > 0) {
                final IndexSearcher searcher = readerInstance.searcher;
                ScoreDoc lastDoc = null;
                if (results != null) {
                    lastDoc = results[results.length - 1];
                } else if (query.getSkip() > 0) {
                    final TopFieldDocs skippedDocs = searcher.search(query.getQuery(),
                            query.getSkip(),
                            sort,
                            false);
                    if (skippedDocs.scoreDocs.length > 0)
                        lastDoc = skippedDocs.scoreDocs[skippedDocs.scoreDocs.length - 1];
                    else {
                        remaining = 0;
                        return new ScoreDoc[0];
                    }
                }

                int maxHits = Math.min(remaining, batchSize);
                final TopFieldDocs docs = searcher.searchAfter(lastDoc,
                        query.getQuery(), maxHits, sort, doScores);
                totalHits = docs.totalHits;
                remaining -= docs.scoreDocs.length;
                return docs.scoreDocs;
            } else {
                return new ScoreDoc[0];
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
