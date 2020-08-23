package org.yah.tools.index.lucene.mapper;

import org.yah.tools.index.IndexWriter;
import org.yah.tools.index.lucene.LuceneIndex;
import org.yah.tools.index.lucene.LuceneIndexFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class Sandbox implements AutoCloseable {

    public static void main(String[] args) {
        final LuceneIndexFactory factory = LuceneIndexFactory
                .builder(Path.of("target/test/index"))
                .build();
        try (Sandbox sandbox = new Sandbox(factory.buildIndex(TestEntity.class))) {
            sandbox.createEntities(1000);
            sandbox.readEntities();
        }
    }

    private void readEntities() {
        System.out.println(index.reader().count() + " entities found");
    }

    private final LuceneIndex<TestEntity> index;

    public Sandbox(LuceneIndex<TestEntity> index) {
        this.index = index;
    }

    @Override
    public void close() {
        index.close();
    }

    private void createEntities(int count) {
        final Supplier<TestEntity> supplier = TestEntity.randomEntities(new Random());
        List<TestEntity> entities =new ArrayList<>(count);
        while (entities.size() < count) entities.add(supplier.get());
        final IndexWriter<TestEntity> writer = index.writer();
        writer.add(entities);
    }

}
