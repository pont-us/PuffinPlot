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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import net.talvi.puffinplot.data.MeasurementType;
import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.Vec3;

import static net.talvi.puffinplot.data.file.TwoGeeHelper.gaussToAm;
import static net.talvi.puffinplot.data.file.TwoGeeHelper.oerstedToTesla;

/**
 * A file loader for the file format used by Steve Hurst's Zplot program.
 * 
 * @author pont
 */
public class ZplotLoader2 implements FileLoader2 {

    private LineNumberReader reader;
    private File file;
    private String studyType;
    private MeasurementType measurementType = MeasurementType.UNSET;

    private static final List<Pattern> HEADERS;
    private static final Pattern NUMBER_PATTERN =
            Pattern.compile("\\d+(\\.\\d+)?");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern DELIMITER_PATTERN = Pattern.compile("\\t");
    
    static {
        final String[] fields = {"^Sample", "^Project", "^Demag.*",
            "^Declin.*", "^Inclin.*", "^Intens.*", "^Operation|^Depth"};
        final List<Pattern> fieldPatterns = new ArrayList<>(fields.length);
        for (String field: fields) {
            fieldPatterns.add(Pattern.compile(field));
        }
        HEADERS = fieldPatterns;
    }
    
    /**
     * Reads a Zplot file.
     *
     * @param file the Zplot file to read
     * @param options load options (not used)
     * @return the data in the file
     */
    @Override
    public LoadedData readFile(File file, Map<Object, Object> options) {
        this.file = file;
        try (LineNumberReader reader =
                new LineNumberReader(new FileReader(file))) {
            return readFile(reader);
        } catch (IOException expection) {
            return new EmptyLoadedData("Error reading file: "
                    + expection.getLocalizedMessage());
        }
    }

    private LoadedData readFile(LineNumberReader reader) throws IOException {
        final SimpleLoadedData loadedData = new SimpleLoadedData();
        // Check first line for magic string.
        final String firstLine = reader.readLine();
        if (firstLine == null) {
            loadedData.addMessage("%s is empty", file.getName());
            return loadedData;
        }
        if (!firstLine.startsWith("File Name:")) {
            loadedData.addMessage("Ignoring unrecognized file %s",
                    file.getName());
            return loadedData;
        }
        
        // Skip ancestor file, date, user name, and project.
        for (int i = 0; i < 4; i++) {
            reader.readLine();
        }

        // Read the study type from the last header line.
        studyType = reader.readLine();

        String headerLine;
        do {
            headerLine = reader.readLine();
        } while (headerLine != null && !headerLine.startsWith("Sample"));
        if (headerLine == null) {
            loadedData.addMessage("Couldn't find header line in ZPlot file %s: "
                    + "skipping this file",
                    file.getName());
            return loadedData;
        }
        final String[] headers = WHITESPACE.split(headerLine);

        if (headers.length < 7 || headers.length > 8) {
            loadedData.addMessage(
                    "Wrong number of header fields in Zplot file %s: " +
                    "expected 7 or 8, got %s", file.getName(), headers.length);
        }
        for (int i = 0; i < HEADERS.size(); i++) {
            if (!HEADERS.get(i).matcher(headers[i]).matches()) {
                loadedData.addMessage("Unknown header field %s in file %s.",
                        headers[i], file.getName());
            }
        }
        String line;
        while ((line = reader.readLine()) != null) {
            final TreatmentStep step = lineToTreatmentStep(line);
            if (step != null) {
                loadedData.addTreatmentStep(step);
            }
        }
        return loadedData;
    }

    private TreatmentStep lineToTreatmentStep(String zPlotLine) {
        final Scanner scanner = new Scanner(zPlotLine);
        scanner.useLocale(Locale.ENGLISH); // ensure "." as decimal separator
        scanner.useDelimiter(DELIMITER_PATTERN);
        final String depthOrSample = scanner.next();
        final String project = scanner.next();
        final double demag = scanner.nextDouble();
        final double dec = scanner.nextDouble();
        final double inc = scanner.nextDouble();
        final double intensity = scanner.nextDouble();
        final String operation = scanner.next();

        final TreatmentStep step = new TreatmentStep(
                gaussToAm(Vec3.fromPolarDegrees(intensity, inc, dec)));
        if (measurementType == MeasurementType.UNSET) {
            measurementType =
                    (NUMBER_PATTERN.matcher(depthOrSample).matches())
                    ? MeasurementType.CONTINUOUS
                    : MeasurementType.DISCRETE;
        }
        step.setMeasurementType(measurementType);
        switch (measurementType) {
            case CONTINUOUS:
                step.setDepth(depthOrSample);
                break;
            case DISCRETE:
                step.setDiscreteId(depthOrSample);
                break;
            default:
                throw new Error("Unhandled measurement type "+ measurementType);
        }

        step.setTreatmentType(
                (project.toLowerCase().contains("therm") ||
                        operation.toLowerCase().contains("therm") ||
                        studyType.toLowerCase().contains("therm")) ?
                        TreatmentType.THERMAL :
                        TreatmentType.DEGAUSS_XYZ);
        switch (step.getTreatmentType()) {
            case DEGAUSS_XYZ:
                step.setAfX(oerstedToTesla(demag));
                step.setAfY(oerstedToTesla(demag));
                step.setAfZ(oerstedToTesla(demag));
                break;
            case THERMAL:
                step.setTemperature(demag);
                break;
            default:
                throw new Error("Unhandled treatment type " +
                        step.getTreatmentType());
        }
        return step;
    }
}
