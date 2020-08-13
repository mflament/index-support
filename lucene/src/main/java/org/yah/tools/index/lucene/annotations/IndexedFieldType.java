package org.yah.tools.index.lucene.annotations;

public enum IndexedFieldType {
    /**
     * single token without any transformation
     */
    AUTO,
    /**
     * single token without any transformation
     */
    KEYWORD,
    /**
     * transformed single token
     */
    STRING,
    /**
     * analyzed phrase
     */
    TEXT,
    /**
     * numeric value
     */
    POINT,
    /**
     * doc values
     */
    DOC_VALUES
}
