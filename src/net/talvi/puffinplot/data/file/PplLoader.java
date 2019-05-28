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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.TreatmentStep;

import static java.lang.Double.isNaN;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static net.talvi.puffinplot.data.file.TwoGeeHelper.gaussToAm;
import static net.talvi.puffinplot.data.file.TwoGeeHelper.oerstedToTesla;
import static net.talvi.puffinplot.data.file.TwoGeeHelper.treatTypeFromString;

/**
 * A loader for PuffinPlot's own file format.
 * 
 * @author pont
 */

public class PplLoader implements FileLoader {

    private static final Pattern PUFFIN_HEADER =
            Pattern.compile("^PuffinPlot file. Version (\\d+)");

    /**
     * Reads data from a specified PuffinPlot file.
     * 
     * @param file the file from which to read data
     * @param options load options (currently unused)
     * @return the data from the file
     */
    @Override
    public LoadedData readFile(File file, Map<Object, Object> options) {
        try (InputStream stream = new FileInputStream(file);
                InputStreamReader isReader =
                        new InputStreamReader(stream, StandardCharsets.UTF_8);
                LineNumberReader reader = new LineNumberReader(isReader)) {
            final String firstLine = reader.readLine();
            if (firstLine == null) {
                throw new IOException(file + " is empty.");
            }
            final Matcher matcher = PUFFIN_HEADER.matcher(firstLine);
            if (!matcher.matches()) {
                throw new IOException(file + " doesn't appear to be a "
                        + "PuffinPlot file.");
            }
            final String versionString = matcher.group(1);
            int version = Integer.parseInt(versionString);
            if (version != 2 && version != 3) {
                throw new IOException(String.format(Locale.ENGLISH,
                        "%s is of version %d, which cannot be "
                        + "loaded by this version of PuffinPlot.",
                        file, version));
            }

            return readData(reader, file.getName(), version);
        } catch (IOException | MalformedFileException exception) {
            return new EmptyLoadedData(exception.getMessage());
        }
    }

    private LoadedData readData(LineNumberReader reader, String filename,
            int version)
            throws IOException, MalformedFileException {
        final String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IOException(filename + " contains no headers or data.");
        }
        final List<String> headers = Arrays.asList(headerLine.split("\t"));
        final int treatmentField = headers.indexOf("TREATMENT");
        final TreatmentStep.Reader stepReader =
                new TreatmentStep.Reader(headers);
        final SimpleLoadedData loadedData = new SimpleLoadedData();
        String line;
        while ((line = reader.readLine()) != null) {
            if ("".equals(line)) {
                break;
            }
            final List<String> values = Arrays.asList(line.split("\t"));
            if (version == 2) {
                /*
                 * Ppl 2 files still use the 2G strings for treatment types, so
                 * we munge it into a suitable input for TreatmentType.valueOf
                 * before passing it to the TreatmentStep reader.
                 */
                values.set(treatmentField, treatTypeFromString(values.get
                        (treatmentField)).toString());
                /*
                 * Fortunately, measurement type strings happen to carry across
                 * so we don't need to munge them.
                 */
            }
            TreatmentStep step = null;
            try {
                step = stepReader.fromStrings(values);
            } catch (NumberFormatException e) {
                final String msg = String.format(Locale.ENGLISH,
                        "Error at line %d "+
                        "of file %s:\n%s", reader.getLineNumber(),
                        filename, e.getMessage());
                throw new MalformedFileException(msg);
            }
            if (version == 2) {
                /*
                 * Ppl 2 files store magnetic data (except susceptibility) in
                 * cgs units, which must be corrected on loading.
                 */
                step.setMoment(gaussToAm(step.getMoment(Correction.NONE)));
                if (!isNaN(step.getAfX())) {
                    step.setAfX(oerstedToTesla(step.getAfX()));
                }
                if (!isNaN(step.getAfY())) {
                    step.setAfX(oerstedToTesla(step.getAfY()));
                }
                if (!isNaN(step.getAfZ())) {
                    step.setAfX(oerstedToTesla(step.getAfZ()));
                }
                if (!isNaN(step.getIrmField())) {
                    step.setIrmField(oerstedToTesla(step.getIrmField()));
                }
                if (!isNaN(step.getArmField())) {
                    step.setArmField(oerstedToTesla(step.getArmField()));
                }
            }
            loadedData.addTreatmentStep(step);
        }
        if (line != null) {
            while ((line = reader.readLine()) != null) {
                loadedData.addExtraLine(line);
            }
        }
        return loadedData;
    }

}
