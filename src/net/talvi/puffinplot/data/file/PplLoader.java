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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.TreatmentStep;

import static java.lang.Double.isNaN;
import static net.talvi.puffinplot.data.file.TwoGeeHelper.gaussToAm;
import static net.talvi.puffinplot.data.file.TwoGeeHelper.oerstedToTesla;
import static net.talvi.puffinplot.data.file.TwoGeeHelper.treatTypeFromString;

/**
 * A loader for PuffinPlot's own file format.
 * 
 * @author pont
 */

public class PplLoader extends AbstractFileLoader {

    private static final Logger logger =
            Logger.getLogger(PplLoader.class.getName());
    private static final Pattern puffinHeader =
            Pattern.compile("^PuffinPlot file. Version (\\d+)");
    private LineNumberReader reader;
    private TreatmentStep.Reader datumReader;
    private int treatmentField;
    private int version;
    private List<String> extraLines = Collections.emptyList();
    private final File file;

    /**
     * Creates a PuffinPlot file loader for the specified file.
     * 
     * @param file the file from which to read data
     */
    public PplLoader(File file) {
        this.file = file;
        try {
            reader = new LineNumberReader(new FileReader(file));
            final String firstLine = reader.readLine();
            if (firstLine == null) {
                throw new IOException(file + " is empty.");
            }
            Matcher matcher = puffinHeader.matcher(firstLine);
            if (!matcher.matches()) {
                throw new IOException(file + " doesn't appear to be a "
                        + "PuffinPlot file.");
            }
            String versionString = matcher.group(1);
            version = Integer.parseInt(versionString);
            if (version != 2 && version != 3) {
                throw new IOException(String.format(Locale.ENGLISH,
                        "%s is of version %d,"
                        + "which cannot be "
                        + "loaded by this version of PuffinPlot.",
                        file, version));
            }
            final String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException(file + " contains no headers or data.");
            }
            List<String> headers = Arrays.asList(headerLine.split("\t"));
            treatmentField = headers.indexOf("TREATMENT");
            datumReader = new TreatmentStep.Reader(headers);
            readFile();
        } catch (IOException | MalformedFileException e) {
            addMessage(e.getMessage());
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Failed to close reader: ", ex);
            }
        }
    }

    private void readFile() throws IOException, MalformedFileException {
        String line;
        while ((line = reader.readLine()) != null) {
            if ("".equals(line)) break;
            List<String> values = Arrays.asList(line.split("\t"));
            if (version==2) {
                // Ppl 2 files still use the 2G strings for treatment types,
                // so we munge it into a suitable input for TreatmentType.valueOf
                // before passing it to the TreatmentStep reader.
                values.set(treatmentField, treatTypeFromString(values.get
                        (treatmentField)).toString());
                // Fortunately, measurement type strings happen to carry
                // across so we don't need to munge them.
            }
            TreatmentStep step = null;
            try {
                step = datumReader.fromStrings(values);
            } catch (NumberFormatException e) {
                final String msg = String.format(Locale.ENGLISH,
                        "Error at line %d "+
                        "of file %s:\n%s", reader.getLineNumber(),
                        file.getName(), e.getMessage());
                throw new MalformedFileException(msg);
            }
            if (version==2) {
                // Ppl 2 files store magnetic data (except susceptibility)
                // in cgs units, which must be corrected on loading.
                step.setMoment(gaussToAm(step.getMoment(Correction.NONE)));
                if (!isNaN(step.getAfX()))
                    step.setAfX(oerstedToTesla(step.getAfX()));
                if (!isNaN(step.getAfY()))
                    step.setAfX(oerstedToTesla(step.getAfY()));
                if (!isNaN(step.getAfZ()))
                    step.setAfX(oerstedToTesla(step.getAfZ()));
                if (!isNaN(step.getIrmField()))
                    step.setIrmField(oerstedToTesla(step.getIrmField()));
                if (!isNaN(step.getArmField()))
                    step.setArmField(oerstedToTesla(step.getArmField()));
            }
            addTreatmentStep(step);
        }
        if (line != null) {
            extraLines = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                extraLines.add(line);
            }
        }
    }

    @Override
    public List<String> getExtraLines() {
        return Collections.unmodifiableList(extraLines);
    }
}
