package org.yah.tools.index.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.yah.tools.index.lucene.mapper.IndexableFieldType;

import java.util.function.Function;

public class MappedEntityQueryParser extends QueryParser {

    private final Function<String, IndexableFieldType> fieldTypeSource;

    public MappedEntityQueryParser(String f, Analyzer a, Function<String, IndexableFieldType> fieldTypeSource) {
        super(f, a);
        this.fieldTypeSource = fieldTypeSource;
        setAutoGeneratePhraseQueries(false);
    }

    @Override
    protected Query newRangeQuery(String field, String part1, String part2, boolean startInclusive, boolean endInclusive) {
        final IndexableFieldType type = fieldTypeSource.apply(field);
        switch (type) {
            case INTEGER:
                return intRangeQuery(field, part1, part2, startInclusive, endInclusive);
            case LONG:
                return longRangeQuery(field, part1, part2, startInclusive, endInclusive);
            case FLOAT:
                return floatRangeQuery(field, part1, part2, startInclusive, endInclusive);
            case DOUBLE:
                return doubleRangeQuery(field, part1, part2, startInclusive, endInclusive);
            default:
                return super.newRangeQuery(field, part1, part2, startInclusive, endInclusive);
        }
    }

    private Query intRangeQuery(String field, String part1, String part2, boolean startInclusive, boolean endInclusive) {
        int min = parseInt(part1, Integer.MIN_VALUE);
        int max = parseInt(part2, Integer.MAX_VALUE);
        if (!startInclusive && min != Integer.MIN_VALUE) min++;
        if (!endInclusive && max != Integer.MAX_VALUE) max--;
        return IntPoint.newRangeQuery(field, min, max);
    }

    private Query longRangeQuery(String field, String part1, String part2, boolean startInclusive, boolean endInclusive) {
        long min = parseLong(part1, Long.MIN_VALUE);
        long max = parseLong(part2, Long.MAX_VALUE);
        if (!startInclusive && min != Long.MIN_VALUE) min++;
        if (!endInclusive && max != Long.MAX_VALUE) max--;
        return LongPoint.newRangeQuery(field, min, max);
    }

    private Query floatRangeQuery(String field, String part1, String part2, boolean startInclusive, boolean endInclusive) {
        float max = parseFloat(part2, Float.MAX_VALUE);
        float min = parseFloat(part1, Float.MIN_VALUE);
        if (!startInclusive && min != Float.MIN_VALUE) min = Math.nextUp(min);
        if (!endInclusive && max != Float.MAX_VALUE) max = Math.nextDown(max);
        return FloatPoint.newRangeQuery(field, min, max);
    }

    private Query doubleRangeQuery(String field, String part1, String part2, boolean startInclusive, boolean endInclusive) {
        double min = parseDouble(part1, Double.MIN_VALUE);
        double max = parseDouble(part2, Double.MAX_VALUE);
        if (!startInclusive && min != Double.MIN_VALUE) min = Math.nextUp(min);
        if (!endInclusive && max != Double.MAX_VALUE) max = Math.nextDown(max);
        return DoublePoint.newRangeQuery(field, min, max);
    }

    private int parseInt(String s, int bound) {
        if (s == null) return bound;
        return Integer.parseInt(s);
    }

    private long parseLong(String s, long bound) {
        if (s == null) return bound;
        return Long.parseLong(s);
    }

    private float parseFloat(String s, float bound) {
        if (s == null) return bound;
        return Float.parseFloat(s);
    }

    private double parseDouble(String s, double bound) {
        if (s == null) return bound;
        return Double.parseDouble(s);
    }
}
