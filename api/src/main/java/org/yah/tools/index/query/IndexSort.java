package org.yah.tools.index.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class IndexSort {

    public static final IndexSort DEFAULT = new IndexSort();

    public static IndexSort relevance() {
        return relevance(IndexSortDirection.ASC);
    }

    public static IndexSort relevance(IndexSortDirection direction) {
        return new IndexSort(IndexSortField.score(direction));
    }

    public static IndexSort indexOrder() {
        return indexOrder(IndexSortDirection.ASC);
    }

    public static IndexSort indexOrder(IndexSortDirection direction) {
        return new IndexSort(IndexSortField.indexOrder(direction));
    }

    private final List<IndexSortField> sortFields;

    public IndexSort(IndexSortField... sortFields) {
        this.sortFields = Arrays.asList(sortFields);
    }

    public IndexSort(Collection<IndexSortField> sortFields) {
        this.sortFields = List.copyOf(sortFields);
    }

    public List<IndexSortField> getSortFields() {
        return sortFields;
    }

    private enum SortableFieldType {
        SCORE,
        INDEX_ORDER,
        PROPERTY
    }

    public static class IndexSortField {

        public static IndexSortField property(String name) {
            return property(name, IndexSortDirection.ASC);
        }

        public static IndexSortField property(String name, IndexSortDirection direction) {
            return new IndexSortField(name, direction, SortableFieldType.PROPERTY);
        }

        public static IndexSortField score(IndexSortDirection direction) {
            return new IndexSortField("score", direction, SortableFieldType.SCORE);
        }

        public static IndexSortField indexOrder(IndexSortDirection direction) {
            return new IndexSortField("indexOrder", direction, SortableFieldType.INDEX_ORDER);
        }

        private final String name;
        private final IndexSortDirection direction;
        private final SortableFieldType type;

        private IndexSortField(String name, IndexSortDirection direction, SortableFieldType type) {
            this.name = Objects.requireNonNull(name);
            this.direction = Objects.requireNonNull(direction);
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public IndexSortDirection getDirection() {
            return direction;
        }

        public boolean isScore() {
            return type == SortableFieldType.SCORE;
        }

        public boolean isIndexOrder() {
            return type == SortableFieldType.INDEX_ORDER;
        }

        @Override
        public String toString() {
            return "IndexSortField{" +
                    "name='" + name + '\'' +
                    ", direction=" + direction +
                    '}';
        }
    }

    public enum IndexSortDirection {
        ASC,
        DESC
    }

}
