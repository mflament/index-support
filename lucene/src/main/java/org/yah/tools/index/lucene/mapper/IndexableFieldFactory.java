package org.yah.tools.index.lucene.mapper;

import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;

import java.util.Optional;

public interface IndexableFieldFactory<V> {

    IndexableFieldFactory<String> string = (name, value) -> new StringField(name, value, Field.Store.NO);
    IndexableFieldFactory<String> text = (name, value) -> new TextField(name, value, Field.Store.NO);

    IndexableFieldFactory<Integer> intPoint = IntPoint::new;
    IndexableFieldFactory<Long> longPoint = LongPoint::new;
    IndexableFieldFactory<Float> floatPoint = FloatPoint::new;
    IndexableFieldFactory<Double> doublePoint = DoublePoint::new;

    IndexableFieldFactory<Range<Integer>> intRange = IndexableFieldFactory::intRange;
    IndexableFieldFactory<Range<Long>> longRange = IndexableFieldFactory::longRange;
    IndexableFieldFactory<Range<Float>> floatRange = IndexableFieldFactory::floatRange;
    IndexableFieldFactory<Range<Double>> doubleRange = IndexableFieldFactory::doubleRange;

    IndexableFieldFactory<String> stringDocValues = (name, s) -> new BinaryDocValuesField(name, new BytesRef(s));
    IndexableFieldFactory<Long> longDocValues = NumericDocValuesField::new;
    IndexableFieldFactory<Float> floatDocValues = FloatDocValuesField::new;
    IndexableFieldFactory<Double> doubleDocValues = DoubleDocValuesField::new;

    static <V> IndexableFieldFactory<V> nullable(IndexableFieldFactory<V> factory) {
        return (name, value) -> value == null ? null : factory.create(name, value);
    }

    static <V> IndexableFieldFactory<Optional<V>> optional(IndexableFieldFactory<V> factory) {
        return (name, value) -> value.map(v -> factory.create(name, v)).orElse(null);
    }

    @SuppressWarnings("unchecked")
    static <V extends Number> IndexableFieldFactory<V> point(Class<V> type) {
        IndexableFieldFactory<?> factory;
        if (type == Integer.class) factory = intPoint;
        else if (type == Long.class) factory = longPoint;
        else if (type == Float.class) factory = floatPoint;
        else if (type == Double.class) factory = doublePoint;
        else throw new IllegalArgumentException("Unsupported point type " + type.getName());
        return (IndexableFieldFactory<V>) factory;
    }

    static <V extends Number> IndexableFieldFactory<Range<V>> range(Class<V> type) {
        IndexableFieldFactory<?> factory;
        if (type == Integer.class) factory = intRange;
        else if (type == Long.class) factory = longRange;
        else if (type == Float.class) factory = floatRange;
        else if (type == Double.class) factory = doubleRange;
        else throw new IllegalArgumentException("Unsupported range type " + type.getName());
        //noinspection unchecked
        return (IndexableFieldFactory<Range<V>>) factory;
    }

    static <V> IndexableFieldFactory<V> docValues(Class<V> type) {
        IndexableFieldFactory<?> factory;
        if (type == String.class) factory = stringDocValues;
        else if (type == Integer.class) factory = longDocValues;
        else if (type == Long.class) factory = longDocValues;
        else if (type == Float.class) factory = floatDocValues;
        else if (type == Double.class) factory = doubleDocValues;
        else throw new IllegalArgumentException("Unsupported docValues type " + type.getName());
        //noinspection unchecked
        return (IndexableFieldFactory<V>) factory;
    }

    IndexableField create(String name, V value);

    private static IndexableField doubleRange(String name, Range<Double> range) {
        return new DoubleRange(name, new double[]{range.min()}, new double[]{range.max()});
    }

    private static IndexableField floatRange(String name, Range<Float> range) {
        return new FloatRange(name, new float[]{range.min()}, new float[]{range.max()});
    }

    private static IndexableField longRange(String name, Range<Long> range) {
        return new LongRange(name, new long[]{range.min()}, new long[]{range.max()});
    }

    private static IndexableField intRange(String name, Range<Integer> range) {
        return new IntRange(name, new int[]{range.min()}, new int[]{range.max()});
    }

}
