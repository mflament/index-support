package org.yah.tools.index;

import java.util.function.Function;

public interface Index<T> {

    IndexWriter<T> openWriter();

    IndexReader<T> openReader();

    IndexReader<T> openReader(IndexWriter<T> writer);

    <V> void reindex(Index<V> target, Function<T, V> mapper, ProgressCallback progressCallback);

    interface ProgressCallback {
        ProgressCallback NOOP = new ProgressCallback() {
            @Override
            public void setExpected(long expected) {
                // no op
            }

            @Override
            public void addCompleted() {
                // no op
            }
        };

        void setExpected(long expected);

        void addCompleted();
    }

}
