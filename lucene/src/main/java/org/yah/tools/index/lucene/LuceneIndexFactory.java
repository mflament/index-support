package org.yah.tools.index.lucene;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.yah.tools.index.lucene.annotations.Index;
import org.yah.tools.index.lucene.annotations.IndexedFieldType;
import org.yah.tools.index.lucene.mapper.DefaultDocumentMapper;
import org.yah.tools.index.lucene.mapper.annotations.IndexAnnotationParser;

import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LuceneIndexFactory {

    private static final PropertyNamingStrategy.SnakeCaseStrategy SNAKE_CASE_STRATEGY = new PropertyNamingStrategy.SnakeCaseStrategy();

    private final Path indexesDirectory;
    private final ObjectMapper objectMapper;
    private final ZoneOffset zoneOffset;
    private final Analyzer defaultAnalyzer;
    private final IndexedFieldType defaultStringFieldType;

    public LuceneIndexFactory(Builder builder) {
        indexesDirectory = Objects.requireNonNull(builder.indexesDirectory);
        objectMapper = builder.objectMapper;
        zoneOffset = builder.zoneOffset;
        defaultAnalyzer = builder.defaultAnalyzer == null ? new StandardAnalyzer() : builder.defaultAnalyzer;
        defaultStringFieldType = builder.defaultStringFieldType;
    }

    public <T> LuceneIndex<T> buildIndex(Class<T> entityType) {
        Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
        final IndexAnnotationParser.Builder<T> parserBuilder = IndexAnnotationParser.builder(entityType)
                .withAnalyzers(fieldAnalyzers);
        if (objectMapper != null)
            parserBuilder.withObjectMapper(objectMapper);
        if (defaultStringFieldType != null)
            parserBuilder.withDefaultStringFieldType(defaultStringFieldType);
        if (zoneOffset != null)
            parserBuilder.withZoneOffset(zoneOffset);
        final Index annotation = entityType.getAnnotation(Index.class);
        Analyzer defaultAnalyzer = this.defaultAnalyzer;
        String name = null;
        if (annotation != null) {
            if (annotation.defaultAnalyzer() != Index.FactoryDefault.class) {
                defaultAnalyzer = IndexAnnotationParser.createAnalyzer(annotation.defaultAnalyzer());
            }
            name = StringUtils.trimToNull(annotation.name());
            if (name == null)
                name = StringUtils.trimToNull(annotation.value());
        }
        if (name == null)
            name = SNAKE_CASE_STRATEGY.translate(entityType.getSimpleName());

        final DefaultDocumentMapper<T> documentMapper = parserBuilder.build().parse();
        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, fieldAnalyzers);
        Path indexPath = indexesDirectory.resolve(name);
        return new LuceneIndex<>(indexPath, analyzer, documentMapper);
    }

    public static Builder builder(Path indexPath) {
        return new Builder(indexPath);
    }

    public static final class Builder {
        private final Path indexesDirectory;
        private ObjectMapper objectMapper;
        private ZoneOffset zoneOffset;
        private Analyzer defaultAnalyzer;
        private IndexedFieldType defaultStringFieldType;

        private Builder(Path indexPath) {
            this.indexesDirectory = indexPath;
        }

        public Builder withObjectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder withZoneOffset(ZoneOffset zoneOffset) {
            this.zoneOffset = zoneOffset;
            return this;
        }

        public Builder withDefaultAnalyzer(Analyzer defaultAnalyzer) {
            this.defaultAnalyzer = defaultAnalyzer;
            return this;
        }

        public Builder withDefaultStringFieldType(IndexedFieldType defaultFieldType) {
            this.defaultStringFieldType = defaultFieldType;
            return this;
        }

        public LuceneIndexFactory build() {
            return new LuceneIndexFactory(this);
        }
    }

}
