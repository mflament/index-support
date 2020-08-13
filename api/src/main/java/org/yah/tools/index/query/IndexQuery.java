package org.yah.tools.index.query;

import java.util.Collections;
import java.util.Set;

@SuppressWarnings("SameReturnValue")
public interface IndexQuery {

    IndexQuery ALL = new IndexQuery() {
        @Override
        public IndexSort getSort() {
            return IndexSort.indexOrder(IndexSort.IndexSortDirection.ASC);
        }

        @Override
        public int getLimit() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getSkip() {
            return 0;
        }

        @Override
        public Set<String> getProjection() {
            return Collections.emptySet();
        }
    };

    IndexSort getSort();

    int getLimit();

    int getSkip();

    Set<String> getProjection();

}
