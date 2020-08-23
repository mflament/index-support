package org.yah.tools.index;

public interface EntityIndex<T> extends Index<T> {

    @Override
    EntityIndexWriter<T> writer();

    @Override
    EntityIndexReader<T> reader();

}
