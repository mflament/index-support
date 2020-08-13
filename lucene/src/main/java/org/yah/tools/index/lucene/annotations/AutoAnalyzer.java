package org.yah.tools.index.lucene.annotations;

import org.apache.lucene.analysis.Analyzer;

public final class AutoAnalyzer extends Analyzer {
    private AutoAnalyzer() {
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        throw new UnsupportedOperationException("createComponents");
    }
}
