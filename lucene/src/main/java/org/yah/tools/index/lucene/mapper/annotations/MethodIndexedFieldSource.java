package org.yah.tools.index.lucene.mapper.annotations;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Function;

class MethodIndexedFieldSource<T,V> implements IndexedFieldSource<T,V> {

    private final Method method;

    public MethodIndexedFieldSource(Method method) {
        this.method = method;
        if (method.getReturnType() == Void.class)
            throw new IllegalArgumentException("Method " + method.getName() + " has not return type");
        method.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Function<T, V> accessor() {
        return o -> {
            try {
                return (V)method.invoke(o);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
    }

    @Override
    public Type type() {
        return method.getGenericReturnType();
    }

    @Override
    public String fieldName() {
        String name = method.getName();
        int prefix = 0;
        if (name.startsWith("get") && name.length() > 3)
            prefix = 3;
        else if (name.startsWith("is") && name.length() > 2)
            prefix = 2;
        if (prefix > 0) {
            name = name.substring(prefix, prefix + 1).toLowerCase() + name.substring(prefix + 1);
        }
        return name;
    }

    @Override
    public String toString() {
        return method.toString();
    }
}
