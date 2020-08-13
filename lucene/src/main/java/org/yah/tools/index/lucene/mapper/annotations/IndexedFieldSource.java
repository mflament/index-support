package org.yah.tools.index.lucene.mapper.annotations;

import java.lang.reflect.Type;
import java.util.function.Function;

public interface IndexedFieldSource<T, V> {

    String fieldName();

    Type type();

    Function<T, V> accessor();

}
