package org.yah.tools.index.lucene.mapper;

import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;

import java.util.Optional;
import java.util.function.BiFunction;

public class IndexableFieldFactories {

    public static final IndexableFieldFactory<String> string = newFactory(IndexableFieldType.STRING, IndexableFieldFactories::stringField);
    public static final IndexableFieldFactory<String> text = newFactory(IndexableFieldType.STRING, IndexableFieldFactories::textField);

    public static final IndexableFieldFactory<Integer> intPoint = newFactory(IndexableFieldType.INTEGER, IntPoint::new);
    public static final IndexableFieldFactory<Long> longPoint = newFactory(IndexableFieldType.LONG, LongPoint::new);
    public static final IndexableFieldFactory<Float> floatPoint = newFactory(IndexableFieldType.FLOAT, FloatPoint::new);
    public static final IndexableFieldFactory<Double> doublePoint = newFactory(IndexableFieldType.DOUBLE, DoublePoint::new);

    public static final IndexableFieldFactory<Range<Integer>> intRange = newFactory(IndexableFieldType.INTEGER, IndexableFieldFactories::intRange);
    public static final IndexableFieldFactory<Range<Long>> longRange = newFactory(IndexableFieldType.LONG, IndexableFieldFactories::longRange);
    public static final IndexableFieldFactory<Range<Float>> floatRange = newFactory(IndexableFieldType.INTEGER, IndexableFieldFactories::floatRange);
    public static final IndexableFieldFactory<Range<Double>> doubleRange = newFactory(IndexableFieldType.INTEGER, IndexableFieldFactories::doubleRange);

    public static final IndexableFieldFactory<String> stringDocValues = newFactory(IndexableFieldType.STRING, IndexableFieldFactories::binaryDocValuesField);
    public static final IndexableFieldFactory<Long> longDocValues = newFactory(IndexableFieldType.STRING, NumericDocValuesField::new);
    public static final IndexableFieldFactory<Float> floatDocValues = newFactory(IndexableFieldType.STRING, FloatDocValuesField::new);
    public static final IndexableFieldFactory<Double> doubleDocValues = newFactory(IndexableFieldType.STRING, DoubleDocValuesField::new);

    public static final IndexableFieldFactory<String> sortedText = newFactory(IndexableFieldType.STRING, IndexableFieldFactories::sortedString);
    public static final IndexableFieldFactory<Integer> sortedInt = newFactory(IndexableFieldType.INTEGER, IndexableFieldFactories::sortedInt);
    public static final IndexableFieldFactory<Long> sortedLong = newFactory(IndexableFieldType.LONG, SortedNumericDocValuesField::new);
    public static final IndexableFieldFactory<Float> sortedFloat = newFactory(IndexableFieldType.FLOAT, IndexableFieldFactories::sortedFloat);
    public static final IndexableFieldFactory<Double> sortedDouble = newFactory(IndexableFieldType.DOUBLE, IndexableFieldFactories::sortedDouble);

    public static <V> IndexableFieldFactory<V> nullable(IndexableFieldFactory<V> factory) {
        return newFactory(factory.getType(), (name, value) -> value == null ? null : factory.create(name, value));
    }

    public static <V> IndexableFieldFactory<Optional<V>> optional(IndexableFieldFactory<V> factory) {
        return newFactory(factory.getType(), (name, value) -> value.map(v -> factory.create(name, v)).orElse(null));
    }

    public static IndexableFieldFactory<? extends Number> point(Class<?> type) {
        return point(IndexableFieldType.fromClass(type));
    }

    public static IndexableFieldFactory<? extends Number> point(IndexableFieldType type) {
        switch (type) {
            case INTEGER:
                return intPoint;
            case LONG:
                return longPoint;
            case FLOAT:
                return floatPoint;
            case DOUBLE:
                return doublePoint;
            default:
                throw new IllegalArgumentException("Unsupported point type " + type);
        }
    }

