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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot. If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.PcaValues;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.data.file.testdata.TestFileLocator;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author pont
 */
public class CaltechLoaderTest {
    
    private static final double delta = 1e-10;
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    @Test
    public void testReadAfFiles() throws IOException {
        final Path tempPath = temporaryFolder.getRoot().toPath();
        final String samFileName = "SV12_batch1.sam";
        
        /*
         * Copy the resource streams into temporary files, since
         * CaltechLoader can't load from a stream.
         */
        final String[] filenames = {
            "SV120101a", "SV120102a", samFileName};
        for (String filename: filenames) {
            final InputStream inputStream =
                    TestFileLocator.class.getResourceAsStream("caltech/" +
                            filename);
            final Path path = tempPath.resolve(filename);
            Files.copy(inputStream, path);
        }

        // Instantiating the loader reads the files.
        final CaltechLoader loader =
                new CaltechLoader(tempPath.resolve(samFileName).toFile());
        
        final List<Datum> data = loader.getData();
        assertEquals(20, data.size());
        final Correction correction =
                new Correction(false, false, Correction.Rotation.SAMPLE, false);

        /*
         * For a simple test, we boil down the data into a PCA value
         * for each sample, and compared with canned PCA values generated
         * from Craig Jones' PaleoMag application (v 3.1.0b6, Windows 32-bit)
         */
        final String[] sampleNames = {
            "0101a", "0102a" };
        final double[][] pcaDirs = {
            {359.5, 67.8, 3.4},
            {13.0,  60.6, 11.7}
        };
        
        for (int i=0; i<sampleNames.length; i++) {
            final String sampleName = sampleNames[i];
            final List<Vec3> sample1dirs = data.stream().
                    filter(d -> sampleName.equals(d.getIdOrDepth())).
                    map(d -> d.getMoment(correction)).
                    collect(Collectors.toList());
            final PcaValues pca = PcaValues.calculate(sample1dirs, false);
            final double[] expected = pcaDirs[i];
            assertEquals(expected[0], pca.getDirection().getDecDeg(), 0.01);
            assertEquals(expected[1], pca.getDirection().getIncDeg(), 0.01);
            assertEquals(expected[2], pca.getMad3(), 0.1);
        }
        
        assertTrue(data.stream().
                allMatch(d -> d.getTreatType() == TreatType.DEGAUSS_XYZ));
        final double[] demagLevels = {
            0, 25, 50, 100, 150, 200, 300, 400, 600, 800};
        for (int i=0; i<data.size(); i++) {
            assertEquals(demagLevels[i % 10],
                    data.get(i).getTreatmentLevel() * 1000,
                    delta);
        }
        
    }
    
}
 