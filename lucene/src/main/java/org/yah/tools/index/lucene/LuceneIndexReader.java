package org.yah.tools.index.lucene;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yah.tools.index.EntityIndexReader;
import org.yah.tools.index.Index.ProgressCallback;
import org.yah.tools.index.IndexException;
import org.yah.tools.index.query.IndexCursor;
import org.yah.tools.index.query.IndexQuery;
import org.yah.tools.index.query.IndexQueryBuilder;
import org.yah.tools.index.query.ScoredElement;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;

class LuceneIndexReader<T> extends LuceneSupportObject<T> implements EntityIndexReader<T>, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneIndexReader.class);

    private final IndexWriter indexWriter;
    private final Directory directory;
    private ReaderInstance readerInstance;

    LuceneIndexReader(LuceneIndex<T> index) {
        super(index);
        indexWriter = null;
        directory = index.openDirectory();
        LOGGER.debug("opened reader directory {}", System.identityHashCode(directory));
    }

    LuceneIndexReader(LuceneIndexWriter<T> writer) {
        super(writer.index);
        indexWriter = writer.indexWriter;
        directory = null;
    }

    @Override
    public int count() {
        try (ReaderInstance readerInstance = open()) {
            return readerInstance.reader.numDocs();
        }
    }

    @Override
    public IndexQueryBuilder prepareQuery() {
        return new LuceneIndexQueryBuilder<>(index);
    }

    @Override
    public Collection<T> find(Collection<String> ids) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        ids.stream()
                .map(this::idTerm)
                .map(TermQuery::new)
                .map(termQuery -> new BooleanClause(termQuery, SHOULD))
                .forEach(builder::add);
        try (final IndexCursor<T> cursor = query(new LuceneIndexQuery(builder.build()), ids.size())) {
            final Collection<T> res = new ArrayList<>((int) cursor.getMinTotalHits());
            cursor.forEachRemaining(res::add);
            return res;
        }
    }

    @Override
    public IndexCursor<T> query(IndexQuery query, int batchSize) {
        final ReaderInstance readerInstance = open();
        return LuceneIndexCursor.create(readerInstance, index.documentMapper, LuceneIndexQuery.cast(query), batchSize);
    }

    @Override
    public List<T> list(IndexQuery query) {
        try (IndexCursor<T> cursor = query(query, 1000)) {
            List<T> res = new ArrayList<>((int) cursor.getMinTotalHits());
            cursor.forEachRemaining(res::add);
            return res;
        }
    }

    @Override
    public Optional<T> findFirst(IndexQuery query) {
        try (IndexCursor<T> cursor = query(query, 1)) {
            if (cursor.hasNext()) return Optional.of(cursor.next());
            return Optional.empty();
        }
    }

    @Override
    public int count(IndexQuery query) {
        final LuceneIndexQuery luceneQuery = LuceneIndexQuery.cast(query);
        try (ReaderInstance readerInstance = open()) {
            return readerInstance.searcher.count(luceneQuery.getQuery());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public IndexCursor<ScoredElement<T>> scoredQuery(IndexQuery query, int batchSize) {
        final ReaderInstance readerInstance = open();

        return LuceneIndexCursor.createScored(readerInstance, index.documentMapper,
                LuceneIndexQuery.cast(query), batchSize);
    }

    @Override
    public List<ScoredElement<T>> scoredList(IndexQuery query) {
        try (IndexCursor<ScoredElement<T>> cursor = scoredQuery(query, 1000)) {
            List<ScoredElement<T>> res = new ArrayList<>((int) cursor.getMinTotalHits());
            cursor.forEachRemaining(res::add);
            return res;
        }
    }

    @Override
    public synchronized void close() {
        if (readerInstance != null) {
            readerInstance.forceClose();
            readerInstance = null;
        }

        if (directory != null) {
            closeSafely(directory);
            LOGGER.debug("closed reader directory {}", System.identityHashCode(directory));
        }
    }

    public <V> void reindex(org.yah.tools.index.IndexWriter<V> target, Function<T, V> mapper, ProgressCallback progressCallback) {
        final int count = count();
        int batchSize = 50000;
        int chunkCount = (int) Math.ceil(count / (float) batchSize);

        final ProgressCallback pc = progressCallback == null ? ProgressCallback.NOOP : progressCallback;
        final List<V> chunk = new ArrayList<>(batchSize);
        pc.setExpected(count + chunkCount);
        if (count == 0)
            return;

        try (final IndexCursor<T> cursor = query(IndexQuery.ALL, 5000)) {
            cursor.stream().map(mapper).forEach(t -> {
                chunk.add(t);
                pc.addCompleted();
                if (chunk.size() == batchSize) {
                    target.add(chunk);
                    pc.addCompleted();
                    chunk.clear();
                }
            });
            if (!chunk.isEmpty()) {
                target.add(chunk);
                pc.addCompleted();
            }
        }
    }

    private synchronized ReaderInstance open() {
        DirectoryReader newReader;
        try {
            if (indexWriter != null) {
                newReader = open(indexWriter);
            } else {
                newReader = open(directory);
            }
        } catch (IOException e) {
            throw new IndexException(e);
        }

        DirectoryReader currentReader = readerInstance == null ? null : readerInstance.reader;
        if (newReader != currentReader) {
            if (readerInstance != null)
                readerInstance.close();
            readerInstance = new ReaderInstance(newReader);
            readerInstance.register();
        }
        readerInstance.register();
        return readerInstance;
    }

    private DirectoryReader open(Directory directory) throws IOException {
        if (readerInstance == null) {
            final DirectoryReader res = DirectoryReader.open(directory);
            LOGGER.debug("opened new reader {} for path {}",
                    System.identityHashCode(res),
                    index.path);
            return res;
        }

        DirectoryReader newReader = DirectoryReader.openIfChanged(readerInstance.reader);
        if (newReader == null) {
            return readerInstance.reader;
        }
        LOGGER.debug("opened updated reader {} for path {}",
                System.identityHashCode(newReader),
                index.path);
        return newReader;
    }

    private DirectoryReader open(IndexWriter indexWriter) throws IOException {
        if (readerInstance == null) {
            final DirectoryReader res = DirectoryReader.open(indexWriter);
            LOGGER.debug("opening new NRT reader {} for writer {}",
                    System.identityHashCode(res),
                    System.identityHashCode(indexWriter));
            return res;
        }

        DirectoryReader newReader = DirectoryReader.openIfChanged(readerInstance.reader, indexWriter);
        if (newReader == null)
            return readerInstance.reader;
        LOGGER.debug("opening updated NRT reader {} for writer {}",
                System.identityHashCode(newReader),
                System.identityHashCode(indexWriter));

        return newReader;
    }

    public static class ReaderInstance implements AutoCloseable {

        final DirectoryReader reader;
        final IndexSearcher searcher;

        private final AtomicInteger reference = new AtomicInteger(0);

        public ReaderInstance(DirectoryReader reader) {
            this.reader = reader;
            this.searcher = new IndexSearcher(reader);
        }

        @Override
        public void close() {
            if (reference.decrementAndGet() == 0) {
                forceClose();
            }
        }

        public void forceClose() {
            LOGGER.debug("Closing reader {}", System.identityHashCode(reader));
            closeSafely(reader);
        }

        void register() {
            reference.incrementAndGet();
        }
    }

}
