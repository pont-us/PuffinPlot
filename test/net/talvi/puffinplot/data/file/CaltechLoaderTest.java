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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.PcaValues;
import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.data.file.testdata.TestFileLocator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        testReadFile("SV12_batch1.sam",
                new String[] {"SV120101a", "SV120102a"},
                new String[] {"0101a", "0102a" },
                new double[][] {{359.5, 67.8, 3.4},
                    {13.0,  60.6, 11.7}},
                new double[] {
                    0, .025, .050, .100, .150, .200, .300, .400, .600, .800},
                TreatmentType.DEGAUSS_XYZ,
                TreatmentType.DEGAUSS_XYZ,
                10);
    }
    
    @Test
    public void testReadThermalFiles() throws IOException {
        testReadFile("PI47-.sam",
                new String[] {"PI47-1a", "PI47-2a"},
                new String[] {"- 1a mag ", "- 2a mag "},
                new double[][] {{84.5, 54.5, 8.1},
                    {56.4, 59.5, 8.8}},
                new double[] {
                    0, 50, 75, 100, 125, 150, 175, 200, 225, 250, 275, 300,
                    325, 350, 375, 400, 425, 450, 475, 500, 525, 550, 560,
                    560, 570, 575, 577, 579},
                TreatmentType.NONE,
                TreatmentType.THERMAL,
                28);
    }

    private void testReadFile(String samFileName,
            String[] dataFileNames, String[] sampleNames,
            double[][] pcaDirs, double[] demagLevels,
            TreatmentType firstTreatmentType,
            TreatmentType subsequentTreatmentType,
            int nDataPerSample) throws IOException {
        final Path tempPath = temporaryFolder.getRoot().toPath();
        
        /*
         * Copy the resource streams into temporary files, since
         * CaltechLoader can't load from a stream.
         */
        final List<String> filenames = new ArrayList<String>();
        filenames.addAll(Arrays.asList(dataFileNames));
        filenames.add(samFileName);
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
        
        final List<TreatmentStep> data = loader.getTreatmentSteps();
        final Correction correction =
                new Correction(false, false, Correction.Rotation.SAMPLE, false);

        /*
         * For a simple test, we boil down the data into a PCA value
         * for each sample, and compared with canned PCA values generated
         * with Craig Jones' PaleoMag application (v 3.1.0b6, Windows 32-bit)
         */
        for (int i=0; i<sampleNames.length; i++) {
            final String sampleName = sampleNames[i];
            final List<TreatmentStep> sampleData = data.stream().
                    filter(d -> sampleName.equals(d.getIdOrDepth())).
                    collect(Collectors.toList());
            final List<Vec3> sampleDirs = sampleData.stream().
                    map(d -> d.getMoment(correction)).
                    collect(Collectors.toList());
            final PcaValues pca = PcaValues.calculate(sampleDirs, false);
            final double[] expected = pcaDirs[i];
            assertEquals(expected[0], pca.getDirection().getDecDeg(), 0.1);
            assertEquals(expected[1], pca.getDirection().getIncDeg(), 0.1);
            assertEquals(expected[2], pca.getMad3(), 0.1);
            assertEquals(firstTreatmentType, sampleData.get(0).getTreatmentType());
            assertTrue(sampleData.stream().skip(1).
                allMatch(d -> d.getTreatmentType() == subsequentTreatmentType));
            assertEquals(nDataPerSample, sampleData.size());
        }
        
        for (int i=0; i<data.size(); i++) {
            assertEquals(demagLevels[i % nDataPerSample],
                    data.get(i).getTreatmentLevel(),
                    delta);
        }
    }
}
