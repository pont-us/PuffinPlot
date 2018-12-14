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
package net.talvi.puffinplot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Suite;
import org.apache.commons.compress.archivers.zip.ZipFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class BundleTest {
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    @Test
    public void testCreateBundleNoSites() throws Exception {
        final Suite suite = TestUtils.createDiscreteSuite();
        final Path bundlePath = temporaryFolder.getRoot().toPath().
                resolve("bundle.zip");
        Bundle.createBundle(suite, bundlePath, Correction.NONE,
                suite.getSamples(), suite.getSites(), true);
        final File bundleFile = bundlePath.toFile();
        checkZipContents(bundleFile, "README.md",
                "data.ppl", "data-sample.csv", "data-suite.csv",
                "process-data.sh", "process-data.bat");
        
    }
    
    @Test
    public void testCreateBundleWithSites() throws Exception {
        final Suite suite = TestUtils.createDiscreteSuite();
        final Path bundlePath = temporaryFolder.getRoot().toPath().
                resolve("bundle.zip");
        suite.setSitesForSamples(suite.getSamples(), site -> "only_site_name");
        Bundle.createBundle(suite, bundlePath, Correction.NONE,
                suite.getSamples(), suite.getSites(), true);
        final File bundleFile = bundlePath.toFile();
        checkZipContents(bundleFile, "README.md",
                "data.ppl", "data-sample.csv", "data-suite.csv",
                "data-site.csv", "process-data.sh", "process-data.bat");
    }
    
    private static void checkZipContents(File zip, String... filenames)
            throws IOException {
        assertTrue(zip.isFile());
        final ZipFile zipFile = new ZipFile(zip);
        final Set<String> actualNames =
                Collections.list(zipFile.getEntries()).stream().
                        map(entry -> entry.getName()).
                        collect(Collectors.toSet());
        final Set<String> expectedNames = Arrays.asList(filenames).stream().
                collect(Collectors.toSet());
        assertEquals(expectedNames, actualNames);        
    }
    
}
