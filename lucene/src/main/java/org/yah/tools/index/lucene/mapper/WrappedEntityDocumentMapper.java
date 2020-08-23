package org.yah.tools.index.lucene.mapper;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;

public class WrappedEntityDocumentMapper<T> implements EntityDocumentMapper<T> {

    private final EntityDocumentMapper<T> delegate;

    public WrappedEntityDocumentMapper(EntityDocumentMapper<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getIdField() {
        return delegate.getIdField();
    }

    @Override
    public String getElementId(T element) {
        return delegate.getElementId(element);
    }

    @Override
    public Document toDocument(T element) {
        final Document document = delegate.toDocument(element);
        document.add(new StringField(getIdField(), getElementId(element), Field.Store.NO));
        return document;
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
