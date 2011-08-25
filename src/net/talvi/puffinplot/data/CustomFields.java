package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a set of custom fields for sample annotation.
 */
public class CustomFields<T> {
    private List<T> values;

    public CustomFields(List<T> initialValues) {
        values = new ArrayList<T>(initialValues.size());
        values.addAll(initialValues);
    }

    CustomFields() {
        this(Collections.EMPTY_LIST);
    }

    public void set(int number, T value) {
        if (number < values.size()) values.set(number, value);
    }

    public T get(int number) {
        if (number >= values.size()) return null;
        return values.get(number);
    }

    public void setSize(int newSize, T fillWith) {
        values = new ArrayList<T>(newSize);
        values.addAll(Collections.nCopies(newSize, fillWith));
    }

    public void add(int position, T value) {
        values.add(position, value);
    }

    public void remove(int position) {
        values.remove(position);
    }

    public void swapAdjacent(int position) {
        final T temp = get(position);
        set(position, get(position+1));
        set(position+1, temp);
    }

    public String exportAsString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (T value: values) {
            if (!first) sb.append("\t");
            sb.append(value.toString());
            first = false;
        }
        return sb.toString();
    }

    public int size() {
        return values.size();
    }
}
