package net.talvi.puffinplot.data.file;

import java.util.*;
import java.util.Map.Entry;
import java.util.prefs.Preferences;
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
    private final static String prefsPrefix = "fileformat";
    public final static FileFormat DEFAULT = 
            new FileFormat(new HashMap<Integer, DatumField>(0),
            0, MeasType.CONTINUOUS, TreatType.DEGAUSS_XYZ,
            "\t");
    
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
            if (!columnMap.containsKey(i)) continue;
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
    
    public void writeToPrefs(Preferences prefs) {
        prefs.put(prefsPrefix+".separator", separator);
        prefs.putInt(prefsPrefix+".headerlines", headerLines);
        prefs.put(prefsPrefix+".meastype", measurementType.toString());
        prefs.put(prefsPrefix+".treatType", treatmentType.toString());
        StringBuilder fieldsBuilder = new StringBuilder();
        boolean first = true;
        for (Entry<Integer, DatumField> entry: columnMap.entrySet()) {
            if (!first) fieldsBuilder.append("\t");
            fieldsBuilder.append(Integer.toString(entry.getKey()));
            fieldsBuilder.append(",");
            fieldsBuilder.append(entry.getValue().toString());
            first = false;
        }
        prefs.put(prefsPrefix+".columnMap", fieldsBuilder.toString());
    }
    
    public static FileFormat readFromPrefs(Preferences prefs) {
        final String separator = prefs.get(prefsPrefix+".separator", "\t");
        final int headerLines = prefs.getInt(prefsPrefix+".headerlines", 0);
        final MeasType measType =
                MeasType.valueOf(prefs.get(prefsPrefix+".measType", "CONTINUOUS"));
        final TreatType treatType =
                TreatType.valueOf(prefs.get(prefsPrefix+".treatType", "DEGAUSS_XYZ"));
        final String columnString = prefs.get(prefsPrefix+".columnMap", "");
        final String[] columnDefs = columnString.split("\t");
        final Map<Integer, DatumField> columnMap =
                new LinkedHashMap<Integer, DatumField>(columnDefs.length);
        for (String columnDef: columnDefs) {
            if ("".equals(columnDef)) continue;
            final String[] parts = columnDef.split(",");
            final int column = Integer.parseInt(parts[0]);
            final DatumField field = DatumField.valueOf(parts[1]);
            columnMap.put(column, field);
        }
        return new FileFormat(columnMap, headerLines, measType, treatType, separator);
    }

    /**
     * @return the measurement type for the file
     */
    public MeasType getMeasurementType() {
        return measurementType;
    }

    /**
     * @return the treatment type for the file
     */
    public TreatType getTreatmentType() {
        return treatmentType;
    }

    /**
     * @return the mapping between column numbers and data field types
     */
    public Map<Integer,DatumField> getColumnMap() {
        return columnMap;
    }

    /**
     * @return the number of header lines to skip at the start of the file
     */
    public int getHeaderLines() {
        return headerLines;
    }

    /**
     * @return a regular expression matching the column separator
     */
    public String getSeparator() {
        return separator;
    }
}
