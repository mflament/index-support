package org.yah.tools.index.lucene.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yah.tools.index.IndexException;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class DefaultDocumentMapper<T> implements EntityDocumentMapper<T> {

    public static <T> Builder<T> builder(Class<T> type) {
        return new Builder<>(type);
    }

    public static final ObjectMapper DEFAULT_OBJECT_MAPPER = createDefaultObjectMapper();

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDocumentMapper.class);
    private static final String JSON_FIELD = "source";

    private final Class<T> type;
    private final ObjectMapper objectMapper;
    private final List<IndexedField<T>> indexedFields;

    private final String idField;
    private final Function<T, String> elementIdProvider;

    public DefaultDocumentMapper(Builder<T> builder) {
        this.type = Objects.requireNonNull(builder.type);
        this.objectMapper = builder.objectMapper == null ? DEFAULT_OBJECT_MAPPER : builder.objectMapper;
        this.indexedFields = List.copyOf(builder.indexedFields);
        this.elementIdProvider = builder.elementIdProvider;
        this.idField = builder.idField;
    }

    @Override
    public Document toDocument(T element) {
        Document document = new Document();
        addJSON(element, document);
        indexedFields.forEach(indexedField -> indexedField.update(element, document));
        return document;
    }

    @Override
    public T toElement(Document document) {
        final String json = document.get(JSON_FIELD);
        if (json == null)
            throw new IndexException("missing json field " + JSON_FIELD);

        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IndexException(e);
        }
    }

    @Override
    public String getIdField() {
        return idField;
    }

    @Override
    public String getElementId(T element) {
        if (elementIdProvider == null)
            throw new UnsupportedOperationException("No elementIdProvider");
        return elementIdProvider.apply(element);
    }

    public boolean isEntityDocumentMapper() {
        return elementIdProvider != null;
    }

    @Override
    public IndexableFieldType getFieldType(String field) {
        return indexedFields.stream()
                .filter(f -> f.getName().equals(field))
                .findFirst()
                .map(IndexedField::getType)
                .orElseThrow(() -> new IllegalArgumentException("field '" + field + "' was not found"));
    }

    Collection<IndexedField<T>> getIndexedFields() {
        return List.copyOf(indexedFields);
    }

    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);

        tryRegisterModules(objectMapper, "com.fasterxml.jackson.module.paramnames.ParameterNamesModule");
        tryRegisterModules(objectMapper, "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule");
        tryRegisterModules(objectMapper, "com.fasterxml.jackson.datatype.jdk8.Jdk8Module");

        return objectMapper;
    }

    private static void tryRegisterModules(ObjectMapper objectMapper, String moduleClassName) {
        final Class<?> moduleClass;
        try {
            moduleClass = Class.forName(moduleClassName);
        } catch (ClassNotFoundException e) {
            // ignore
            return;
        }
        final Module module;
        try {
            module = (Module) moduleClass.getConstructor().newInstance();
        } catch (Exception e) {
            LOGGER.error("Error creating JSON module {}", moduleClassName, e);
            return;
        }
        objectMapper.registerModule(module);
    }

    private void addJSON(T element, Document document) {
        final String json;
        try {
            json = objectMapper.writeValueAsString(element);
        } catch (JsonProcessingException e) {
            throw new IndexException(e);
        }
        document.add(new StoredField(JSON_FIELD, json));
    }

    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public static final class Builder<T> {

        private final Class<T> type;
        private final Collection<IndexedField<T>> indexedFields = new ArrayList<>();
        private ObjectMapper objectMapper;

        private String idField;
        private Function<T, String> elementIdProvider;

        private Builder(Class<T> type) {
            this.type = type;
        }

        public <V> Builder<T> withCollection(String name,
                                             Function<T, Collection<V>> provider,
                                             IndexableFieldFactory<? super V> factory) {
            return withCollection(name, provider, Function.identity(), factory);
        }

        public Class<T> type() {
            return type;
        }

        public <V, F> Builder<T> withCollection(String name,
                                                Function<T, Collection<V>> provider,
                                                Function<V, F> converter,
                                                IndexableFieldFactory<? super F> factory) {
            BiConsumer<T, Document> updater = (e, doc) -> {
                final Collection<V> collection = provider.apply(e);
                if (collection == null)
                    return;
                collection.stream()
                        .map(converter)
                        .map(v -> factory.create(name, v))
                        .filter(Objects::nonNull)
                        .forEach(doc::add);
            };
            indexedFields.add(new IndexedField<>(name, factory.getType(), updater));
            return this;
        }

        public Builder<T> withObjectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public <V> Builder<T> withIndexedField(String name,
                                               Function<T, V> provider,
                                               IndexableFieldFactory<V> factory) {
            indexedFields.add(new IndexedField<>(name, provider, factory));
            return this;
        }

        public <V> Builder<T> withNullableField(String name,
                                                Function<T, V> provider,
                                                IndexableFieldFactory<V> factory) {
            return withIndexedField(name, provider, IndexableFieldFactories.nullable(factory));
        }

        public <V> Builder<T> withOptionalField(String name,
                                                Function<T, Optional<V>> provider,
                                                IndexableFieldFactory<V> factory) {
            return withIndexedField(name, provider, IndexableFieldFactories.optional(factory));
        }

        public <V> Builder<T> withFields(Function<T, V> provider, DefaultDocumentMapper<V> delegate) {
            delegate.indexedFields.stream()
                    .map(f -> delegateField(provider, f))
                    .forEach(indexedFields::add);
            return this;
        }

        public Builder<T> withIdField(String idField, Function<T, String> elementIdProvider) {
            this.idField = Objects.requireNonNull(idField, "idField is null");
            this.elementIdProvider = Objects.requireNonNull(elementIdProvider, "elementIdProvider is null");
            return this;
        }

        public DefaultDocumentMapper<T> build() {
            return new DefaultDocumentMapper<>(this);
        }

        private <V> IndexedField<T> delegateField(Function<T, V> provider, IndexedField<V> delegate) {
            BiConsumer<T, Document> updater = (e, doc) -> {
                final V v = provider.apply(e);
                if (v != null)
                    delegate.update(v, doc);
            };
            return new IndexedField<>(delegate.getName(), delegate.getType(), updater);
        }
    }
}
