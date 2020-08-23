package org.yah.tools.index.lucene.mapper;

import com.github.javafaker.Faker;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.yah.tools.index.lucene.annotations.*;
import org.yah.tools.index.lucene.annotations.IndexedField;

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

        te.nestedBean1 = NestedBean.randomBean(faker);
        if (faker.random().nextDouble() > 0.3) {
            te.nestedBean2 = NestedBean.randomBean(faker);
        }
        te.nestedBean3 = NestedBean.randomBean(faker);
        return te;
    }

    @Id
    private String id;

    @IndexedField(type = IndexedFieldType.KEYWORD)
    private String firstName;

    @IndexedFields({
            @IndexedField(type = IndexedFieldType.KEYWORD),
            @IndexedField(type = IndexedFieldType.DOC_VALUES)
    })
    private String lastName;

    @IndexedField(type = IndexedFieldType.KEYWORD, analyzer = StandardAnalyzer.class)
    private String[] colors;

    @IndexedField(analyzer = KeywordAnalyzer.class)
    private List<String> animals;

    @IndexedField(name = "theSize")
    @SortedField
    private int size;

    @IndexedField
    private LocalDate birthDate;

    @Indexed
    private NestedBean nestedBean1;
    @Indexed("nestedBeer2")
    private NestedBean nestedBean2;
    @Indexed(flatten = true)
    private NestedBean nestedBean3;

    private boolean valid;

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

    @IndexedField(type = IndexedFieldType.KEYWORD)
    public boolean isValid() {
        return valid;
    }

    public NestedBean getNestedBean1() {
        return nestedBean1;
    }

    public NestedBean getNestedBean2() {
        return nestedBean2;
    }

    @IndexedField(type = IndexedFieldType.TEXT, analyzer = FrenchAnalyzer.class)
    @SortedField("sortedName")
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public NestedBean getNestedBean3() {
        return nestedBean3;
    }

    public static class NestedBean {

        public static NestedBean randomBean(Faker faker) {
            NestedBean res = new NestedBean();
            res.text = faker.beer().name();
            return res;
        }

        @IndexedField(type = IndexedFieldType.TEXT)
        private String text;

        public NestedBean() {
        }

        public String getText() {
            return text;
        }
    }
}
