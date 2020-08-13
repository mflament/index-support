package org.yah.tools.index.query;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

public class ScoredElement<T> {

    public static <V> String print(Collection<ScoredElement<V>> elements) {
        return print(elements, Objects::toString);
    }

    public static <V> String print(Collection<ScoredElement<V>> elements,
                                   Function<V, String> elementConverter) {
        final int maxLength = elements.stream()
                .map(ScoredElement::getElement)
                .map(elementConverter)
                .mapToInt(String::length)
                .max()
                .orElse(0);
        final StringBuilder sb = new StringBuilder();
        final String format = "%-" + maxLength + "s : %.3f";
        final String ls = System.lineSeparator();
        elements.forEach(e -> sb.append(String.format(format, elementConverter.apply(e.element), e.score)).append(ls));
        return sb.toString();
    }

    private final T element;
    private final float score;

    public ScoredElement(T element, float score) {
        this.element = element;
        this.score = score;
    }

    public T getElement() {
        return element;
    }

    public float getScore() {
        return score;
    }

    @Override
    public String toString() {
        return element + ": " + score;
    }
}
