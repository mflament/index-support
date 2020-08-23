package org.yah.tools.index.lucene;

import org.apache.lucene.index.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

abstract class LuceneSupportObject<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneSupportObject.class);

    protected final LuceneIndex<T> index;

    protected LuceneSupportObject(LuceneIndex<T> index) {
        this.index = Objects.requireNonNull(index);
    }

    protected final Term idTerm(String id) {
        return new Term(index.documentMapper.getIdField(), id);
    }

    static void closeSafely(Object closeable) {
        if (closeable instanceof AutoCloseable) {
            try {
                ((AutoCloseable) closeable).close();
            } catch (Exception e) {
                LOGGER.warn("Error closing {}", closeable, e);
            }
        }
    }
}
