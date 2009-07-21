package net.talvi.puffinplot.data;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class CsvWriter {
    private Writer writer;

    public CsvWriter(Writer writer) {
        this.writer = writer;
    }

    public void writeCsv(Object... os) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Object o: os) {
            if (o instanceof List) {
                List list = (List) o;
                for (Object p: list) {
                    sb.append(p);
                    sb.append(",");
                }
            } else {
                sb.append(o);
                sb.append(",");
            }
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append("\n");
        writer.write(sb.toString());
    }

    public void close() throws IOException {
        writer.close();
    }
}
