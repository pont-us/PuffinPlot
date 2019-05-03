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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.talvi.puffinplot.data.AmsData;
import net.talvi.puffinplot.data.file.testdata.TestFileLocator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class AmsLoaderTest {
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    private static final double DELTA = 1e-10;
    
    @Test
    public void testReadFile() throws Exception {
        final InputStream testInputStream =
                TestFileLocator.class.getResourceAsStream("LPA03091.ASC");
        final Path inputPath = temporaryFolder.getRoot().toPath().
                resolve("INPUT.ASC");
        Files.copy(testInputStream, inputPath);
        final AmsLoader amsLoader = new AmsLoader(inputPath.toFile());
        final List<AmsData> amsDataList = amsLoader.readFile();
        assertEquals(1, amsDataList.size());
        final AmsData amsData = amsDataList.get(0);
        assertEquals("LPA0309.1", amsData.getName());
        assertEquals(28, amsData.getSampleAz(), DELTA);
        assertEquals(0, amsData.getSampleDip(), DELTA);
        
        /*
         * Rotated 90° from the value in the file, since the file uses
         * the P4=90 orientation convention (strike) and PuffinPlot uses
         * P4=0 (down-dip direction).
         */
        assertEquals(90, amsData.getFormAz(), DELTA);
        assertEquals(0, amsData.getFormDip(), DELTA);
        assertEquals(4.7, amsData.getfTest(), DELTA);
        assertArrayEquals(
                new double[] {0.9948, 1.004, 1.0012, 0.0016, 0.0106, -0.0047},
                amsData.getTensor(), DELTA);
    }
    
}
