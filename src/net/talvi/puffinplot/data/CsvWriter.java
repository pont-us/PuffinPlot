package net.talvi.puffinplot.data;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class CsvWriter {

    private final Writer writer;
    private final String separator;

    public CsvWriter(Writer writer, String separator) {
        this.writer = writer;
        this.separator = separator;
    }

    public CsvWriter(Writer writer) {
        this(writer, ",");
    }

    public void writeCsv(Object... os) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Object o : os) {
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

    public void close() throws IOException {
        writer.close();
    }
}
