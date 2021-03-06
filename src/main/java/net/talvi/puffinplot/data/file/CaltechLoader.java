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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.talvi.puffinplot.data.MeasurementType;
import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.Vec3;

import static net.talvi.puffinplot.Util.parseDoubleSafely;
import static net.talvi.puffinplot.data.file.TwoGeeHelper.gaussToAm;

/**
 * A file loader for CIT (Caltech) data files. This code is informed
 * in part by the file format description at
 * http://cires1.colorado.edu/people/jones.craig/PMag_Formats.html .
 *
 * @author pont
 */
public class CaltechLoader implements FileLoader {

    private static final Logger logger
            = Logger.getLogger(CaltechLoader.class.getName());

    /*
     * NB: this probably won't work for Caltech "NRM" files, which apparently
     * use slightly different field widths. I don't have any of these files to
     * test with at present, so for now am not supporting them.
     */
    private final String patternString
            = "^(..)(....)(......)(......)(......)(......)"
            + "(.........)(......)(......)(......)(.........)(.........)"
            + "(.........)(.........)(...........)(........)";
    private final Pattern pattern = Pattern.compile(patternString);

    /**
     * Read a file into a data object containing treatment steps.
     * 
     * @param file the file to read
     * @param options import options; none are currently implemented
     * @return a data object containing the treatment 
     */
    @Override
    public LoadedData readFile(File file, Map<String, Object> options) {
        final File parentDir = file.getParentFile();
        try (LineNumberReader reader =
                new LineNumberReader(new FileReader(file))) {
            return readFile(reader, parentDir);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Exception reading file " +
                    file.getAbsolutePath(), e);
            return new EmptyLoadedData(e.getLocalizedMessage());
        }
    }
    
    private SimpleLoadedData readFile(LineNumberReader reader, File parentDir)
            throws IOException {
        final SimpleLoadedData data = new SimpleLoadedData();

        // Skip the first two lines
        for (int i = 0; i < 2; i++) {
            reader.readLine();
        }

        final Path parentPath = parentDir.toPath();
        String line;
        while ((line = reader.readLine()) != null) {
            final String leafname = line.trim();
            if ("".equals(leafname)) {
                continue;
            }
            final Path path = parentPath.resolve(leafname);
            readSubFile(path.toFile(), data);
        }
        return data;
    }


    private void readSubFile(File file, SimpleLoadedData data)
            throws IOException {
        try (BufferedReader subReader =
                new BufferedReader(new FileReader(file))) {
            String line;
            /*
             * "In the first line the first four characters are the locality id,
             * the next 9 the sample id, and the remainder (to 255) is a sample
             * comment."
             * http://cires1.colorado.edu/people/jones.craig/PMag_Formats.html
             */
            line = subReader.readLine();
            int lastPosition = line.length() > 13 ? 13 : line.length();
            final String sampleName = line.substring(4, lastPosition);

            /*
             * "the first character is ignored, the next 6 comprise the
             * stratigraphic level (usually in meters). The remaining fields are
             * all the same format: first character ignored (should be a blank
             * space) and then 5 characters used. These are the core strike,
             * core dip, bedding strike, bedding dip, and core volume or mass."
             * http://cires1.colorado.edu/people/jones.craig/PMag_Formats.html
             */
            final String sampleData = subReader.readLine();
            final double stratLev
                    = parseDoubleSafely(sampleData.substring(1, 7));
            final double coreStrike
                    = parseDoubleSafely(sampleData.substring(8, 13));
            final double coreDip
                    = parseDoubleSafely(sampleData.substring(14, 19));
            final double bedStrike
                    = parseDoubleSafely(sampleData.substring(20, 25));
            final double bedDip
                    = parseDoubleSafely(sampleData.substring(26, 31));

            /*
             * The example files I have seem to use more than 5 characters for
             * core volume or mass, so I am using the whole rest of the line for
             * this field, since it's the last one anyway.
             */
            final double coreVolOrMass
                    = parseDoubleSafely(sampleData.substring(32));

            /*
             * According to the PaleoMag manual, treatmentSteps should start
             * here. However, example files from JMG all have line 3 blank. So
             * we assume that treatmentSteps lines start here, but check for
             * blank lines before attempting to interpret them.
             */
            while ((line = subReader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                final TreatmentStep step = linetoTreatmentStep(line, file.getName());
                if (step != null) {
                    step.setDiscreteId(sampleName);
                    step.setSampAz((coreStrike + 270) % 360);
                    step.setSampHade(coreDip);
                    step.setFormAz((bedStrike + 90) % 360);
                    step.setFormDip(bedDip);
                    data.addTreatmentStep(step);
                }
            }
        }
    }

    private TreatmentStep linetoTreatmentStep(String pmagLine, String filename) {

        final Matcher matcher = pattern.matcher(pmagLine);
        matcher.find();
        /*
         * 1 demag type: AF, TT (thermal), CH (chemical), NRM 2 demag level
         * (degC, mT) (omitted when NRM) 3 geographic declination 4 geographic
         * inclination 5 stratigraphic declination 6 stratigraphic inclination 7
         * normalized intensity 8 measurement error angle 9 core plate
         * declination 10 core plate inclination 11 std. dev. of the measurement
         * in the core's x coord in 10^5 emu 12 std. dev. of the measurement in
         * the core's y coord in 10^5 emu 13 std. dev. of the measurement in the
         * core's z coord in 10^5 emu 14 name 15 date 16 time
         */

        /*
         * It seems likely that 9 and 10 are declination and inclination in
         * specimen co-ordinates -- confirmed by examination of pmagpy magic
         * import scripts.
         */
        final double intens = Double.parseDouble(matcher.group(7));
        final double inc = Double.parseDouble(matcher.group(10));
        final double dec = Double.parseDouble(matcher.group(9));
        final String demagLevelString = matcher.group(2);
        double demagLevel = 0;
        if (!("M   ".equals(demagLevelString)
                || // "NRM"
                "    ".equals(demagLevelString))) { // Blank -- treat as zero
            try {
                demagLevel = Double.parseDouble(demagLevelString);
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING,
                        "Unparseable demagnetization level \"{0}\" in a data "
                        + "file listed in \"{1}\"",
                        new Object[]{demagLevelString, filename});
            }
        }
        final TreatmentStep step
                = new TreatmentStep(gaussToAm(Vec3.fromPolarDegrees(
                        intens, inc, dec)));
        step.setMeasurementType(MeasurementType.DISCRETE);

        final String treatment = matcher.group(1);
        if (null != treatment) {
            switch (treatment) {
                case "NR":
                    step.setTreatmentType(TreatmentType.NONE);
                    break;
                case "AF":
                    step.setTreatmentType(TreatmentType.DEGAUSS_XYZ);
                    step.setAfX(demagLevel / 1000.0);
                    step.setAfY(demagLevel / 1000.0);
                    step.setAfZ(demagLevel / 1000.0);
                    break;
                case "TT":
                    step.setTreatmentType(TreatmentType.THERMAL);
                    step.setTemperature(demagLevel);
                    break;
                default:
                    step.setTreatmentType(TreatmentType.UNKNOWN);
                    break;
            }
        }

        return step;
    }

}
