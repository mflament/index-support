package org.yah.tools.index.lucene.mapper;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.SortField;

import javax.print.Doc;
import java.util.Optional;

public interface DocumentMapper<T> {

    Document toDocument(T element);

    T toElement(Document document);

    IndexableFieldType getFieldType(String field);

}
