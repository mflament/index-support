package org.yah.tools.index;

import java.util.Collection;
import java.util.Collections;

public interface EntityIndexWriter<T> extends IndexWriter<T> {

    void update(Collection<T> elements);

    default void update(T element) {
        update(Collections.singleton(element));
    }

    default void delete(String id) {
        delete(Collections.singleton(id));
    }

    void delete(Collection<String> ids);

}
