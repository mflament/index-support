package org.yah.tools.index.query;

import java.util.Set;

public interface IndexQueryBuilder {

    default IndexQueryBuilder withProjection(String... fields) {
        return withProjection(Set.of(fields));
    }

    IndexQueryBuilder withProjection(Set<String> projection);

    default IndexQueryBuilder addProjection(String... fields) {
        return addProjection(Set.of(fields));
    }

    IndexQueryBuilder addProjection(Set<String> projection);

    IndexQueryBuilder limit(int limit);

    IndexQueryBuilder skip(int skip);

    IndexQueryBuilder sort(IndexSort sort);

    IndexQueryBuilder withKeyword(String fieldName, String keyword, Occur occur, float boost);

    default IndexQueryBuilder withKeyword(String fieldName, String keyword) {
        return withKeyword(fieldName, keyword, Occur.SHOULD, 1);
    }

    default IndexQueryBuilder withKeyword(String fieldName, String keyword, Occur occur) {
        return withKeyword(fieldName, keyword, occur, 1);
    }

    IndexQueryBuilder withTerms(String fieldName, String terms, Occur occur, TermOccur termsOccur, float boost);

    default IndexQueryBuilder withTerms(String fieldName, String terms) {
        return withTerms(fieldName, terms, Occur.SHOULD, TermOccur.SHOULD, 1);
    }

    default IndexQueryBuilder withTerms(String fieldName, String terms, Occur occur) {
        return withTerms(fieldName, terms, occur, TermOccur.SHOULD, 1);
    }

    default IndexQueryBuilder withTerms(String fieldName, String terms, Occur occur, TermOccur termOccur) {
        return withTerms(fieldName, terms, occur, termOccur, 1);
    }

    IndexQueryBuilder withPhrase(String fieldName, String phrase, Occur occur, int slope, float boost);

    default IndexQueryBuilder withPhrase(String fieldName, String phrase, Occur occur, int slope) {
        return withPhrase(fieldName, phrase, occur, slope, 1);
    }

    default IndexQueryBuilder withPhrase(String fieldName, String phrase, Occur occur) {
        return withPhrase(fieldName, phrase, occur, 1, 1);
    }

    default IndexQueryBuilder withPhrase(String fieldName, String phrase) {
        return withPhrase(fieldName, phrase, Occur.SHOULD, 1, 1);
    }

    <R> IndexQueryBuilder withRange(String fieldName, R min, R max, Occur occur, float boost);

    default <R> IndexQueryBuilder withRange(String fieldName, R min, R max, Occur occur) {
        return withRange(fieldName, min, max, occur, 1);
    }

    default <R> IndexQueryBuilder withRange(String fieldName, R min, R max) {
        return withRange(fieldName, min, max, Occur.SHOULD, 1);
    }

    IndexQueryBuilder withQuery(IndexQuery query, Occur occur, float boost);

    default IndexQueryBuilder withQuery(IndexQuery query, Occur occur) {
        return withQuery(query, occur, 1);
    }

    default IndexQueryBuilder withQuery(IndexQuery query) {
        return withQuery(query, Occur.SHOULD, 1);
    }

    IndexQueryBuilder withQuery(String defaultField, String query, Occur occur, float boost);

    default IndexQueryBuilder withQuery(String defaultField, String query, Occur occur) {
        return withQuery(defaultField, query, occur, 1);
    }

    default IndexQueryBuilder withQuery(String defaultField, String query) {
        return withQuery(defaultField, query, Occur.SHOULD, 1);
    }

    IndexQuery build();

    enum Occur {
        MUST,
        MUST_NOT,
        SHOULD,
        FILTER
    }

    enum TermOccur {
        MUST,
        SHOULD
    }

}
