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

import java.util.regex.Pattern;
import net.talvi.puffinplot.data.AmsData;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import static java.lang.Double.parseDouble;

/**
 * Turns an AGICO .ASC AMS file into a list of AMS tensors.
 *
 * @author pont
 */

public class AmsLoader {
    private final File ascFile;
    private static final String tensorLabel = "Specimen";
    private static final Pattern lastLine =
            Pattern.compile("^\\d\\d-\\d\\d-\\d\\d\\d\\d$");

    /** Creates a new AMS laoder for a specified file. 
     * @param ascFile the file for which to create the loader */
    public AmsLoader(File ascFile) {
        this.ascFile = ascFile;
    }

    private String[][] readFileChunk(BufferedReader reader) throws IOException {
        ArrayList<String[]> result = new ArrayList<String[]>(64);
        do {
            String line = reader.readLine();
            if (line==null) break;
            line = line.replace("\f", "");
            result.add(line.split("\\s+"));
            if (lastLine.matcher(line).matches()) break;
        } while (true);
        return result.toArray(new String[][] {});
    }

    /**
     * Reads the file for which this loader was created.
     * 
     * @return the AMS tensor data in the file
     * @throws IOException if there was an I/O error while reading the file
     */
    public List<AmsData> readFile() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(ascFile));
        List<AmsData> result = new ArrayList<AmsData>();
        do {
            String[][] chunk = readFileChunk(reader);
            if (chunk.length<39) break;
            double[] tensor = {parseDouble(chunk[37][5]),
                parseDouble(chunk[37][6]), parseDouble(chunk[37][7]),
                parseDouble(chunk[38][5]),
                parseDouble(chunk[38][6]), parseDouble(chunk[38][7])};
            AmsData amsData = new AmsData(chunk[0][0], tensor,
                    parseDouble(chunk[3][1]), parseDouble(chunk[5][1]),
                    parseDouble(chunk[16][5]));
            result.add(amsData);
        } while (true);
        return result;
    }

    private List<AmsData> oldReadFile() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(ascFile));
        List<AmsData> result = new ArrayList<AmsData>();
        String line = null;
        boolean tensorFound = false;
        String name = null;
        double k11=0, k22=0, k33=0;
        double sampleAz=0, sampleDip=0;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("\\s+");
            if (parts.length > 1) {
                if ("ANISOTROPY".equals(parts[1])) {
                    name = parts[0];
                }
                if ("ANISOTROPY".equals(parts[2])) {
                    name = parts[1];
                }
                if ("Azi".equals(parts[1])) {
                    sampleAz = parseDouble(parts[0]);
                }
                if ("Dip".equals(parts[1])) {
                    sampleDip = parseDouble(parts[0]);
                }
            }
            if (tensorFound) {
                tensorFound = false;
                double k12 = parseDouble(parts[5]);
                double k23 = parseDouble(parts[6]);
                double k13 = parseDouble(parts[7]);
                final double[] tensor =
                        new double[] {k11, k22, k33, k12, k23, k13};
                result.add(new AmsData(name, tensor, sampleAz, sampleDip, 10.));
            }
            if (parts.length > 0 && tensorLabel.equals(parts[0])) {
                tensorFound = true;
                k11 = parseDouble(parts[5]);
                k22 = parseDouble(parts[6]);
                k33 = parseDouble(parts[7]);
            }
        }
        return result;
    }

}
