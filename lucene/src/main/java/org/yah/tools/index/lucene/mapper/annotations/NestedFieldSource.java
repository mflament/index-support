package org.yah.tools.index.lucene.mapper.annotations;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Function;

public class NestedFieldSource<T, U, V> implements IndexedFieldSource<T, V> {

    private final IndexedFieldSource<T, U> beanAccesor;
    private final IndexedFieldSource<U, V> fieldAccessor;

    public NestedFieldSource(IndexedFieldSource<T, U> beanAccesor, IndexedFieldSource<U, V> fieldAccessor) {
        this.beanAccesor = Objects.requireNonNull(beanAccesor);
        this.fieldAccessor = Objects.requireNonNull(fieldAccessor);
    }

    @Override
    public String fieldName() {
        return fieldAccessor.fieldName();
    }

    @Override
    public Type type() {
        return fieldAccessor.type();
    }

    @Override
    public Function<T, V> accessor() {
        return beanAccesor.accessor().andThen(v -> v == null ? null : fieldAccessor.accessor().apply(v));
    }
}
