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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot. If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.LinkedList;
import net.talvi.puffinplot.data.Datum;
import java.util.Map;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.Vec3;

/**
 * Loader for IAPD files.
 * 
 * @author pont
 */
public class IapdLoader extends AbstractFileLoader {
    
    private LineNumberReader reader;
    private final File file;
    private final Map<Object, Object> importOptions;
    
    /**
     * Creates a new IapdLoader.
     * 
     * Valid import option keys are:
     * 
     * {@code TreatType.class}; value must be an instance of {@link TreatType}
     * {@code MeasType.class}; value must be an instance of {@link MeasType}
     * 
     * These keys respectively specify the treatment type and measurement
     * type for the data in the file. If they are omitted, defaults will
     * be used.
     * 
     * @param file the file from which to read data
     * @param importOptions import options for reading the data
     */
    public IapdLoader(File file, Map<Object,Object> importOptions) {
        this.file = file;
        data = new LinkedList<>();
        this.importOptions = importOptions;
        try {
            reader = new LineNumberReader(new FileReader(file));
            readFile();
        } catch (IOException e) {
            messages.add("Error reading \"" + file.getName() + "\"");
            messages.add(e.getMessage());
        }
    }
    
    private static class ParsedDoubles {
        public final boolean success;
        private final double[] values;
        
        private ParsedDoubles(boolean success, double[] values) {
            this.success = success;
            this.values = values;
        }
        
        public static ParsedDoubles parse(String[] strings, double[] defaults,
                int startAt) {
            boolean success = true;
            double[] values = new double[defaults.length];
            for (int i=startAt; i<defaults.length; i++) {
                values[i] = defaults[i];
                try {
                    values[i] = Double.parseDouble(strings[i]);
                } catch (NumberFormatException |
                        ArrayIndexOutOfBoundsException e) {
                    success = false;
                }
            }
            return new ParsedDoubles(success, values);
        }
        
        public double get(int i) {
            return values[i];
        }
    }

    private void readFile() throws IOException {
        TreatType treatType = TreatType.DEGAUSS_XYZ;
        MeasType measType = MeasType.DISCRETE;
        if (importOptions.containsKey(TreatType.class)) {
            treatType = (TreatType) importOptions.get(TreatType.class);
        }
        if (importOptions.containsKey(MeasType.class)) {
            measType = (MeasType) importOptions.get(MeasType.class);
        }
        
        final String headerLine = reader.readLine();
        if (headerLine == null) {
            addMessage("%s is empty", file.getName());
            return;
        }
        final String[] header = headerLine.trim().split(" +");
        String sampleName = "Unknown";
        if (header.length > 0) {
            if (header[0].isEmpty()) {
                addMessage("No sample name in \"%s\"", file.getName());
            } else {
                sampleName = header[0];
            }
        } else {
            // Should never happen, since split should always return
            // a non-empty array, but best to cover it just in case.
            addMessage("No header data in \"%s\"", file.getName());
        }
        
        final ParsedDoubles headerValues = 
                ParsedDoubles.parse(header,
                        new double[] {0, 0, 90, 0, 0, 10}, 1);
        
        if (!headerValues.success) {
            addMessage("Malformed header in \"%s\"", file.getName());
        }
        
        String line;
        double a95max = 0;
        boolean success = true;
        final double[] defaults = {0, 0, 0, 0, 0, 0, 0};
        while ((line = reader.readLine()) != null) {
            final Datum d = new Datum();
            final String[] parts = line.trim().split(" +");
            
            final ParsedDoubles fields = ParsedDoubles.parse(parts, defaults, 0);
            success = success && fields.success;
            final double treatmentLevel = fields.get(0);
            
            switch (treatType) {
                case THERMAL:
                    d.setTemp(treatmentLevel);
                    break;
                case ARM:
                case DEGAUSS_XYZ:
                    d.setAfX(treatmentLevel / 1000);
                    d.setAfY(treatmentLevel / 1000);
                    d.setAfZ(treatmentLevel / 1000);
                    break;
                case DEGAUSS_Z:
                    d.setAfZ(treatmentLevel / 1000);
                    break;
                case IRM:
                    d.setIrmField(treatmentLevel / 1000);
                    break;
            }
            
            d.setMoment(Vec3.fromPolarDegrees(fields.get(1) / 1000,
                    fields.get(6), fields.get(5)));
            d.setDiscreteId(sampleName);
            d.setSampAz(headerValues.get(1));
            d.setSampDip(headerValues.get(2));
            d.setFormAz(headerValues.get(3));
            d.setFormDip(headerValues.get(4));
            d.setVolume(headerValues.get(5));
            d.setTreatType(treatType);
            d.setMeasType(measType);
            final double a95 = fields.get(4);
            if (a95 > a95max) {
                a95max = a95;
            }
            data.add(d);
        }
        if (!success) {
            addMessage("Malformed data fields in \"%s\".", file.getName());
        }
        if (a95max >= 5) {
            addMessage("File \"%s\" has high α95 values (max. %.1f).",
                    file.getName(), a95max);
        }
    }
}
