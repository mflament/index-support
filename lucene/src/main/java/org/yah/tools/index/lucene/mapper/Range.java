package org.yah.tools.index.lucene.mapper;

public interface Range<T extends Number> {
    T min();
    T max();
}
