package org.yah.tools.index.lucene.annotations;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Index {

    /**
     * name() alias
     */
    String value() default "";

    /**
     * the index name
     */
    String name() default "";

    Class<? extends Analyzer> defaultAnalyzer() default FactoryDefault.class;

    class FactoryDefault extends Analyzer {
        private FactoryDefault() {
        }
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            throw new UnsupportedOperationException();
        }
    }
}
