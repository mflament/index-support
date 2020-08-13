package org.yah.tools.index;

import org.yah.tools.index.query.IndexCursor;
import org.yah.tools.index.query.IndexQuery;
import org.yah.tools.index.query.IndexQueryBuilder;
import org.yah.tools.index.query.ScoredElement;

import java.util.*;

public interface IndexReader<T> extends AutoCloseable {

    int count();

    IndexQueryBuilder prepareQuery();

    Collection<T> find(Collection<String> ids);

    IndexCursor<T> query(IndexQuery query, int batchSize);

    List<T> list(IndexQuery query);

    Optional<T> findFirst(IndexQuery query);

    default T get(String id) {
        final Collection<T> res = find(Collections.singleton(id));
        if (res.isEmpty())
            throw new NoSuchElementException("Element " + id + " not found");
        return res.iterator().next();
    }

    default Optional<T> find(String id) {
        try {
            return Optional.of(get(id));
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    IndexCursor<ScoredElement<T>> scoredQuery(IndexQuery query, int batchSize);

    List<ScoredElement<T>> scoredList(IndexQuery query);

    @Override
    void close();

}
