package org.yah.tools.index;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Objects;

public class IndexHolder<T> implements AutoCloseable {
    private final Index<T> index;
    private IndexReader<T> reader;
    private IndexWriter<T> writer;

    public IndexHolder(Index<T> index) {
        this.index = Objects.requireNonNull(index);
    }

    public Index<T> getIndex() {
        return index;
    }

    @Override
    public synchronized void close() {
        closeWriter();
        closeReader();
    }

    public synchronized void closeWriter() {
        if (writer != null) {
            closeSafely(writer);
            writer = null;
            closeReader();
        }
    }

    public synchronized void closeReader() {
        if (reader != null) {
            closeSafely(reader);
            reader = null;
        }
    }

    public synchronized IndexReader<T> reader() {
        if (reader == null) {
            reader = index.openReader(writer);
        }
        return preventCloseProxy(reader, IndexReader.class);
    }

    public synchronized IndexWriter<T> writer() {
        if (writer == null) {
            writer = index.openWriter();
            closeReader();
        }
        return preventCloseProxy(writer, IndexWriter.class);
    }

    private static void closeSafely(AutoCloseable object) {
        try {
            object.close();
        } catch (Exception e) {
            //ignore
        }
    }

    private static <T extends AutoCloseable> T preventCloseProxy(T target, Class<?> type) {
        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if (args == null && method.getName().equals("close")) {
                // ignore;
                return null;
            } else {
                return method.invoke(target, args);
            }
        };
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(target.getClass().getClassLoader(), new Class[]{type}, invocationHandler);
    }

}
