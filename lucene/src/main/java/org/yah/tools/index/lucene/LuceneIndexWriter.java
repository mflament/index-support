package org.yah.tools.index.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yah.tools.index.IndexException;
import org.yah.tools.index.query.IndexQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

class LuceneIndexWriter<T> extends LuceneSupportObject<T> implements org.yah.tools.index.IndexWriter<T> {


    public static Term idTerm(String id) {
        return new Term(LuceneIndex.ID_FIELD, id);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneIndexWriter.class);

    Directory directory;
    IndexWriter indexWriter;

    public LuceneIndexWriter(LuceneIndex<T> index) {
        super(index);
        open();
    }

    @Override
    public void add(Collection<T> elements) {
        final Collection<Document> documents = elements.stream()
                .map(this::toDocument)
                .collect(Collectors.toCollection(() -> new ArrayList<>(elements.size())));
        try {
            indexWriter.addDocuments(documents);
        } catch (IOException e) {
            throw new IndexException(e);
        }
    }

    @Override
    public void update(Collection<T> elements) {
        elements.forEach(this::update);
    }

    @Override
    public void update(T element) {
        final Term id = extractId(element);
        try {
            indexWriter.updateDocument(id, toDocument(element));
        } catch (IOException e) {
            throw new IndexException(e);
        }
    }

    @Override
    public void delete(Collection<String> ids) {
        final Term[] terms = ids.stream()
                .distinct()
                .map(LuceneIndexWriter::idTerm)
                .toArray(Term[]::new);
        try {
            indexWriter.deleteDocuments(terms);
        } catch (IOException e) {
            throw new IndexException(e);
        }
    }

    @Override
    public void delete(IndexQuery query) {
        LuceneIndexQuery luceneIndexQuery = (LuceneIndexQuery) query;
        try {
            indexWriter.deleteDocuments(luceneIndexQuery.getQuery());
        } catch (IOException e) {
            throw new IndexException(e);
        }
    }

    @Override
    public void clear() {
        try {
            indexWriter.deleteAll();
        } catch (IOException e) {
            throw new IndexException(e);
        }
    }

    @Override
    public void close() {
        closeSafely(indexWriter);
        closeSafely(directory);
        LOGGER.debug("closed writer {} for path {}", System.identityHashCode(indexWriter), index.path);
    }

    private Term extractId(T element) {
        return idTerm(index.documentMapper.getElementId(element));
    }

    private Document toDocument(T element) {
        Document document = new Document();
        index.documentMapper.toDocument(element, document);
        document.add(new StringField(LuceneIndex.ID_FIELD,
                index.documentMapper.getElementId(element),
                Field.Store.NO));
        return document;
    }

    private void open() {
        try {
            directory = index.openDirectory();
            indexWriter = new IndexWriter(directory, new IndexWriterConfig(index.analyzer));
            LOGGER.debug("opened writer {} for path {}", System.identityHashCode(indexWriter), index.path);
        } catch (IOException e) {
            throw new IndexException(e);
        }
    }

}
