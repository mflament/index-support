package org.yah.tools.index.lucene.mapper;

import com.github.javafaker.Faker;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.yah.tools.index.lucene.annotations.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Supplier;

@Index(name = "test", defaultAnalyzer = EnglishAnalyzer.class)
public class TestEntity {

    public static Supplier<TestEntity> randomEntities(Random random) {
        Faker faker = Faker.instance(random);
        return () -> randomEntity(faker);
    }

    public static TestEntity randomEntity(Random random) {
        return randomEntity(Faker.instance(random));
    }

    public static TestEntity randomEntity(Faker faker) {
        TestEntity te = new TestEntity();
        te.id = UUID.randomUUID().toString();
        te.firstName = faker.name().firstName();
        te.lastName = faker.name().lastName();

        te.colors = new String[faker.random().nextInt(6)];
        for (int i = 0; i < te.colors.length; i++) {
            te.colors[i] = faker.color().name();
        }

        int size = faker.random().nextInt(6);
        te.animals = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            te.animals.add(faker.animal().name());
        }

        te.size = faker.random().nextInt(10, 100);

        final Date birthday = faker.date().birthday(18, 115);
        te.birthDate = LocalDate.from(LocalDate.ofInstant(birthday.toInstant(), ZoneId.systemDefault()));
        return te;
    }

    @Id
    private String id;

    @IndexField(type = IndexedFieldType.KEYWORD)
    private String firstName;

    @IndexFields({
            @IndexField(type = IndexedFieldType.KEYWORD),
            @IndexField(type = IndexedFieldType.DOC_VALUES)
    })
    private String lastName;

    @IndexField(type = IndexedFieldType.KEYWORD, analyzer = StandardAnalyzer.class)
    private String[] colors;

    @IndexField(analyzer = KeywordAnalyzer.class)
    private List<String> animals;

    @IndexField(name = "theSize")
    @SortedField
    private int size;

    @IndexField
    private LocalDate birthDate;

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String[] getColors() {
        return colors;
    }

    public List<String> getAnimals() {
        return animals;
    }

    public int getSize() {
        return size;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    @IndexField(type = IndexedFieldType.TEXT, analyzer = FrenchAnalyzer.class)
    @SortedField("sortedName")
    public String getFullName() {
        return firstName + " " + lastName;
    }

}
