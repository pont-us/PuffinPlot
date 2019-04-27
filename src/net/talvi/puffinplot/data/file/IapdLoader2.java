/* This file is part of PuffinPlot, a program for palaeomagnetic
 * treatmentSteps plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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
import java.util.Map;

import net.talvi.puffinplot.data.MeasurementType;
import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.Vec3;

/**
 * Loader for IAPD files.
 * 
 * @author pont
 */
public class IapdLoader2 implements FileLoader2 {
    
    /**
     * Reads an IAPD file.
     *
     * Valid import option keys are:
     *
     * {@code TreatmentType.class}; value must be an instance of
     * {@link TreatmentType}
     * {@code MeasurementType.class}; value must be an instance of
     * {@link MeasurementType}
     *
     * These keys respectively specify the treatment type and measurement type
     * for the treatmentSteps in the file. If they are omitted, defaults will be
     * used.
     *
     * @param file the file from which to read treatmentSteps
     * @param importOptions import options for reading the treatmentSteps
     */
    @Override
    public LoadedData readFile(File file, Map<Object,Object> importOptions) {
        try (LineNumberReader reader =
                new LineNumberReader(new FileReader(file))) {
            return readFile(reader, importOptions, file.getName());
        } catch (IOException e) {
            return new EmptyLoadedData("Error reading \"" + file.getName() +
                    "\": " +e.getLocalizedMessage());
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

    private LoadedData readFile(LineNumberReader reader,
            Map<Object, Object> importOptions, String filename)
            throws IOException {
        final SimpleLoadedData loadedData = new SimpleLoadedData();
        
        final TreatmentType treatmentType =
                (TreatmentType) importOptions.getOrDefault(
                        TreatmentType.class, TreatmentType.DEGAUSS_XYZ);
        final MeasurementType measurementType =
                (MeasurementType) importOptions.getOrDefault(
                        MeasurementType.class, MeasurementType.DISCRETE);
        
        final String headerLine = reader.readLine();
        if (headerLine == null) {
            loadedData.addMessage("%s is empty", filename);
            return loadedData;
        }
        final String[] header = headerLine.trim().split(" +");
        String sampleName = "Unknown";
        if (header.length > 0) {
            if (header[0].isEmpty()) {
                loadedData.addMessage("No sample name in \"%s\"", filename);
            } else {
                sampleName = header[0];
            }
        } else {
            /*
             * Should never happen, since split should always return a non-empty
             * array, but best to cover it just in case.
             */
            loadedData.addMessage("No header data in \"%s\"", filename);
        }
        
        final ParsedDoubles headerValues = 
                ParsedDoubles.parse(header,
                        new double[] {0, 0, 90, 0, 0, 10}, 1);
        
        if (!headerValues.success) {
            loadedData.addMessage("Malformed header in \"%s\"", filename);
        }
        
        String line;
        double a95max = 0;
        boolean success = true;
        final double[] defaults = {0, 0, 0, 0, 0, 0, 0};
        while ((line = reader.readLine()) != null) {
            final TreatmentStep step = new TreatmentStep();
            final String[] parts = line.trim().split(" +");
            
            final ParsedDoubles fields =
                    ParsedDoubles.parse(parts, defaults, 0);
            success = success && fields.success;
            /*
             * We carry on even if there was a parsing error (i.e.
             * fields.success == false): ParsedDoubles sets default values
             * so we may get something useful out, and the user will in any
             * case be warned about the malformed data.
             */
            final double treatmentLevel = fields.get(0);
            
            switch (treatmentType) {
                case THERMAL:
                    step.setTemperature(treatmentLevel);
                    break;
                case ARM:
                case DEGAUSS_XYZ:
                    step.setAfX(treatmentLevel / 1000);
                    step.setAfY(treatmentLevel / 1000);
                    step.setAfZ(treatmentLevel / 1000);
                    break;
                case DEGAUSS_Z:
                    step.setAfZ(treatmentLevel / 1000);
                    break;
                case IRM:
                    step.setIrmField(treatmentLevel / 1000);
                    break;
            }
            
            step.setMoment(Vec3.fromPolarDegrees(fields.get(1) / 1000,
                    fields.get(6), fields.get(5)));
            step.setDiscreteId(sampleName);
            step.setSampAz(headerValues.get(1));
            step.setSampDip(headerValues.get(2));
            step.setFormAz(headerValues.get(3));
            step.setFormDip(headerValues.get(4));
            step.setVolume(headerValues.get(5));
            step.setTreatmentType(treatmentType);
            step.setMeasurementType(measurementType);
            final double a95 = fields.get(4);
            if (a95 > a95max) {
                a95max = a95;
            }
            loadedData.addTreatmentStep(step);
        }
        if (!success) {
            loadedData.addMessage("Malformed data fields in \"%s\".", filename);
        }
        if (a95max >= 5) {
            loadedData.addMessage(
                    "File \"%s\" has high α95 values (max. %.1f).",
                    filename, a95max);
        }
        return loadedData;
    }
}
