package org.yah.tools.index;

import org.yah.tools.index.query.IndexCursor;
import org.yah.tools.index.query.IndexQuery;
import org.yah.tools.index.query.IndexQueryBuilder;
import org.yah.tools.index.query.ScoredElement;

import java.util.*;

public interface IndexReader<T> {

    int count();

    IndexQueryBuilder prepareQuery();

    IndexCursor<T> query(IndexQuery query, int batchSize);

    List<T> list(IndexQuery query);

    Optional<T> findFirst(IndexQuery query);

    int count(IndexQuery query);

    IndexCursor<ScoredElement<T>> scoredQuery(IndexQuery query, int batchSize);

    List<ScoredElement<T>> scoredList(IndexQuery query);

}
