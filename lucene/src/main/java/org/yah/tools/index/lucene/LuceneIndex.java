package org.yah.tools.index.lucene;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.yah.tools.index.Index;
import org.yah.tools.index.IndexException;
import org.yah.tools.index.IndexWriter;
import org.yah.tools.index.lucene.mapper.DefaultDocumentMapper;
import org.yah.tools.index.lucene.mapper.DocumentMapper;
import org.yah.tools.index.lucene.mapper.annotations.IndexAnnotationParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class LuceneIndex<T> implements Index<T> {
    public static final String ID_FIELD = "_id";

    final Path path;
    final Analyzer analyzer;
    final DocumentMapper<T> documentMapper;

    public LuceneIndex(Path path, Analyzer analyzer, DocumentMapper<T> documentMapper) {
        this.path = Objects.requireNonNull(path);
        this.analyzer = Objects.requireNonNull(analyzer);
        this.documentMapper = Objects.requireNonNull(documentMapper);
    }

    public Path getPath() {
        return path;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public DocumentMapper<T> getDocumentMapper() {
        return documentMapper;
    }

    @Override
    public LuceneIndexWriter<T> openWriter() {
        return new LuceneIndexWriter<>(this);
    }

    @Override
    public LuceneIndexReader<T> openReader() {
        return new LuceneIndexReader<>(this);
    }

    @Override
    public LuceneIndexReader<T> openReader(IndexWriter<T> writer) {
        if (writer == null)
            return openReader();
        return new LuceneIndexReader<>((LuceneIndexWriter<T>) writer);
    }

    @Override
    public <V> void reindex(Index<V> target,
                            Function<T, V> mapper,
                            ProgressCallback progressCallback) {
        try (final LuceneIndexReader<T> reader = openReader();
             final IndexWriter<V> writer = target.openWriter()) {
            reader.reindex(writer, mapper, progressCallback);
        }
    }

    public <V> LuceneIndex<V> reindex(Function<T, V> mapper,
                                      Analyzer analyzer,
                                      DocumentMapper<V> documentMapper,
                                      ProgressCallback progressCallback) {
        Path targetPath = Path.of(path.toString() + "_tmp");
        delete(targetPath);
        LuceneIndex<V> target = new LuceneIndex<>(targetPath, analyzer, documentMapper);
        reindex(target, mapper, progressCallback);
        target.moveTo(this.path);
        return new LuceneIndex<>(path, analyzer, documentMapper);
    }

    Directory openDirectory() {
        Objects.requireNonNull(path);
        Directory directory;
        try {
            Files.createDirectories(path);
            directory = FSDirectory.open(path);
            tryCreate(directory);
        } catch (IOException e) {
            throw new IndexException(e);
        }
        return directory;
    }

    private void moveTo(Path path) {
        list(path, Files::delete);
        list(this.path, s -> Files.move(s, path.resolve(s.getFileName())));
        delete(this.path);
    }

    private static void tryCreate(Directory directory) throws IOException {
        try (org.apache.lucene.index.IndexWriter writer = new org.apache.lucene.index.IndexWriter(directory, new IndexWriterConfig())) {
            // simply create index
            writer.commit();
        } catch (LockObtainFailedException e) {
            // ignore
        }
    }

    private static void delete(Path path) {
        if (!Files.exists(path))
            return;

        if (Files.isDirectory(path)) {
            list(path, LuceneIndex::delete);
        }

        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new IndexException(e);
        }
    }

    private static void list(Path directory, PathConsumer consumer) {
        if (!Files.isDirectory(directory))
            throw new IllegalArgumentException(directory + " is not a directory");

        try (Stream<Path> stream = Files.list(directory)) {
            stream.forEach(consumer);
        } catch (IOException e) {
            throw new IndexException(e);
        }
    }


    private interface PathConsumer extends Consumer<Path> {
        default void accept(Path p) {
            try {
                acceptPath(p);
            } catch (IOException e) {
                throw new IndexException(e);
            }
        }

        void acceptPath(Path path) throws IOException;
    }



}
