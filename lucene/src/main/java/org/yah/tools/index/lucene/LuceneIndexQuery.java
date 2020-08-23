package org.yah.tools.index.lucene;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.yah.tools.index.lucene.mapper.DocumentMapper;
import org.yah.tools.index.lucene.mapper.IndexableFieldType;
import org.yah.tools.index.query.IndexQuery;
import org.yah.tools.index.query.IndexSort;

import java.util.Collections;
import java.util.Set;

class LuceneIndexQuery implements IndexQuery {

    private static final LuceneIndexQuery ALL = new LuceneIndexQuery(new MatchAllDocsQuery());

    private final Query query;

    private final IndexSort sort;
    private final int limit;
    private final int skip;
    private final Set<String> projection;

    public LuceneIndexQuery(LuceneIndexQueryBuilder<?> builder) {
        this.query = builder.createLuceneQuery();
        this.sort = builder.sort;
        this.limit = builder.limit;
        this.skip = builder.skip;
        this.projection = Set.copyOf(builder.projection);
    }

    public LuceneIndexQuery(Query query) {
        this.query = query;
        this.sort = IndexSort.relevance(IndexSort.IndexSortDirection.DESC);
        this.limit = Integer.MAX_VALUE;
        this.skip = 0;
        this.projection = Collections.emptySet();
    }

    public Query getQuery() {
        return query;
    }

    public IndexSort getSort() {
        return sort;
    }

    public int getLimit() {
        return limit;
    }

    public int getSkip() {
        return skip;
    }

    public Set<String> getProjection() {
        return projection;
    }

    public static LuceneIndexQuery cast(IndexQuery query) {
        if (query == IndexQuery.ALL) return LuceneIndexQuery.ALL;
        return (LuceneIndexQuery) query;
    }

    public Sort createLuceneSort(DocumentMapper<?> documentMapper) {
        if (sort == IndexSort.DEFAULT)
            return Sort.RELEVANCE;

        final SortField[] sortFields = sort.getSortFields().stream()
                .map(f -> createLuceneSortField(documentMapper, f))
                .toArray(SortField[]::new);
        return new Sort(sortFields);
    }

    private SortField createLuceneSortField(DocumentMapper<?> documentMapper, IndexSort.IndexSortField field) {
        if (field.isScore())
            return SortField.FIELD_SCORE;
        if (field.isIndexOrder())
            return SortField.FIELD_DOC;
        boolean reverse = field.getDirection() == IndexSort.IndexSortDirection.DESC;
        final IndexableFieldType fieldType = documentMapper.getFieldType(field.getName());
        final SortField.Type sortType = getSortType(fieldType);
        return new SortField(field.getName(), sortType, reverse);
    }

    private SortField.Type getSortType(IndexableFieldType fieldType) {
        switch (fieldType) {
            case STRING:
                return SortField.Type.STRING;
            case INTEGER:
                return SortField.Type.INT;
            case LONG:
                return SortField.Type.LONG;
            case FLOAT:
                return SortField.Type.FLOAT;
            case DOUBLE:
                return SortField.Type.DOC;
            default:
                throw new IllegalArgumentException(fieldType + " is not sortable");
        }
    }

}
