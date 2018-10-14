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
    private static final Pattern lastLine =
            Pattern.compile("^\\d\\d-\\d\\d-\\d\\d\\d\\d$");

    /** Creates a new AMS loader for a specified file. 
     * @param ascFile the file for which to create the loader */
    public AmsLoader(File ascFile) {
        this.ascFile = ascFile;
    }

    /**
     * Reads a chunk of a file and splits it into fields.
     * Fields are delimited by whitespace. The chunk itself is
     * delimited by the lastLine pattern. If a line begins with
     * whitespace, field 0 for that line will be an empty string,
     * with the first non-whitespace content assigned to field 1.
     * 
     * @param reader the reader from which to take the data
     * @return an array of arrays of strings. Each sub-array
     * contains the string values of the fields in the corresponding
     * line.
     * @throws IOException 
     */
    private String[][] readFileChunk(BufferedReader reader) throws IOException {
        ArrayList<String[]> result = new ArrayList<>(64);
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
        final BufferedReader reader =
                new BufferedReader(new FileReader(ascFile));
        final List<AmsData> result = new ArrayList<>();
        do {
            final String[][] chunk = readFileChunk(reader);
            if (chunk.length<39) break;
            
            int tensorHeader = -1; // line number of tensor header
            for (int i=0; i<chunk.length; i++) {
                // Find the tensor header in the file.
                if (chunk[i].length >= 5 &&
                        "Normed".equals(chunk[i][3]) &&
                        "tensor".equals(chunk[i][4])) {
                    tensorHeader = i;
                    break;
                }
            }
            
            int fTestHeader = -1; // line number of f-test header
            for (int i=0; i<chunk.length; i++) {
                // Find the F-test header in the file.
                if (chunk[i].length == 8 &&
                        "F".equals(chunk[i][5]) &&
                        "F12".equals(chunk[i][6]) &&
                        "F23".equals(chunk[i][7])) {
                    fTestHeader = i;
                    break;
                }
            }
            if (tensorHeader == -1 || fTestHeader == -1) {
                // Without the headers, we can't find the data.
                throw new IOException("ASC header lines not found "+
                        "in file " + ascFile.getName());
            }
            
            if (chunk[tensorHeader + 2].length != 8 ||
                chunk[tensorHeader + 3].length != 8 ||
                chunk[0].length < 1 ||
                chunk[3].length < 2 ||
                chunk[5].length < 2 ||
                chunk[fTestHeader+2].length < 7) {
                // The fields we need aren't in the expected positions.
                throw new IOException("Data fields not found "+
                        "in file " + ascFile.getName());
            }
            
            try {
                final double[] tensor = {
                    parseDouble(chunk[(tensorHeader + 2)][5]),
                    parseDouble(chunk[(tensorHeader + 2)][6]),
                    parseDouble(chunk[(tensorHeader + 2)][7]),
                    parseDouble(chunk[(tensorHeader + 3)][5]),
                    parseDouble(chunk[(tensorHeader + 3)][6]),
                    parseDouble(chunk[(tensorHeader + 3)][7])};
                final String[] fTestLine = chunk[fTestHeader + 2];
                final AmsData amsData = new AmsData
                        (chunk[0][0], // sample name
                        tensor,
                        parseDouble(chunk[3][1]), // azimuth
                        parseDouble(chunk[5][1]), // dip
                        parseDouble(fTestLine[fTestLine.length-3])
                        /* We count backwards from the end of the line for
                           the F-test result, because SAFYR has 7 fields
                           in this line (plus the initial blank) but 
                           SUSAR has only 6. */
                        );
                result.add(amsData);
            } catch (NumberFormatException e) {
                throw new IOException("Malformed data "+
                        "in file " + ascFile.getName());
            }
        } while (true);
        return result;
    }
}
