package org.yah.tools.index.lucene.mapper;

import org.apache.lucene.index.IndexableField;

public interface IndexableFieldFactory<V> {

    IndexableField create(String name, V value);

    IndexableFieldType getType();

}
