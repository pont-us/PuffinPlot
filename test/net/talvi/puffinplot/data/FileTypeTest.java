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
package net.talvi.puffinplot.data;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.talvi.puffinplot.TestUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author pont
 */
public class FileTypeTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    @Test
    public void testValueOf() {
        for (FileType ft: FileType.values()) {
            assertEquals(ft, FileType.valueOf(ft.toString()));
        }
    }

    @Test
    public void testGuessFromSuffix() throws IOException {
        assertEquals(FileType.TWOGEE,
                FileType.guess(temporaryFolder.newFile("test.dat")));
        assertEquals(FileType.IAPD,
                FileType.guess(temporaryFolder.newFile("test.iapd")));
        assertEquals(FileType.ZPLOT,
                FileType.guess(temporaryFolder.newFile("test.txt")));
        assertEquals(FileType.CALTECH,
                FileType.guess(temporaryFolder.newFile("test.sam")));
        assertEquals(FileType.UNKNOWN,
                FileType.guess(temporaryFolder.newFile("test.aksjdy")));
    }
    
    @Test
    public void testGuessPplTypes() throws IOException {
        assertEquals(FileType.PUFFINPLOT_OLD,
                FileType.guess(temporaryFolder.newFile("empty.ppl")));
        assertEquals(FileType.PUFFINPLOT_OLD,
                FileType.guess(TestUtils.writeStringToTemporaryFile("old.ppl",
                "Any\nold\nnonsense\n", temporaryFolder)));
        assertEquals(FileType.PUFFINPLOT_NEW,
                FileType.guess(TestUtils.writeStringToTemporaryFile("new.ppl",
                "PuffinPlot file.\nData would start here...\n",
                temporaryFolder)));
    }

    @Test(expected = IOException.class)
    public void testGuessOnNonexistentFile() throws IOException {
        FileType.guess(temporaryFolder.getRoot().toPath().
                resolve("nonexistent.ppl").toFile());
    }
    
    @Test
    public void testGetNiceName() {
        /*
         * It makes no sense to test the exact form of the nice name
         * (needless duplication), but we can check that they're not null
         * at least.
         */
        
        for (FileType ft: FileType.values()) {
            assertNotNull(ft.getNiceName());
        }
        
        // Check that nice names are unique.
        assertEquals((long) FileType.values().length,
                (long) Stream.of(FileType.values()).map(ft -> ft.getNiceName()).
                        distinct().collect(Collectors.counting()));
    }

    @Test
    public void testGetShortcut() {
        // Check that shortcuts are unique.
        assertEquals((long) FileType.values().length,
                (long) Stream.of(FileType.values()).map(ft -> ft.getShortcut()).
                        distinct().collect(Collectors.counting()));

    }
    
}
