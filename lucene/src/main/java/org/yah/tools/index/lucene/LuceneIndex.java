package org.yah.tools.index.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.yah.tools.index.*;
import org.yah.tools.index.lucene.mapper.DocumentMapper;
import org.yah.tools.index.lucene.mapper.EntityDocumentMapper;
import org.yah.tools.index.lucene.mapper.WrappedDocumentMapper;
import org.yah.tools.index.lucene.mapper.WrappedEntityDocumentMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class LuceneIndex<T> implements EntityIndex<T>, AutoCloseable {

    protected final Path path;
    protected final Analyzer analyzer;
    protected final EntityDocumentMapper<T> documentMapper;

    protected LuceneIndexReader<T> reader;
    protected LuceneIndexWriter<T> writer;

    public LuceneIndex(Path path, Analyzer analyzer, DocumentMapper<T> documentMapper) {
        this.path = Objects.requireNonNull(path);
        this.analyzer = Objects.requireNonNull(analyzer);
        Objects.requireNonNull(documentMapper, "documentMapper is null");
        if (documentMapper instanceof EntityDocumentMapper) {
            this.documentMapper = new WrappedEntityDocumentMapper<>((EntityDocumentMapper<T>) documentMapper);
        } else {
            this.documentMapper = new WrappedDocumentMapper<>(documentMapper);
        }
    }

    public boolean isEntityIndex() {
        return documentMapper instanceof WrappedEntityDocumentMapper;
    }

    public Path getPath() {
        return path;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public EntityDocumentMapper<T> getDocumentMapper() {
        return documentMapper;
    }

    @Override
    public void close() {
        reader = closeSafely(reader);
        writer = closeSafely(writer);
    }

    @Override
    public synchronized EntityIndexWriter<T> writer() {
        if (writer == null) {
            writer = new LuceneIndexWriter<>(this);
            reader = closeSafely(reader);
        }
        return writer;
    }

    @Override
    public synchronized EntityIndexReader<T> reader() {
        if (reader == null) {
            if (writer != null)
                reader = new LuceneIndexReader<>(writer);
            else
                reader = new LuceneIndexReader<>(this);
        }
        return reader;
    }

    @Override
    public <V> void reindex(Index<V> target,
                            Function<T, V> mapper,
                            ProgressCallback progressCallback) {
        final LuceneIndexReader<T> reader = (LuceneIndexReader<T>) reader();
        final IndexWriter<V> writer = target.writer();
        reader.reindex(writer, mapper, progressCallback);
    }

    public <V> LuceneIndex<V> reindex(Function<T, V> mapper,
                                      Analyzer analyzer,
                                      DocumentMapper<V> documentMapper,
                                      ProgressCallback progressCallback) {
        Path targetPath = Path.of(path.toString() + "_tmp");
        delete(targetPath);
        LuceneIndex<V> target = new LuceneIndex<>(targetPath, analyzer, documentMapper);
        reindex(target, mapper, progressCallback);

        close();
        target.close();
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

    private void moveTo(Path targetPath) {
        list(targetPath, Files::delete);
        list(path, s -> Files.move(s, targetPath.resolve(s.getFileName())));
        delete(path);
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

    private static <T> T closeSafely(T object) {
        if (object instanceof AutoCloseable) {
            try {
                ((AutoCloseable) object).close();
                return null;
            } catch (Exception e) {
                //ignore
            }
        }
        return object;
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
