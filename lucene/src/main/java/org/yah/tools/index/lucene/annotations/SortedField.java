package org.yah.tools.index.lucene.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SortedField {

    /**
     * Alias of name()
     */
    String value() default "";

    /**
     * The indexed field name, default to bean property name
     */
    String name() default "";

}