    public static <V extends Number> IndexableFieldFactory<Range<V>> range(Class<V> type) {
        //noinspection unchecked
        return (IndexableFieldFactory<Range<V>>) range(IndexableFieldType.fromClass(type));
    }

    public static IndexableFieldFactory<? extends Range<?>> range(IndexableFieldType type) {
        switch (type) {
            case INTEGER:
                return intRange;
            case LONG:
                return longRange;
            case FLOAT:
                return floatRange;
            case DOUBLE:
                return doubleRange;
            default:
                throw new IllegalArgumentException("Unsupported range type " + type);
        }
    }

    public static <V> IndexableFieldFactory<V> docValues(Class<V> type) {
        //noinspection unchecked
        return (IndexableFieldFactory<V>) docValues(IndexableFieldType.fromClass(type));
    }

    public static IndexableFieldFactory<?> docValues(IndexableFieldType type) {
        switch (type) {
            case STRING:
                return stringDocValues;
            case INTEGER:
            case LONG:
                return longDocValues;
            case FLOAT:
                return floatDocValues;
            case DOUBLE:
                return doubleDocValues;
            default:
                throw new IllegalArgumentException("Unsupported docValues type " + type);
        }
    }

    public static <V> IndexableFieldFactory<V> sortedField(Class<V> type) {
        //noinspection unchecked
        return (IndexableFieldFactory<V>) sortedField(IndexableFieldType.fromClass(type));
    }

    public static IndexableFieldFactory<?> sortedField(IndexableFieldType type) {
        switch (type) {
            case STRING:
                return sortedText;
            case INTEGER:
            case LONG:
            case FLOAT:
            case DOUBLE:
                return sortedNumber(type);
            default:
                throw new IllegalArgumentException("Unsupported sorted type " + type);
        }
    }

    public static <V extends Number> IndexableFieldFactory<V> sortedNumber(Class<V> type) {
        //noinspection unchecked
        return (IndexableFieldFactory<V>) sortedNumber(IndexableFieldType.fromClass(type));
    }

    public static IndexableFieldFactory<? extends Number> sortedNumber(IndexableFieldType type) {
        switch (type) {
            case INTEGER:
                return sortedInt;
            case LONG:
                return sortedLong;
            case FLOAT:
                return sortedFloat;
            case DOUBLE:
                return sortedDouble;
            default:
                throw new IllegalArgumentException("Unsupported sorted number type " + type);
        }
    }

    private static IndexableField binaryDocValuesField(String name, String value) {
        return new BinaryDocValuesField(name, new BytesRef(value));
    }

    private static IndexableField sortedString(String name, String value) {
        return new SortedDocValuesField(name, new BytesRef(value));
    }

    private static IndexableField stringField(String name, String value) {
        return new StringField(name, value, Field.Store.NO);
    }

    private static IndexableField textField(String name, String value) {
        return new TextField(name, value, Field.Store.NO);
    }

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

    private static IndexableField sortedInt(String name, Integer value) {
        return new SortedNumericDocValuesField(name, value);
    }

    private static IndexableField sortedFloat(String name, Float value) {
        return new SortedNumericDocValuesField(name, NumericUtils.floatToSortableInt(value));
    }

    private static IndexableField sortedDouble(String name, Double value) {
        return new SortedNumericDocValuesField(name, NumericUtils.doubleToSortableLong(value));
    }

    private static <V> IndexableFieldFactory<V> newFactory(IndexableFieldType type, BiFunction<String, V, IndexableField> factory) {
        return new DefaultIndexableFieldFactory<>(type, factory);
    }

    private static class DefaultIndexableFieldFactory<V> implements IndexableFieldFactory<V> {
        private final IndexableFieldType type;
        private final BiFunction<String, V, IndexableField> factory;

        public DefaultIndexableFieldFactory(IndexableFieldType type, BiFunction<String, V, IndexableField> factory) {
            this.type = type;
            this.factory = factory;
        }

        @Override
        public IndexableField create(String name, V value) {
            return factory.apply(name, value);
        }

        @Override
        public IndexableFieldType getType() {
            return type;
        }
    }
}
