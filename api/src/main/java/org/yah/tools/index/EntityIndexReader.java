package org.yah.tools.index;

import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Optional;

public interface EntityIndexReader<T> extends IndexReader<T> {

    Collection<T> find(Collection<String> ids);

    default Optional<T> find(String id) {
        try {
            return Optional.of(get(id));
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    default T get(String id) {
        final Collection<T> res = find(Collections.singleton(id));
        if (res.isEmpty())
            throw new NoSuchElementException("Element " + id + " not found");
        return res.iterator().next();
    }

}
