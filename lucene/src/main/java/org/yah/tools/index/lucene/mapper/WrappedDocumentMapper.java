package org.yah.tools.index.lucene.mapper;

import org.apache.lucene.document.Document;

public class WrappedDocumentMapper<T> implements EntityDocumentMapper<T> {

    private final DocumentMapper<T> delegate;

    public WrappedDocumentMapper(DocumentMapper<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getIdField() {
        throw new UnsupportedOperationException("not an EntityDocumentMapper");
    }

    @Override
    public String getElementId(T element) {
        throw new UnsupportedOperationException("not an EntityDocumentMapper");
    }

    @Override
    public Document toDocument(T element) {
        return delegate.toDocument(element);
    }

    @Override
    public T toElement(Document document) {
        return delegate.toElement(document);
    }

    @Override
    public IndexableFieldType getFieldType(String field) {
        return delegate.getFieldType(field);
    }

}
