package org.yah.tools.index.lucene.mapper;

public enum IndexableFieldType {
    STRING,
    INTEGER,
    LONG,
    FLOAT,
    DOUBLE;

    static IndexableFieldType fromClass(Class<?> type) {
        if (type == String.class) return STRING;
        if (type == Integer.class) return INTEGER;
        if (type == Long.class) return LONG;
        if (type == Float.class) return FLOAT;
        if (type == Double.class) return DOUBLE;
        throw new IllegalArgumentException("No type from " + type.getName());
    }
}
