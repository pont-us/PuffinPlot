package net.talvi.puffinplot.data.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.DatumField;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.Vec3;

/**
 * This class represents an ASCII-based file format with one row per
 * measurement. It defines the mapping from column numbers to data fields,
 * and the number of header lines to be skipped. Columns are zero-indexed.
 * 
 * @author pont
 */
public class FileFormat {
    
    private final MeasType measurementType;
    private final TreatType treatmentType;
    private final Map<Integer,DatumField> columnMap;
    private final int headerLines;
    private final String separator;
    
    public FileFormat(Map<Integer,DatumField> columnMap, int headerLines,
            MeasType measurementType, TreatType treatmentType,
            String separator) {
        this.columnMap = new HashMap<Integer, DatumField>(columnMap);
        this.headerLines = headerLines;
        this.separator = separator;
        this.measurementType = measurementType;
        this.treatmentType = treatmentType;
    }
    
    private double safeParse(String s) {
        double result = 0;
        try {
            result = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            // do nothing
        }
        return result;
    }
    
    public Datum readLine(String line) {
        final String[] fieldStrings = line.split(separator);
        final Datum datum = new Datum();
        datum.setMeasType(measurementType);
        datum.setTreatType(treatmentType);
        datum.setDepth("0");
        datum.setDiscreteId("UNKNOWN");
        double dec = Double.NaN, inc = Double.NaN, intensity = Double.NaN;
        for (int i=0; i<fieldStrings.length; i++) {
            final DatumField fieldType = columnMap.get(i);
            String valueString = fieldStrings[i];
            switch (fieldType) {
                case VIRT_MAGNETIZATION:
                    intensity = safeParse(valueString);
                    break;
                case VIRT_DECLINATION:
                    dec = safeParse(valueString);
                    break;
                case VIRT_INCLINATION:
                    inc = safeParse(valueString);
                    break; 
                default:
                    datum.setValue(fieldType, fieldStrings[i]);
            }
        }
        if (!(Double.isNaN(dec) || Double.isNaN(inc) || Double.isNaN(intensity))) {
            datum.setMoment(Vec3.fromPolarDegrees(intensity, inc, dec));
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
