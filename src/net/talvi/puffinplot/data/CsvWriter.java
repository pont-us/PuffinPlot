package net.talvi.puffinplot.data;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * This class provides a wrapper around a supplied {@link Writer} to 
 * allow easy writing of lines of data delimited by a particular string.
 * Despite the name of the class, any string may be used as the delimiter.
 * 
 * @author pont
 */
public class CsvWriter {

    private final Writer writer;
    private final String separator;

    /**
     * Creates a new CSV writer which will write to the specified writer
     * and delimit fields with the specified string.
     * 
     * @param writer the writer to which to write
     * @param separator the separator which will be placed between the fields
     */
    public CsvWriter(Writer writer, String separator) {
        this.writer = writer;
        this.separator = separator;
    }

    /**
     * Creates a new CSV writer which will write to the specified writer
     * using a comma as the field delimiter.
     * 
     * @param writer writer the writer to which to write
     */
    public CsvWriter(Writer writer) {
        this(writer, ",");
    }

    /**
     * Writes a line to the writer provided to the constructor. The line
     * consists of strings representations of each of the provided objects,
     * separated by the field delimiter configured for this CSV writer.
     * 
     * @param objects the objects to write
     * @throws IOException if an I/O error occurs during writing
     */
    public void writeCsv(Object... objects) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Object o : objects) {
            if (o instanceof List) {
                List list = (List) o;
                for (Object p : list) {
                    sb.append(p);
                    sb.append(separator);
                }
            } else {
                sb.append(o);
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
