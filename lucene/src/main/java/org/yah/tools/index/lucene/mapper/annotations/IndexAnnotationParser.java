package org.yah.tools.index.lucene.mapper.annotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yah.tools.index.lucene.annotations.*;
import org.yah.tools.index.lucene.mapper.DefaultDocumentMapper;
import org.yah.tools.index.lucene.mapper.IndexableFieldFactories;
import org.yah.tools.index.lucene.mapper.IndexableFieldFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.time.*;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.apache.commons.lang3.reflect.TypeUtils.isAssignable;

public final class IndexAnnotationParser<T> {

    public static <T> Builder<T> builder(DefaultDocumentMapper.Builder<T> builder) {
        return new Builder<>(builder);
    }

    public static <T> Builder<T> builder(Class<T> entityClass) {
        return new Builder<>(DefaultDocumentMapper.builder(entityClass));
    }

    public static Analyzer createAnalyzer(Class<? extends Analyzer> analyzerClass) {
        try {
            return analyzerClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error creating analyzer " + analyzerClass.getName(), e);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexAnnotationParser.class);
    private final DefaultDocumentMapper.Builder<T> builder;
    private final Map<String, Analyzer> analyzers;
    private final IndexedFieldType defaultStringFieldType;
    private final ZoneOffset zoneOffset;
    private final Map<String, Class<? extends Analyzer>> analyzersClasses = new HashMap<>();

    private final LinkedList<NestedBeanSource> beanSources = new LinkedList<>();
    private AnnotatedIdSource idSource;

    public IndexAnnotationParser(Builder<T> builder) {
        this.builder = Objects.requireNonNull(builder.builder);
        this.analyzers = Objects.requireNonNull(builder.analyzers);
        this.defaultStringFieldType = Objects.requireNonNull(builder.defaultStringFieldType);
        this.zoneOffset = Objects.requireNonNull(builder.zoneOffset);
    }

    public DefaultDocumentMapper<T> parse() {
        parse(builder.type());
        createAnalyzers();
        return builder.build();
    }

    private void parse(Type type) {
        Type currentType = type;
        while (currentType != Object.class) {
            Class<?> typeClass = TypeUtils.getRawType(currentType, currentType);
            if (typeClass == null)
                throw new IllegalArgumentException("Coundl not get class from " + currentType);
            final Stream<? extends AccessibleObject> elements = Stream.concat(
                    Arrays.stream(typeClass.getDeclaredFields()),
                    Arrays.stream(typeClass.getDeclaredMethods())
            );
            elements.flatMap(this::createSources)
                    .forEach(AnnotatedSource::createFields);
            currentType = typeClass.getGenericSuperclass();
        }
    }

    private Stream<AnnotatedSource> createSources(AnnotatedElement element) {
        final Id id = element.getAnnotation(Id.class);
        final Collection<IndexedField> fields = annotationsList(element,
                IndexedFields.class,
                IndexedField.class,
                IndexedFields::value);
        final Collection<SortedField> sortedFields = annotationsList(element,
                SortedFields.class,
                SortedField.class,
                SortedFields::value);

        List<AnnotatedSource> sources = new ArrayList<>();
        if (id != null) {
            if (idSource != null) {
                if (idSource.isSameDeclaringClass(element))
                    LOGGER.warn("Found multiple id source fields: {}", List.of(element, idSource));
            } else {
                idSource = new AnnotatedIdSource(element, id);
                sources.add(idSource);
            }
        }

        fields.stream()
                .map(a -> new IndexFieldSource(element, a))
                .forEach(sources::add);
        sortedFields.stream()
                .map(a -> new SortedFieldSource(element, a))
                .forEach(sources::add);

        if (sources.isEmpty()) {
            final Indexed indexed = element.getAnnotation(Indexed.class);
            if (indexed != null) {
                sources.add(new NestedBeanSource(element, indexed));
            }
        }
        return sources.stream();
    }

    private static <A extends Annotation, B extends Annotation> Collection<B> annotationsList(AnnotatedElement element,
                                                                                              Class<A> listAnnotationClass,
                                                                                              Class<B> singleAnnotationClass,
                                                                                              Function<A, B[]> listAccessor) {
        final A listAnnotation = element.getAnnotation(listAnnotationClass);
        final B singleAnnotation = element.getAnnotation(singleAnnotationClass);
        if (listAnnotation != null && singleAnnotation != null) {
            LOGGER.warn("Found both {} & {} annotation on {}, {} will be ignored",
                    listAnnotationClass.getName(),
                    singleAnnotationClass.getName(),
                    element,
                    listAnnotationClass.getName());
            return List.of(singleAnnotation);
        }
        if (listAnnotation != null)
            return Arrays.asList(listAccessor.apply(listAnnotation));
        if (singleAnnotation != null)
            return List.of(singleAnnotation);
        return Collections.emptyList();
    }

    private void createAnalyzers() {
        Map<Class<? extends Analyzer>, Analyzer> analyzersByType = new HashMap<>();
        analyzersClasses.values().forEach(c -> analyzersByType.put(c, createAnalyzer(c)));
        analyzersClasses.forEach((field, analyzerClass) ->
                analyzers.put(field, analyzersByType.get(analyzerClass))
        );
    }

    private void putAnalyzer(String fieldName, Class<? extends Analyzer> analyzer) {
        if (analyzers.containsKey(fieldName)) {
            // field analyzer already configured, ignore annotation
            return;
        }

        final Class<? extends Analyzer> previous = analyzersClasses.put(fieldName, analyzer);
        if (previous != null && previous != analyzer) {
            LOGGER.warn("Conflicting analyzers '{}' and '{}' for field {}", previous.getName(),
                    analyzer.getName(), fieldName);
        }
    }

    private static String trimToNull(String s) {
        if (s == null)
            return null;
        s = s.trim();
        if (s.length() == 0)
            return null;
        return s;
    }

    private static Class<?> box(Class<?> other) {
        if (other == Byte.TYPE) return Byte.class;
        if (other == Short.TYPE) return Short.class;
        if (other == Integer.TYPE) return Integer.class;
        if (other == Long.TYPE) return Long.class;
        if (other == Float.TYPE) return Float.class;
        if (other == Double.TYPE) return Double.class;
        if (other == Boolean.TYPE) return Boolean.class;
        if (other == Character.TYPE) return Character.class;
        return other;
    }

    @SuppressWarnings("unchecked")
    private static <T, V> Function<T, V> cast(Function<T, ?> accessor) {
        return (Function<T, V>) accessor;
    }

    @SuppressWarnings("unchecked")
    private static <T, V> IndexedFieldSource<T, V> cast(IndexedFieldSource<T, ?> source) {
        return (IndexedFieldSource<T, V>) source;
    }

    @SuppressWarnings("unchecked")
    private static <V> Function<Object, Object> mapper(Function<V, ?> mapper) {
        return o -> mapper.apply((V) o);
    }

    private static Type getFirstArgument(Type type, Class<?> toClass) {
        final Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(type, toClass);
        final TypeVariable<? extends Class<?>> typeParameter = toClass.getTypeParameters()[0];
        final Type argumentType = typeArguments.get(typeParameter);
        if (argumentType != null) return argumentType;
        throw new IllegalArgumentException(typeParameter + " not found in " + type);
    }

    private <V> Function<T, V> unwrapOptional(Function<T, Optional<V>> accessor) {
        //noinspection OptionalAssignedToNull
        return accessor.andThen(opt -> opt == null ? null : opt.orElse(null));
    }

    private IndexedFieldSource<T, ?> createFieldSource(AnnotatedElement element) {
        IndexedFieldSource<?, ?> res;
        if (element instanceof Method)
            res = new MethodIndexedFieldSource<>((Method) element);
        else
            res = new FieldIndexedFieldSource<>((Field) element);

        NestedBeanSource parent = beanSources.peekLast();
        if (parent != null) {
            return parent.createNestedFieldSource(res);
        }

        //noinspection unchecked
        return (IndexedFieldSource<T, ?>) res;
    }

    private static Class<?> getDeclaringClass(AnnotatedElement element) {
        return ((Member) element).getDeclaringClass();
    }

    private abstract class AnnotatedSource {

        protected final AnnotatedElement element;

        protected final String fieldName;

        protected final IndexedFieldSource<T, ?> fieldSource;

        private AnnotatedSource(AnnotatedElement element, String... names) {
            this.element = Objects.requireNonNull(element, "element is null");
            this.fieldSource = createFieldSource(element);
            final NestedBeanSource parent = beanSources.peekLast();
            List<String> parts = new ArrayList<>();
            if (parent != null && parent.fieldName != null)
                parts.add(parent.fieldName);
            parts.add(fieldName(names));
            this.fieldName = String.join(".", parts);
        }

        public abstract void createFields();

        @Override
        public String toString() {
            return fieldName + ": " + element.toString();
        }

        protected final String fieldName(String... names) {
            return Arrays.stream(names)
                    .map(IndexAnnotationParser::trimToNull)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseGet(fieldSource::fieldName);
        }
    }

    private class AnnotatedIdSource extends AnnotatedSource {
        public AnnotatedIdSource(AnnotatedElement element, Id annotation) {
            super(element, annotation.name(), annotation.value());
        }

        @Override
        public void createFields() {
            // id can not be null and will always be transformed to string
            Function<T, String> accessor;
            if (fieldSource.type() == String.class)
                accessor = cast(fieldSource.accessor());
            else
                accessor = fieldSource.accessor().andThen(Object::toString);
            builder.withIdField(fieldName, accessor);
        }

        boolean isSameDeclaringClass(AnnotatedElement other) {
            return getDeclaringClass(this.element) == getDeclaringClass(other);
        }

    }

    private abstract class AnnotatedFieldSource extends AnnotatedSource {

        public AnnotatedFieldSource(AnnotatedElement element, String... names) {
            super(element, names);
        }

        @Override
        public void createFields() {
            // indexed field
            Function<T, ?> accessor = fieldSource.accessor();
            Type type = fieldSource.type();
            if (isAssignable(type, Optional.class)) {
                type = getFirstArgument(type, Optional.class);
                accessor = unwrapOptional(cast(accessor));
            }

            final Type componentType = TypeUtils.getArrayComponentType(type);
            boolean collection = false;
            if (componentType != null || isAssignable(type, Collection.class)) {
                if (componentType != null) {
                    type = componentType;
                    accessor = arrayToCollection(cast(accessor));
                } else {
                    type = getFirstArgument(type, Collection.class);
                }
                collection = true;
            }

            ResolvedFieldFactory<?> fieldFactory = createFieldFactory(type, collection);
            if (fieldFactory != null) {
                Function<Object, Object> mapper;
                if (!isAssignable(type, fieldFactory.inputType)) {
                    mapper = createMapper(type, fieldFactory.inputType);
                } else {
                    mapper = Function.identity();
                }

                if (collection) {
                    builder.withCollection(fieldName, cast(accessor), cast(mapper), fieldFactory.factory);
                } else {
                    builder.withNullableField(fieldName, cast(accessor.andThen(mapper)), fieldFactory.factory);
                }
            }
        }

        protected abstract ResolvedFieldFactory<?> createFieldFactory(Type forType, boolean collection);

        protected final boolean isPointType(Type type) {
            return numberType(type) != null;
        }

        protected final Class<? extends Number> numberType(Type forType) {
            if (isAssignable(forType, Temporal.class) || isAssignable(forType, Date.class))
                return Long.class;

            if (forType instanceof Class) {
                final Class<?> boxedType = box((Class<?>) forType);
                if (TypeUtils.isAssignable(boxedType, Number.class)) {
                    //noinspection unchecked
                    return (Class<? extends Number>) boxedType;
                }
            }
            return null;
        }

        private <V> Function<T, Collection<V>> arrayToCollection(Function<T, V[]> accessor) {
            return accessor.andThen(a -> a == null ? null : Arrays.asList(a));
        }

        private Function<Object, Object> createMapper(Type fomType, Class<?> toType) {
            Function<Object, Object> mapper = null;
            if (toType == String.class) {
                mapper = Object::toString;
            } else if (Number.class.isAssignableFrom(toType)) {
                if (isAssignable(fomType, Temporal.class))
                    mapper = temporalMapper(fomType);
                else if (isAssignable(fomType, Date.class))
                    mapper = dateMapper();
                else if (isAssignable(fomType, Number.class))
                    mapper = Function.identity();
            }
            if (mapper == null)
                throw new IllegalArgumentException(fomType + " can not be converted to " + toType);
            final Function<Object, Object> finalMapper = mapper;
            return o -> o == null ? null : finalMapper.apply(o);
        }

        private Function<Object, Object> dateMapper() {
            return o -> ((Date) o).getTime();
        }

        private Function<Object, Object> temporalMapper(Type valueType) {
            if (isAssignable(valueType, Instant.class))
                return mapper(Instant::toEpochMilli);
            if (isAssignable(valueType, LocalDate.class))
                return mapper(LocalDate::toEpochDay);
            if (isAssignable(valueType, LocalDateTime.class))
                return IndexAnnotationParser.<LocalDateTime>mapper(ldt -> ldt.toEpochSecond(zoneOffset));
            if (isAssignable(valueType, OffsetDateTime.class))
                return mapper(OffsetDateTime::toEpochSecond);
            throw new IllegalArgumentException("Unsupported temporal type " + valueType);
        }

    }

    private class NestedBeanSource extends AnnotatedSource {

        private final String fieldName;

        public NestedBeanSource(AnnotatedElement element, Indexed indexed) {
            super(element);
            final NestedBeanSource parent = beanSources.peekLast();
            List<String> names = new ArrayList<>();
            if (parent != null && parent.fieldName != null)
                names.add(parent.fieldName);
            if (!indexed.flatten())
                names.add(fieldName(indexed.name(), indexed.value()));
            if (names.isEmpty())
                this.fieldName = null;
            else
                this.fieldName = String.join(".", names);
        }

        @Override
        public void createFields() {
            beanSources.addLast(this);
            fieldSource.type();
            parse(fieldSource.type());
            beanSources.removeLast();
        }

        public IndexedFieldSource<T, ?> createNestedFieldSource(IndexedFieldSource<?, ?> fieldSource) {
            return new NestedFieldSource<>(cast(this.fieldSource), cast(fieldSource));
        }
    }

    private class IndexFieldSource extends AnnotatedFieldSource {
        private final IndexedField indexField;

        public IndexFieldSource(AnnotatedElement element, IndexedField indexField) {
            super(element, indexField.name(), indexField.value());
            this.indexField = indexField;
        }

        @Override
        protected ResolvedFieldFactory<?> createFieldFactory(Type forType, boolean collection) {
            IndexedFieldType fieldType;
            if (indexField.type() == IndexedFieldType.AUTO) {
                fieldType = isPointType(forType) ? IndexedFieldType.POINT : defaultStringFieldType;
            } else {
                fieldType = indexField.type();
            }

            if (indexField.analyzer() != AutoAnalyzer.class) {
                putAnalyzer(fieldName, indexField.analyzer());
            } else if (fieldType == IndexedFieldType.KEYWORD) {
                putAnalyzer(fieldName, KeywordAnalyzer.class);
            }

            switch (fieldType) {
                case KEYWORD:
                case STRING:
                    return ResolvedFieldFactory.fromString(IndexableFieldFactories.string);
                case TEXT:
                    return ResolvedFieldFactory.fromString(IndexableFieldFactories.text);
                case POINT:
                    Class<? extends Number> numberType = numberType(forType);
                    if (numberType == null)
                        throw new IllegalArgumentException("type " + forType + " can not be converted to number");
                    return ResolvedFieldFactory.fromNumber(IndexableFieldFactories.point(numberType));
                case DOC_VALUES:
                    if (isAssignable(forType, String.class))
                        return ResolvedFieldFactory.fromString(IndexableFieldFactories.stringDocValues);

                    numberType = numberType(forType);
                    if (numberType != null) {
                        if (Long.class.isAssignableFrom(numberType) || Integer.class.isAssignableFrom(numberType))
                            return ResolvedFieldFactory.fromNumber(IndexableFieldFactories.longDocValues);
                        if (Float.class.isAssignableFrom(numberType))
                            return ResolvedFieldFactory.fromNumber(IndexableFieldFactories.floatDocValues);
                        if (Double.class.isAssignableFrom(numberType))
                            return ResolvedFieldFactory.fromNumber(IndexableFieldFactories.doubleDocValues);
                    }
                    throw new IllegalArgumentException("type " + forType + " can not be converted to number");
                default:
                    throw new IllegalArgumentException(fieldType.toString());
            }
        }
    }

    private class SortedFieldSource extends AnnotatedFieldSource {

        public SortedFieldSource(AnnotatedElement element, SortedField sortedField) {
            super(element, sortedField.name(), sortedField.value());
        }

        @Override
        protected ResolvedFieldFactory<?> createFieldFactory(Type forType, boolean collection) {
            if (collection) {
                LOGGER.warn("Collection field  " + fieldName + " can not be sorted");
                return null;
            }

            if (isAssignable(forType, String.class)) {
                return ResolvedFieldFactory.fromString(IndexableFieldFactories.sortedText);
            }

            final Class<? extends Number> numberType = numberType(forType);
            if (numberType != null) {
                return ResolvedFieldFactory.fromNumber(IndexableFieldFactories.sortedNumber(numberType));
            }

            throw new IllegalArgumentException("Unable to create SortableFieldFactory for type " + forType);
        }

    }

    private static class ResolvedFieldFactory<V> {
        private final Class<? super V> inputType;
        private final IndexableFieldFactory<V> factory;

        private ResolvedFieldFactory(Class<? super V> inputType, IndexableFieldFactory<V> factory) {
            this.inputType = inputType;
            this.factory = factory;
        }

        static ResolvedFieldFactory<String> fromString(IndexableFieldFactory<String> factory) {
            return new ResolvedFieldFactory<>(String.class, factory);
        }

        static <V extends Number> ResolvedFieldFactory<V> fromNumber(IndexableFieldFactory<V> factory) {
            return new ResolvedFieldFactory<>(Number.class, factory);
        }

    }

    public static final class Builder<T> {
        private final DefaultDocumentMapper.Builder<T> builder;
        public Map<String, Analyzer> analyzers = new HashMap<>();
        private IndexedFieldType defaultStringFieldType = IndexedFieldType.TEXT;
        private ZoneOffset zoneOffset = ZoneOffset.UTC;

        public Builder(DefaultDocumentMapper.Builder<T> builder) {
            this.builder = builder;
        }

        public Builder<T> withDefaultStringFieldType(IndexedFieldType defaultStringFieldType) {
            this.defaultStringFieldType = defaultStringFieldType;
            return this;
        }

        public Builder<T> withZoneOffset(ZoneOffset zoneOffset) {
            this.zoneOffset = zoneOffset;
            return this;
        }

        public Builder<T> withAnalyzers(Map<String, Analyzer> analyzers) {
            this.analyzers = analyzers;
            return this;
        }

        public Builder<T> withObjectMapper(ObjectMapper objectMapper) {
            this.builder.withObjectMapper(objectMapper);
            return this;
        }

        public IndexAnnotationParser<T> build() {
            return new IndexAnnotationParser<>(this);
        }

    }
}

