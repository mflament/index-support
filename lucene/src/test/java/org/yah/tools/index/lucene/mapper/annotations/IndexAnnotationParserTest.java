package org.yah.tools.index.lucene.mapper.annotations;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.yah.tools.index.lucene.annotations.IndexedFieldType;
import org.yah.tools.index.lucene.mapper.DefaultDocumentMapper;
import org.yah.tools.index.lucene.mapper.DefaultDocumentMapperTest;
import org.yah.tools.index.lucene.mapper.IndexedField;
import org.yah.tools.index.lucene.mapper.TestEntity;
import org.yah.tools.index.lucene.mapper.TestEntity.NestedBean;

import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class IndexAnnotationParserTest {

    private final Random random = new Random(12456789);

    @Test
    public void parse() {
        Map<String, Analyzer> analyzers = new HashMap<>();
        analyzers.put("animals", new StandardAnalyzer());

        final IndexAnnotationParser<TestEntity> parser = IndexAnnotationParser.builder(TestEntity.class)
                .withAnalyzers(analyzers)
                .withDefaultStringFieldType(IndexedFieldType.STRING)
                .withZoneOffset(ZoneOffset.ofHours(3))
                .build();

        final DefaultDocumentMapper<TestEntity> mapper = parser.parse();

        assertThat(analyzers.entrySet(), hasSize(8));
        assertThat(analyzers.get("firstName"), instanceOf(KeywordAnalyzer.class));
        assertThat(analyzers.get("lastName"), instanceOf(KeywordAnalyzer.class));
        assertThat(analyzers.get("colors"), instanceOf(StandardAnalyzer.class));
        assertThat(analyzers.get("animals"), instanceOf(StandardAnalyzer.class));
        assertThat(analyzers.get("fullName"), instanceOf(FrenchAnalyzer.class));
        assertThat(analyzers.get("nestedBean1.id"), instanceOf(KeywordAnalyzer.class));
        assertThat(analyzers.get("nestedBeer2.id"), instanceOf(KeywordAnalyzer.class));
        assertThat(analyzers.get("valid"), instanceOf(KeywordAnalyzer.class));

        final Collection<IndexedField<TestEntity>> indexedFields = DefaultDocumentMapperTest.getIndexedFields(mapper);
        final Set<String> names = indexedFields.stream()
                .map(IndexedField::getName)
                .collect(Collectors.toSet());
        assertThat(names, containsInAnyOrder("firstName", "lastName", "colors",
                "animals", "theSize", "size", "birthDate", "fullName", "sortedName",
                "nestedBean1.id", "nestedBean1.text", "nestedBeer2.id", "nestedBeer2.text", "valid"));

        TestEntity te = TestEntity.randomEntity(random);
        assertThat(mapper.getElementId(te), is(te.getId()));
        Document document = new Document();
        mapper.toDocument(te, document);

        assertThat(fields(document, "firstName"), contains(
                string(te.getFirstName())
        ));

        final List<IndexableField> lastName = fields(document, "lastName");
        assertThat(lastName, containsInAnyOrder(List.of(
                string(te.getLastName()),
                binaryDocValue(te.getLastName())
        )));

        assertThat(fields(document, "colors"), Matchers.contains(strings(te.getColors())));
        assertThat(fields(document, "animals"), contains(strings(te.getAnimals())));

        assertThat(fields(document, "theSize"), contains(indexableField(IntPoint.class, te.getSize())));
        assertThat(fields(document, "size"), contains(indexableField(SortedNumericDocValuesField.class, (long) te
                .getSize())));

        assertThat(fields(document, "birthDate"), contains(indexableField(LongPoint.class, te.getBirthDate()
                .toEpochDay())));

        assertThat(fields(document, "fullName"), contains(text(te.getFullName())));
        assertThat(fields(document, "sortedName"), contains(sortedText(te.getFullName())));

        assertThat(fields(document, "valid"), contains(string(te.isValid())));

        assertNestedBean(document, "nestedBean1", te.getNestedBean1());
        if (te.getNestedBean2() != null)
            assertNestedBean(document, "nestedBeer2", te.getNestedBean2());
        else {
            assertThat(fields(document, "nestedBeer2.id"), empty());
            assertThat(fields(document, "nestedBeer2.text"), empty());
        }
    }

    private void assertNestedBean(Document document, String name, NestedBean expected) {
        assertThat(fields(document, name + ".id"), contains(
                string(expected.getId())
        ));
        assertThat(fields(document, name + ".text"), contains(
                text(expected.getText())
        ));
    }

    private static List<IndexableField> fields(Document document, String name) {
        return Arrays.asList(document.getFields(name));
    }

    private static Matcher<? super IndexableField> string(Object value) {
        return indexableField(StringField.class, value.toString());
    }

    private static Matcher<? super IndexableField> text(Object value) {
        return indexableField(TextField.class, value.toString());
    }

    private static List<Matcher<? super IndexableField>> strings(String... values) {
        return Arrays.stream(values).map(IndexAnnotationParserTest::string).collect(Collectors.toList());
    }

    private static List<Matcher<? super IndexableField>> strings(Collection<String> values) {
        return values.stream().map(v -> indexableField(StringField.class, v)).collect(Collectors.toList());
    }

    private static Matcher<IndexableField> indexableField(Class<? extends IndexableField> expectedType, String value) {
        return IndexableFieldMatcher.indexableField(expectedType, value);
    }

    private static Matcher<IndexableField> indexableField(Class<? extends IndexableField> expectedType, Number value) {
        return IndexableFieldMatcher.indexableField(expectedType, value);
    }

    private static Matcher<IndexableField> binaryDocValue(String value) {
        return IndexableFieldMatcher
                .indexableField(BinaryDocValuesField.class, new BytesRef(value), Field::binaryValue);
    }

    private static Matcher<IndexableField> sortedText(String value) {
        return IndexableFieldMatcher.indexableField(SortedDocValuesField.class,
                new BytesRef(value),
                Field::binaryValue);
    }

    private static class IndexableFieldMatcher<T extends IndexableField> extends TypeSafeMatcher<IndexableField> {
        private final Class<T> expectedType;
        private final Matcher<T> matcher;

        public IndexableFieldMatcher(Class<T> expectedType, Matcher<T> matcher) {
            super(expectedType);
            this.expectedType = expectedType;
            this.matcher = matcher;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(expectedType.getSimpleName());
            if (matcher != null)
                description.appendText(" ").appendDescriptionOf(matcher);
        }

        @Override
        protected boolean matchesSafely(IndexableField item) {
            if (expectedType.isAssignableFrom(item.getClass())) {
                return matcher == null || matcher.matches(item);
            }
            return false;
        }

        @Override
        protected void describeMismatchSafely(IndexableField item, Description mismatchDescription) {
            if (expectedType.isAssignableFrom(item.getClass())) {
                mismatchDescription.appendDescriptionOf(matcher);
            } else
                mismatchDescription.appendValue(item.getClass());
        }

        static <T extends IndexableField> Matcher<IndexableField> indexableField(Class<T> expectedType,
                                                                                 Object expectedValue,
                                                                                 Function<T, Object> valueAccessor) {
            Matcher<T> valueMatcher = new TypeSafeMatcher<>() {
                @Override
                public void describeTo(Description description) {
                    description.appendText("with value ").appendValue(expectedValue);
                }

                @Override
                protected boolean matchesSafely(T item) {
                    return Objects.equals(valueAccessor.apply(item), expectedValue);
                }

                @Override
                protected void describeMismatchSafely(T item, Description mismatchDescription) {
                    mismatchDescription.appendText(item.getClass().getSimpleName()).appendValue(item);
                }
            };
            return new IndexableFieldMatcher<>(expectedType, valueMatcher);
        }

        static <T extends IndexableField> Matcher<IndexableField> indexableField(Class<T> expectedType,
                                                                                 String expectedValue) {
            return indexableField(expectedType, expectedValue, IndexableField::stringValue);
        }

        static <T extends IndexableField> Matcher<IndexableField> indexableField(Class<T> expectedType,
                                                                                 Number expectedValue) {
            return indexableField(expectedType, expectedValue, IndexableField::numericValue);
        }
    }
}