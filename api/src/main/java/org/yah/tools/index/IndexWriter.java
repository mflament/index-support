package org.yah.tools.index;

import org.yah.tools.index.query.IndexQuery;

import java.util.Collection;
import java.util.Collections;

public interface IndexWriter<T> extends AutoCloseable {

    default void add(T element) {
        add(Collections.singleton(element));
    }

    void add(Collection<T> elements);

    void update(Collection<T> elements);

    default void update(T element) {
        update(Collections.singleton(element));
    }

    default void delete(String id) {
        delete(Collections.singleton(id));
    }

    void delete(Collection<String> ids);

    void delete(IndexQuery query);

    void clear();

    @Override
    void close();

}
