package org.yah.tools.index.lucene.mapper;

public interface EntityDocumentMapper<T> extends DocumentMapper<T> {

    String getIdField();

    String getElementId(T element);

}
