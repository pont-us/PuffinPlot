/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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
public class ZplotLoader extends AbstractFileLoader {

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
     * Creates a new Zplot loader to read a specified file.
     *
     * @param file the Zplot file to read
     */
    public ZplotLoader(File file) {
        this.file = file;
        data = new LinkedList<>();
        try {
            reader = new LineNumberReader(new FileReader(file));
            readFile();
        } catch (IOException e) {
            // TODO rethrow here?
        }
    }

    private void readFile() throws IOException {
        // Check first line for magic string
        final String firstLine = reader.readLine();
        if (firstLine == null) {
            addMessage("%s is empty", file.getName());
            return;
        }
        if (!firstLine.startsWith("File Name:")) {
            addMessage("Ignoring unrecognized file %s", file.getName());
            return;
        }
        
        // skip ancestor file, date, user name, project
        for (int i = 0; i < 4; i++) {
            reader.readLine();
        }

        // read the study type from the last header line
        studyType = reader.readLine();

        String headerLine;
        do {
            headerLine = reader.readLine();
        } while (headerLine != null && !headerLine.startsWith("Sample"));
        if (headerLine == null) {
            addMessage("Couldn't find header line in ZPlot file %s: "
                    + "skipping this file",
                    file.getName());
            return;
        }
        final String[] headers = WHITESPACE.split(headerLine);

        if (headers.length < 7 || headers.length > 8) {
            addMessage("Wrong number of header fields in Zplot file %s: " +
                    "expected 7 or 8, got %s", file.getName(), headers.length);
            // return;
        }
        for (int i = 0; i < HEADERS.size(); i++) {
            if (!HEADERS.get(i).matcher(headers[i]).matches()) {
                addMessage("Unknown header field %s in file %s.",
                        headers[i], file.getName());
                // return;
            }
        }
        String line;
        while ((line = reader.readLine()) != null) {
            TreatmentStep step = lineToDatum(line);
            if (step != null) {
                data.add(step);
            }
        }
    }

    private TreatmentStep lineToDatum(String zPlotLine) {
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
                step.setTemp(demag);
                break;
            default:
                throw new Error("Unhandled treatment type " +
                        step.getTreatmentType());
        }
        return step;
    }
}
