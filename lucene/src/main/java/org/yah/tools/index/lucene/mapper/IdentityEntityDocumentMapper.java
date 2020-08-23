package org.yah.tools.index.lucene.mapper;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import java.util.Collections;
import java.util.Map;

public class IdentityEntityDocumentMapper implements EntityDocumentMapper<Document> {

    public static DocumentMapper<Document> documentMapper(IndexableFieldType defaultType) {
        return new IdentityEntityDocumentMapper(null, Collections.emptyMap(), defaultType);
    }

    public static DocumentMapper<Document> documentMapper(Map<String, IndexableFieldType> fieldTypes, IndexableFieldType defaultType) {
        return new IdentityEntityDocumentMapper(null, fieldTypes, defaultType);
    }

    public static EntityDocumentMapper<Document> entityMapper(String idField, IndexableFieldType defaultType) {
        return new IdentityEntityDocumentMapper(idField, Collections.emptyMap(), defaultType);
    }

    public static EntityDocumentMapper<Document> entityMapper(String idField, Map<String, IndexableFieldType> fieldTypes, IndexableFieldType defaultType) {
        return new IdentityEntityDocumentMapper(idField, fieldTypes, defaultType);
    }

    private final String idField;

    private final Map<String, IndexableFieldType> fieldTypes;

    private final IndexableFieldType defaultType;

    private IdentityEntityDocumentMapper(String idField, Map<String, IndexableFieldType> fieldTypes, IndexableFieldType defaultType) {
        this.idField = idField;
        this.fieldTypes = Map.copyOf(fieldTypes);
        this.defaultType = defaultType;
    }

    @Override
    public Document toDocument(Document element) {
        return element;
    }

    @Override
    public Document toElement(Document document) {
        return document;
    }

    @Override
    public IndexableFieldType getFieldType(String field) {
        final IndexableFieldType type = fieldTypes.get(field);
        if (type == null && defaultType == null)
            throw new IllegalArgumentException("unknown field " + field);
        return defaultType;
    }

    @Override
    public String getIdField() {
        return idField;
    }

    @Override
    public String getElementId(Document element) {
        if (idField == null)
            throw new UnsupportedOperationException("no idField");
        final IndexableField field = element.getField(idField);
        if (field == null)
            throw new IllegalArgumentException("document " + element + " has no id field " + idField);
        return field.stringValue();
    }
}
