/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Objects;

/**
 * This class provides a wrapper around a supplied {@link Writer} to allow easy
 * writing of lines of data with fields separated by a particular string.
 * Despite the name of the class, any single character may be used as the
 * separator.
 */
public class CsvWriter implements Closeable {

    private final Writer writer;
    private final String separator;

    /**
     * Creates a new CSV writer which will write to the specified writer
     * and separate fields with the specified string.
     * 
     * @param writer the writer to which to write
     * @param separator the separator which will be placed between the fields.
     * It must be one character long.
     * 
     * @throws NullPointerException if either of the arguments is {@code null}
     * @throws IllegalArgumentException if the length of the separator is
     *         not 1
     */
    public CsvWriter(Writer writer, String separator) {
        Objects.requireNonNull(writer);
        Objects.requireNonNull(separator);
        if (separator.length() != 1) {
            throw new IllegalArgumentException(
                    "Separator must be one character long");
        }
        this.writer = writer;
        this.separator = separator;
    }

    /**
     * Creates a new CSV writer which will write to the specified writer
     * using a comma as the field separator.
     * 
     * @param writer writer the writer to which to write
     * 
     * @throws NullPointerException if {@code writer} is {@code null}
     */
    public CsvWriter(Writer writer) {
        this(writer, ",");
    }

    
    /**
     * Turns an object into a CSV-friendly string. If the object's string
     * representation contains any instances of the separator character,
     * the whole string will be wrapped in quotation marks. If the string
     * representation contains quotation marks already, these will be 
     * doubled up, which is the usual way of escaping them in a CSV file.
     * 
     * @param o an object
     * @return a CSV-friendly representation of the supplied object
     */
    private String makeCsvString(Object o) {
        if (o == null) {
            return "null";
        }
        String s = o.toString();
        if (s.contains(("\""))) {
            s = s.replace("\"", "\"\"");
        }
        if (s.contains(separator) || s.contains("\"")) {
            s = "\"" + s + "\"";
        }
        return s;
    }
    
    /**
     * Writes a line to the writer provided to the constructor. The line
     * consists of strings representations of each of the provided objects,
     * separated by the field delimiter configured for this CSV writer.
     * If any of the supplied objects is a {@code java.util.List}, each of its
     * members will be written as an individual field (i.e. the list will
     * be automatically unpacked).
     * 
     * @param objects the objects to write
     * @throws IOException if an I/O error occurs during writing
     */
    public void writeCsv(Object... objects) throws IOException {
        final StringBuilder sb = new StringBuilder();
        for (Object o: objects) {
            if (o instanceof List) {
                final List list = (List) o;
                for (Object p: list) {
                    sb.append(makeCsvString(p));
                    sb.append(separator);
                }
            } else {
                sb.append(makeCsvString(o));
                sb.append(separator);
            }
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("\n");
        writer.write(sb.toString());
    }

    /**
     * Closes the underlying writer supplied to this CSV writer's constructor.
     * 
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        writer.close();
    }
}
