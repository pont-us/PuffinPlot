package net.talvi.puffinplot.data.file;

import java.util.regex.Pattern;
import java.util.Arrays;
import net.talvi.puffinplot.data.AmsData;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import net.talvi.puffinplot.data.Tensor;
import static java.lang.Double.parseDouble;

/**
 * Turn an AGICO .ASC AMS file into a list of AMS tensors.
 *
 * @author pont
 */

public class AmsLoader {
    private final File ascFile;
    private static final String tensorLabel = "Specimen";
    private static final Pattern lastLine =
            Pattern.compile("^\\d\\d-\\d\\d-\\d\\d\\d\\d$");

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

    public List<AmsData> readFile2() throws IOException {
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

    public List<AmsData> readFile() throws IOException {
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
