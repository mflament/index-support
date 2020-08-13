package org.yah.tools.index.lucene.mapper.annotations;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.function.Function;

class FieldIndexedFieldSource<T, V> implements IndexedFieldSource<T, V> {

    private final Field field;

    public FieldIndexedFieldSource(Field field) {
        this.field = field;
        field.setAccessible(true);
    }

    @Override
    public String fieldName() {
        return field.getName();
    }

    @Override
    public Type type() {
        return field.getGenericType();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Function<T, V> accessor() {
        return o -> {
            try {
                return (V) field.get(o);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        };
    }

    @Override
    public String toString() {
        return field.toString();
    }
}
