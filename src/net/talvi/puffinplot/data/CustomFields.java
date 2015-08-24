/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2015 Pontus Lurcock.
 *
 * PuffinPlot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PuffinPlot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a set of custom fields for sample annotation. The field list
 * is indexed by a non-negative integer, and contains values (objects) of a
 * specified type.
 * 
 * @param <T> the type of data to be stored in the fields
 */
public class CustomFields<T> {
    private List<T> values;

    /** Creates a set of custom fields with the specified values.
     * @param initialValues the values with which to initialize the fields
     */
    public CustomFields(List<T> initialValues) {
        values = new ArrayList<>(initialValues.size());
        values.addAll(initialValues);
    }

    /** Creates an empty set of custom fields. */
    public CustomFields() {
        this(Collections.EMPTY_LIST);
    }

    /**
     * Sets a specified custom field to a specified value. If the specified
     * index is outside the range of available custom fields, nothing
     * happens.
     * 
     * @param number the index of the field to set
     * @param value the value to which to set the field
     */
    public void set(int number, T value) {
        if (number < values.size()) values.set(number, value);
    }

    /**
     * Gets the value of a specified custom field. If the index is outside
     * the range of available custom fields, {@code null} is returned
     * 
     * @param number the index of a field
     * @return the value of the field with the specified index, or 
     * {@code null} if there is no such field
     */
    public T get(int number) {
        if (number >= values.size()) return null;
        return values.get(number);
    }

    /**
     * Sets the size of the list of custom fields. The current contents
     * are deleted, and the list is filled with a specified value.
     * 
     * @param newSize the new size of the custom field list
     * @param fillWith the value with which to fill the list
     */
    public void setSize(int newSize, T fillWith) {
        values = new ArrayList<>(newSize);
        values.addAll(Collections.nCopies(newSize, fillWith));
    }

    /**
     * Adds a new value to this custom field list.
     * 
     * @param position the position at which to insert the new value
     * @param value the value to insert
     */
    public void add(int position, T value) {
        values.add(position, value);
    }

    /**
     * Removes a value from this custom field list.
     * 
     * @param position the index of the value to remove
     */
    public void remove(int position) {
        values.remove(position);
    }

    /**
     * Swaps the values in two adjacent positions within this 
     * custom field list.
     * 
     * @param position the first of the positions to swap; its value will
     * be swapped with the next value
     */
    public void swapAdjacent(int position) {
        final T temp = get(position);
        set(position, get(position+1));
        set(position+1, temp);
    }

    /** Returns a string representation of the list of values.
     * @return a string representation of the list of values */
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

    /** Returns the number of values in this list.
     * @return the number of values in this list */
    public int size() {
        return values.size();
    }
}
