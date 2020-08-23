package org.yah.tools.index.lucene;

import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.util.QueryBuilder;
import org.yah.tools.index.lucene.mapper.IndexableFieldType;
import org.yah.tools.index.query.IndexQuery;
import org.yah.tools.index.query.IndexQueryBuilder;
import org.yah.tools.index.query.IndexSort;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

class LuceneIndexQueryBuilder<T> implements IndexQueryBuilder {
    protected final Set<String> projection = new HashSet<>();
    protected final LuceneIndex<T> index;

    private final BooleanQuery.Builder queryBuilder;
    private final QueryBuilder fieldQueryBuilder;
    private final Function<String, IndexableFieldType> fieldTypeSource;

    protected IndexSort sort = IndexSort.DEFAULT;
    protected int limit = Integer.MAX_VALUE;
    protected int skip = 0;

    LuceneIndexQueryBuilder(LuceneIndex<T> index) {
        this.index = Objects.requireNonNull(index);
        queryBuilder = new BooleanQuery.Builder();
        fieldQueryBuilder = new QueryBuilder(index.analyzer);
        fieldTypeSource = index.documentMapper::getFieldType;
    }

    @Override
    public IndexQueryBuilder withProjection(Set<String> projection) {
        this.projection.clear();
        return addProjection(projection);
    }

    @Override
    public IndexQueryBuilder addProjection(Set<String> projection) {
        this.projection.addAll(projection);
        return this;
    }

    @Override
    public IndexQueryBuilder limit(int limit) {
        if (limit <= 0) throw new IllegalArgumentException("limit " + limit + " must be > 0");
        this.limit = limit;
        return this;
    }

    @Override
    public IndexQueryBuilder skip(int skip) {
        if (skip < 0) throw new IllegalArgumentException("skip " + skip + " must be >= 0");
        this.skip = skip;
        return this;
    }

    @Override
    public IndexQueryBuilder sort(IndexSort sort) {
        this.sort = Objects.requireNonNull(sort);
        return this;
    }

    @Override
    public IndexQueryBuilder withTerm(String fieldName, String term, Occur occur, float boost) {
        queryBuilder.add(boost(new TermQuery(new Term(fieldName, term)), boost), createLuceneOccur(occur));
        return this;
    }

    @Override
    public IndexQueryBuilder withTerms(String fieldName, String terms, Occur occur, TermOccur termsOccur, float boost) {
        final Query termsQuery = fieldQueryBuilder.createBooleanQuery(fieldName, terms, createLuceneOccur(termsOccur));
        queryBuilder.add(boost(termsQuery, boost), createLuceneOccur(occur));
        return this;
    }

    @Override
    public IndexQueryBuilder withPhrase(String fieldName, String phrase, Occur occur, int slope, float boost) {
        final Query termsQuery = fieldQueryBuilder.createPhraseQuery(fieldName, phrase, slope);
        queryBuilder.add(boost(termsQuery, boost), createLuceneOccur(occur));
        return this;
    }

    @Override
    public <R> IndexQueryBuilder withRange(String fieldName, R min, R max, Occur occur, float boost) {
        if (min == null && max == null)
            throw new IllegalArgumentException("min and max are null");
        Class<?> type = min != null ? min.getClass() : max.getClass();
        Query rangeQuery;
        if (Integer.class.isAssignableFrom(type)) {
            rangeQuery = IntPoint.newRangeQuery(fieldName,
                    cast(min, Integer.MIN_VALUE),
                    cast(max, Integer.MAX_VALUE));
        } else if (Long.class.isAssignableFrom(type)) {
            rangeQuery = LongPoint.newRangeQuery(fieldName,
                    cast(min, Long.MIN_VALUE),
                    cast(max, Long.MAX_VALUE));
        } else if (Float.class.isAssignableFrom(type)) {
            rangeQuery = FloatPoint.newRangeQuery(fieldName,
                    cast(min, Float.MIN_VALUE),
                    cast(max, Float.MAX_VALUE));
        } else if (Double.class.isAssignableFrom(type)) {
            rangeQuery = DoublePoint.newRangeQuery(fieldName,
                    cast(min, Double.MIN_VALUE),
                    cast(max, Double.MAX_VALUE));
        } else if (LocalDate.class.isAssignableFrom(type)) {
            rangeQuery = LongPoint.newRangeQuery(fieldName,
                    cast(min, LocalDate::toEpochDay, Long.MIN_VALUE),
                    cast(max, LocalDate::toEpochDay, Long.MAX_VALUE));
        } else if (Instant.class.isAssignableFrom(type)) {
            rangeQuery = LongPoint.newRangeQuery(fieldName,
                    cast(min, Instant::toEpochMilli, Long.MIN_VALUE),
                    cast(max, Instant::toEpochMilli, Long.MAX_VALUE));
        } else {
            throw new IllegalArgumentException("Unhandled RangeQuery type " + type.getName());
        }
        queryBuilder.add(boost(rangeQuery, boost), createLuceneOccur(occur));
        return this;
    }

    @Override
    public IndexQueryBuilder withQuery(IndexQuery query, Occur occur, float boost) {
        LuceneIndexQuery luceneIndexQuery = LuceneIndexQuery.cast(query);
        return withQuery(luceneIndexQuery.getQuery(), occur, boost);
    }

    @Override
    public IndexQueryBuilder withQuery(String defaultField, String query, Occur occur, float boost) {
        final Query parsedQuery;
        try {
            parsedQuery = new MappedEntityQueryParser(defaultField, index.analyzer, fieldTypeSource).parse(query);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
        withQuery(parsedQuery, occur, boost);
        return this;
    }

    @Override
    public IndexQuery build() {
        return new LuceneIndexQuery(this);
    }

    Query createLuceneQuery() {
        final BooleanQuery query = queryBuilder.build();
        if (query.clauses().isEmpty())
            return new MatchAllDocsQuery();
        return query;
    }

    private IndexQueryBuilder withQuery(Query query, Occur occur, float boost) {
        queryBuilder.add(boost(query, boost), createLuceneOccur(occur));
        return this;
    }

    private static Query boost(Query query, float boost) {
        if (boost != 1)
            return new BoostQuery(query, boost);
        return query;
    }

    private static BooleanClause.Occur createLuceneOccur(Occur occur) {
        switch (occur) {
            case MUST:
                return BooleanClause.Occur.MUST;
            case MUST_NOT:
                return BooleanClause.Occur.MUST_NOT;
            case SHOULD:
                return BooleanClause.Occur.SHOULD;
            case FILTER:
                return BooleanClause.Occur.FILTER;
            default:
                throw new IllegalArgumentException("Unsupported " + occur);
        }
    }

    private static BooleanClause.Occur createLuceneOccur(TermOccur termsOccur) {
        switch (termsOccur) {
            case MUST:
                return BooleanClause.Occur.MUST;
            case SHOULD:
                return BooleanClause.Occur.SHOULD;
            default:
                throw new IllegalArgumentException("Unsupported " + termsOccur);
        }
    }

    private static <T> T cast(Object value, T defaultValue) {
        if (value == null) return defaultValue;
        //noinspection unchecked
        return (T) value;
    }

    private static <V, T> T cast(Object value, Function<V, T> convert, T defaultValue) {
        if (value == null) return defaultValue;
        //noinspection unchecked
        return convert.apply((V) value);
    }

}
