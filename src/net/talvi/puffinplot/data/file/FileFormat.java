/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
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
package net.talvi.puffinplot.data.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.Util;
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
public final class FileFormat {
    
    private final MeasType measurementType;
    private final TreatType treatmentType;
    private final Map<Integer,DatumField> columnMap;
    private final int headerLines;
    private final String separator;
    private final boolean useFixedWidthColumns;
    private final List<Integer> columnWidths;
    private final static String prefsPrefix = "fileformat";
    private final static String[] emptyStringArray = {};
    
    /**
     * Creates a new file format with the specified parameters.
     * 
     * @param columnMap a mapping from column numbers (0-indexed) to data fields
     * @param headerLines number of header lines to skip
     * @param measurementType type of all measurements in file
     * @param treatmentType type of all treatments in file
     * @param separator column separator for non-fixed-width-column formats
     * @param useFixedWidthColumns whether this format uses fixed-width columns
     * @param columnWidths the widths of columns for fixed-width-column formats
     */
    public FileFormat(Map<Integer,DatumField> columnMap, int headerLines,
            MeasType measurementType, TreatType treatmentType,
            String separator, boolean useFixedWidthColumns,
            List<Integer> columnWidths) {
        this.columnMap = new HashMap<Integer, DatumField>(columnMap);
        this.headerLines = headerLines;
        this.separator = separator;
        this.measurementType = measurementType;
        this.treatmentType = treatmentType;
        this.useFixedWidthColumns = useFixedWidthColumns;
        this.columnWidths = columnWidths;
    }
    
    private String[] splitLine(String line) {
        if (useFixedWidthColumns) {
            List<String> result = new ArrayList<String>(columnWidths.size());
            int start = 0; // start of current column
            for (int width: columnWidths) {
                result.add(line.substring(start, start+width));
                start += width;
            }
            return result.toArray(emptyStringArray);
        } else {
            return line.split(separator);
        }
    }
    
    /**
     * Creates a {@link Datum} from a line formatted according to this format.
     * 
     * @param line a line formatted according this this format
     * @return the datum defined by the supplied line
     */
    public Datum readLine(String line) {
        final String[] fieldStrings = splitLine(line);
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
                    intensity = Util.parseDoubleSafely(valueString);
                    break;
                case VIRT_DECLINATION:
                    dec = Util.parseDoubleSafely(valueString);
                    break;
                case VIRT_INCLINATION:
                    inc = Util.parseDoubleSafely(valueString);
                    break; 
                default:
                    datum.setValue(fieldType, fieldStrings[i]);
            }
        }
        if (!(Double.isNaN(dec) || Double.isNaN(inc))) {
            if (Double.isNaN(intensity)) {
                intensity = 1;
            }
            datum.setMoment(Vec3.fromPolarDegrees(intensity, inc, dec));
        }
        return datum;
    }
    
    /**
     * Reds a list of lines in this format and produces the corresponding
     * {@link Datum}s.
     * 
     * @param lines a list of lines in this format
     * @return the data defined by the lines (in the same order)
     */
    public List<Datum> readLines(List<String> lines) {
        final List<Datum> data = new ArrayList<Datum>(lines.size() - headerLines);
        for (int i=headerLines; i<lines.size(); i++) {
            data.add(readLine(lines.get(i)));
        }
        return data;
    }
    
    /**
     * Turns a string containing comma-separated decimal integers into a
     * {@link List} of {@link Integer.
     * 
     * @param widthString a string of comma-separated decimal integers
     * @return the list of integers defined by the input string
     */
    public static List<Integer> convertStringToColumnWidths(String widthString) {
        String[] widths = widthString.split(", *");
        List<Integer> result = new ArrayList<Integer>(widths.length);
        for (String wString: widths) {
            if ("".equals(wString)) continue;
            try {
                result.add(Integer.parseInt(wString));
            } catch (NumberFormatException ex) {
                // ignore ill-formed fields
            }
        }
        return result;
    }
    
    /**
     * @return the column widths for this format as a string of 
     * comma-separated decimal integers
     */
    public String getColumnWidthsAsString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int width: columnWidths) {
            if (!first) sb.append(",");
            sb.append(Integer.toString(width));
            first = false;
        }
        return sb.toString();
    }
    
    /**
     * Saves this format to a preferences object.
     * 
     * @param prefs the preferences to which to save this format
     */
    public void writeToPrefs(Preferences prefs) {
        final String pp = prefsPrefix;
        prefs.put(pp+".separator", separator);
        prefs.putInt(pp+".headerLines", headerLines);
        prefs.put(pp+".measType", measurementType.toString());
        prefs.put(pp+".treatType", treatmentType.toString());
        prefs.putBoolean(pp+".useFixedWidth", useFixedWidthColumns);
        prefs.put(pp+".columnWidths", getColumnWidthsAsString());
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
    
    /**
     * Creates a format from a preferences object.
     * 
     * @param prefs a preferences object containing the data for a format
     * @return the corresponding format
     */
    public static FileFormat readFromPrefs(Preferences prefs) {
        final String pp = prefsPrefix;
        final String separator = prefs.get(pp+".separator", "\t");
        final int headerLines = prefs.getInt(pp+".headerLines", 0);
        final MeasType measType =
                MeasType.valueOf(prefs.get(pp+".measType", "CONTINUOUS"));
        final TreatType treatType =
                TreatType.valueOf(prefs.get(pp+".treatType", "DEGAUSS_XYZ"));
        final boolean useFixedWidth = prefs.getBoolean(pp+".useFixedWidth", false);
        final List<Integer> columnWidths =
                convertStringToColumnWidths(prefs.get(pp+".columnWidths", ""));
        final String columnString = prefs.get(pp+".columnMap", "");
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
        return new FileFormat(columnMap, headerLines, measType, treatType,
                separator, useFixedWidth, columnWidths);
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
    
    /**
     * @return {@code true} if the format uses fixed-width columns
     */
    public boolean useFixedWidthColumns() {
        return useFixedWidthColumns;
    }
    
    public boolean specifiesFullVector() {
        return (columnMap.containsValue(DatumField.X_MOMENT) &&
                columnMap.containsValue(DatumField.Y_MOMENT) &&
                columnMap.containsValue(DatumField.Z_MOMENT)) ||
                (columnMap.containsValue(DatumField.VIRT_INCLINATION) &&
                columnMap.containsValue(DatumField.VIRT_DECLINATION) &&
                columnMap.containsValue(DatumField.VIRT_MAGNETIZATION));
    }
    
    public boolean specifiesDirection() {
        return specifiesFullVector() ||
                (columnMap.containsValue(DatumField.VIRT_INCLINATION) &&
                columnMap.containsValue(DatumField.VIRT_DECLINATION));
    }
}
