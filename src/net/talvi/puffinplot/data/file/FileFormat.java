package net.talvi.puffinplot.data.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.DatumField;

/**
 * This class represents an ASCII-based file format with one row per
 * measurement. It defines the mapping from column numbers to data fields,
 * and the number of header lines to be skipped. Columns are zero-indexed.
 * 
 * @author pont
 */
public class FileFormat {
    
    private final Map<Integer,DatumField> columnMap;
    private final int headerLines;
    private final String separator;
    
    public FileFormat(Map<Integer,DatumField> columnMap, int headerLines,
            String separator) {
        this.columnMap = new HashMap<Integer, DatumField>(columnMap);
        this.headerLines = headerLines;
        this.separator = separator;
    }
    
    public Datum readLine(String line) {
        final String[] fieldStrings = line.split(separator);
        final Datum datum = new Datum();
        for (int i=0; i<fieldStrings.length; i++) {
            final DatumField fieldType = columnMap.get(i);
            datum.setValue(fieldType, fieldStrings[i]);
        }
        return datum;
    }
    
    public List<Datum> readLines(List<String> lines) {
        final List<Datum> data = new ArrayList<Datum>(lines.size() - headerLines);
        for (int i=headerLines; i<lines.size(); i++) {
            data.add(readLine(lines.get(i)));
        }
        return data;
    }
}
