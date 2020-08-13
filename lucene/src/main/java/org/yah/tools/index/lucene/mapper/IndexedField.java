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
        return (e, doc) -> {
            final IndexableField field = factory.create(name, provider.apply(e));
            if (field != null)
                doc.add(field);
        };
    }

    private final String name;
    private final BiConsumer<T, Document> documentUpdater;
    private final SortField.Type sortType;

    public <V> IndexedField(String name, Function<T, ? extends V> provider, IndexableFieldFactory<V> factory) {
        this(name, documentUpdater(name, provider, factory), getSortType(factory));
    }

    public IndexedField(String name, BiConsumer<T, Document> documentUpdater, SortField.Type sortType) {
        this.name = Objects.requireNonNull(name);
        this.documentUpdater = Objects.requireNonNull(documentUpdater);
        this.sortType = sortType;
    }

    public String getName() {
        return name;
    }

    public Optional<SortField.Type> getSortType() {
        return Optional.ofNullable(sortType);
    }

    public void update(T element, Document document) {
        documentUpdater.accept(element, document);
    }

    static <V> SortField.Type getSortType(IndexableFieldFactory<V> factory) {
        return factory instanceof SortableFieldFactory ? ((SortableFieldFactory<V>) factory)
                .getSortType() : null;
    }

}
