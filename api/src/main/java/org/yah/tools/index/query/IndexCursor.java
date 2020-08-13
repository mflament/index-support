package org.yah.tools.index.query;

import java.util.Iterator;
import java.util.stream.Stream;

public interface IndexCursor<T> extends Iterator<T>, AutoCloseable {

    Stream<T> stream();

    long getTotalHits();

    long getMinTotalHits();

    @Override
    void close();

}
