/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2018 Pontus Lurcock.
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
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.Vec3;
import static net.talvi.puffinplot.data.file.TwoGeeHelper.*;
import static net.talvi.puffinplot.Util.parseDoubleSafely;

/**
 * A file loader for CIT (Caltech) data files. This code is informed in part
 * by the file format description at
 * http://cires1.colorado.edu/people/jones.craig/PMag_Formats.html .
 *
 * @author pont
 */
public class CaltechLoader extends AbstractFileLoader {

    private static final Logger logger =
            Logger.getLogger(CaltechLoader.class.getName());
    private final File file;
    private final File parentDir; // for locating associated files.
    private LineNumberReader reader = null;
    /*
     * NB: this probably won't work for Caltech "NRM" files, which apparently
     * use slightly different field widths. I don't have any of these files to
     * test with at present, so for now am not supporting them.
     */
    private final String patternString =
            "^(..)(....)(......)(......)(......)(......)" +
            "(.........)(......)(......)(......)(.........)(.........)" +
            "(.........)(.........)(...........)(........)";
    private final Pattern pattern = Pattern.compile(patternString);
    
    /**
     * Creates a new CIT loader to read a specified file.
     *
     * @param file the .sam file to read
     */
    public CaltechLoader(File file) {
        this.file = file;
        this.parentDir = file.getParentFile();
        data = new LinkedList<>();
        try {
            reader = new LineNumberReader(new FileReader(file));
            readFile();
        } catch (IOException e) {
            // TODO at least log the exception
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Failed to close reader: ", ex);
            }
        }
    }

    private void readFile() throws IOException {
        // Skip the first two lines
        for (int i = 0; i < 2; i++) {
            reader.readLine();
        }

        final Path parentPath = parentDir.toPath();
        String line;
        while ((line = reader.readLine()) != null) {
            final String leafname = line.trim();
            final Path path = parentPath.resolve(leafname);
            readSubFile(path.toFile());
        }
    }

    private void readSubFile(File file) throws IOException {
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
            final double stratLev =
                    parseDoubleSafely(sampleData.substring(1, 7));
            final double coreStrike =
                    parseDoubleSafely(sampleData.substring(8, 13));
            final double coreDip =
                    parseDoubleSafely(sampleData.substring(14, 19));
            final double bedStrike =
                    parseDoubleSafely(sampleData.substring(20, 25));
            final double bedDip =
                    parseDoubleSafely(sampleData.substring(26, 31));

            /*
             * The example files I have seem to use more than 5 characters for
             * core volume or mass, so I am using the whole rest of the line for
             * this field, since it's the last one anyway.
             */
            final double coreVolOrMass =
                    parseDoubleSafely(sampleData.substring(32));
            
            /*
             * According to the PaleoMag manual, data should start here.
             * However, example files from JMG all have line 3 blank. So we
             * assume that data lines start here, but check for blank lines
             * before attempting to interpret them.
             */
            while ((line = subReader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Datum d = lineToDatum(line);
                if (d != null) {
                    d.setDiscreteId(sampleName);
                    //d.setSampAz((coreStrike + 90) % 360);
                    d.setSampAz((coreStrike + 270) % 360);
                    //d.setSampDip(coreDip);
                    d.setSampHade(coreDip);
                    d.setFormAz((bedStrike + 90) % 360);
                    d.setFormDip(bedDip);
                    data.add(d);
                }
            }
        }
    }

    private Datum lineToDatum(String pmagLine) {

        final Matcher matcher = pattern.matcher(pmagLine);
        matcher.find();
        /*
         *  1 demag type: AF, TT (thermal), CH (chemical)
         *  2 demag level (degC, mT)
         *  3 geographic declination
         *  4 geographic inclination
         *  5 stratigraphic declination
         *  6 stratigraphic inclination
         *  7 normalized intensity
         *  8 measurement error angle
         *  9 core plate declination
         * 10 core plate inclination
         * 11 std. dev. of the measurement in the core's x coord in 10^5 emu
         * 12 std. dev. of the measurement in the core's y coord in 10^5 emu
         * 13 std. dev. of the measurement in the core's z coord in 10^5 emu
         * 14 name
         * 15 date
         * 16 time
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
        try {
            demagLevel = Double.parseDouble(demagLevelString) / 1000.0;
        } catch (NumberFormatException e) {
            // TODO no action needed since it defaults to zero, but
            // should log this
        }
        Datum d = new Datum(gaussToAm(Vec3.fromPolarDegrees(intens, inc, dec)));
        d.setMeasType(MeasType.DISCRETE);
        
        final String treatment = matcher.group(1);
        if (null != treatment) switch (treatment) {
            case "AF":
                d.setTreatType(TreatType.DEGAUSS_XYZ);
                break;
            case "TT":
                d.setTreatType(TreatType.THERMAL);
                break;
            default:
                d.setTreatType(TreatType.UNKNOWN);
                break;
        }
        
        d.setAfX(demagLevel);
        d.setAfY(demagLevel);
        d.setAfZ(demagLevel);
        return d;
    }
}
