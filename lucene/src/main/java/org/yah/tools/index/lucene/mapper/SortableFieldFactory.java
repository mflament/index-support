package org.yah.tools.index.lucene.mapper;

import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;

public interface SortableFieldFactory<V> extends IndexableFieldFactory<V> {

    SortableFieldFactory<String> sortedText = create((name, value) ->
            new SortedDocValuesField(name, new BytesRef(value)), SortField.Type.STRING);
    SortableFieldFactory<Integer> sortedInteger = create(SortableFieldFactory::sortedInt, SortField.Type.INT);
    SortableFieldFactory<Long> sortedLong = create(SortedNumericDocValuesField::new, SortField.Type.LONG);
    SortableFieldFactory<Float> sortedFloat = create(SortableFieldFactory::sortedFloat, SortField.Type.FLOAT);
    SortableFieldFactory<Double> sortedDouble = create(SortableFieldFactory::sortedDouble, SortField.Type.DOUBLE);

    @SuppressWarnings("unchecked")
    static <V extends Number> SortableFieldFactory<V> number(Class<V> type) {
        IndexableFieldFactory<? extends Number> factory;
        if (type == Integer.class) factory = sortedInteger;
        else if (type == Long.class) factory = sortedLong;
        else if (type == Float.class) factory = sortedFloat;
        else if (type == Double.class) factory = sortedDouble;
        else throw new IllegalArgumentException("Unsupported sortable value type " + type.getName());
        return (SortableFieldFactory<V>) factory;
    }

    SortField.Type getSortType();

    private static <V> SortableFieldFactory<V> create(IndexableFieldFactory<V> factory, SortField.Type type) {
        return new SortableFieldFactory<>() {
            @Override
            public SortField.Type getSortType() {
                return type;
            }

            @Override
            public IndexableField create(String name, V value) {
                return factory.create(name, value);
            }
        };
    }

    private static SortedNumericDocValuesField sortedInt(String name, Integer value) {
        return new SortedNumericDocValuesField(name, value);
    }

    private static SortedNumericDocValuesField sortedFloat(String name, Float value) {
        return new SortedNumericDocValuesField(name, NumericUtils.floatToSortableInt(value));
    }

    private static SortedNumericDocValuesField sortedDouble(String name, Double value) {
        return new SortedNumericDocValuesField(name, NumericUtils.doubleToSortableLong(value));
    }

}
