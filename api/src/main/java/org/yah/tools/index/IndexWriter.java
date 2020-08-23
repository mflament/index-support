package org.yah.tools.index;

import org.yah.tools.index.query.IndexQuery;

import java.util.Collection;
import java.util.Collections;

public interface IndexWriter<T> {

    default void add(T element) {
        add(Collections.singleton(element));
    }

    void add(Collection<T> elements);

    void delete(IndexQuery query);

    void clear();

}
