package org.yah.tools.index.lucene.mapper;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.SortField;

import java.util.Optional;

public interface DocumentMapper<T> {

    void toDocument(T element, Document document);

    T toElement(Document document);

    String getElementId(T element);

    Optional<SortField.Type> getSortType(String field);

}
