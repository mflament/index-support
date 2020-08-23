package org.yah.tools.index.lucene.mapper;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.SortField;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class IndexedField<T> {

    public static <T, V> BiConsumer<T, Document> documentUpdater(String name,
                                                                 Function<T, ? extends V> provider,
                                                                 IndexableFieldFactory<V> factory) {
        return (value, doc) -> {
            final IndexableField field;
            try {
                field = factory.create(name, provider.apply(value));
            } catch (RuntimeException e) {
                throw new IllegalStateException("Error creating field " + name, e);
            }
            if (field != null)
                doc.add(field);
        };
    }

    private final String name;
    private final IndexableFieldType fieldType;
    private final BiConsumer<T, Document> documentUpdater;

    public <V> IndexedField(String name,
                            Function<T, ? extends V> provider,
                            IndexableFieldFactory<V> factory) {
        this(name, factory.getType(), documentUpdater(name, provider, factory));
    }

    public IndexedField(String name,
                        IndexableFieldType fieldType,
                        BiConsumer<T, Document> documentUpdater) {
        this.name = Objects.requireNonNull(name);
        this.fieldType = Objects.requireNonNull(fieldType);
        this.documentUpdater = Objects.requireNonNull(documentUpdater);
    }

    public String getName() {
        return name;
    }

    public IndexableFieldType getType() {
        return fieldType;
    }

    public void update(T element, Document document) {
        documentUpdater.accept(element, document);
    }

}
