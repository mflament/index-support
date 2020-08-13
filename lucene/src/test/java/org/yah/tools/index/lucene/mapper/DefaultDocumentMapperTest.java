package org.yah.tools.index.lucene.mapper;

import java.util.Collection;

public class DefaultDocumentMapperTest {

    public static <T> Collection<IndexedField<T>> getIndexedFields(DefaultDocumentMapper<T> mapper) {
        return mapper.getIndexedFields();
    }

}
