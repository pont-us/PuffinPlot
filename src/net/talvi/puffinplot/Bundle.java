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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import static net.talvi.puffinplot.PuffinApp.getGreatCirclesValidityCondition;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Site;
import net.talvi.puffinplot.data.Suite;

public class Bundle {
    
    private static final Logger LOGGER =
            Logger.getLogger("net.talvi.puffinplot");

    public static void createBundle(Suite suite, Path bundlePath,
            Correction correction, List<Sample> samples, List<Site> sites)
            throws IOException, PuffinUserException {
        LOGGER.info("Starting bundle creation.");
        
        final Path tempDir = Files.createTempDirectory("puffinplot");
        LOGGER.log(Level.INFO, "Temporary directory: {0}",
                tempDir.toString());
        
        suite.saveAs(tempDir.resolve(Paths.get("data.ppl")).toFile());
        suite.doAllCalculations(correction,
                getGreatCirclesValidityCondition());
        suite.calculateSuiteMeans(samples, sites);
        
        suite.saveCalcsSample(
                tempDir.resolve(Paths.get("data-sample.csv")).toFile());
        if (!suite.getSites().isEmpty()) {
            suite.saveCalcsSite(
                    tempDir.resolve(Paths.get("data-site.csv")).toFile());
        }
        suite.saveCalcsSuite(
                tempDir.resolve(Paths.get("data-suite.csv")).toFile());
        
        writeFile(tempDir, "process-data.sh", true,
                "#!/bin/sh\n\n" +
                        "java -jar PuffinPlot.jar -process data.ppl\n");
        writeFile(tempDir, "process-data.bat", true,
                "java -jar PuffinPlot.jar -process data.ppl\n");

        final InputStream readMeStream =
                Bundle.class.getResourceAsStream("bundle-readme.md");
        /* NB: this file reading technique will convert all line endings
         * to \n. It's not a problem here since they're all \n anyway.
         */
        final String readMeContents =
                new BufferedReader(new InputStreamReader(readMeStream)).
                        lines().collect(Collectors.joining("\n", "", "\n"));        
        writeFile(tempDir, "README.md", false, readMeContents);
        copyPuffinPlotJarFile(tempDir);
        Util.zipDirectory(tempDir, bundlePath);
    }
    
    private static void writeFile(Path parent, String filename,
            boolean executable, String contents) throws IOException {
        final Path path = parent.resolve(Paths.get(filename));
        try (FileWriter fw = new FileWriter(path.toFile())) {
            fw.write(contents);
        }
        if (executable) {
            path.toFile().setExecutable(true, false);
        }
    }
    
    private static void copyPuffinPlotJarFile(Path destinationDir)
            throws IOException {
        File jarFile = null;
        try {
            jarFile = new File(PuffinApp.class.getProtectionDomain().
                    getCodeSource().getLocation().toURI().getPath());
        } catch (URISyntaxException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        if (jarFile == null) {
            return;
        }
        LOGGER.log(Level.INFO, "PuffinPlot jar file location: {0}",
                jarFile.getAbsolutePath());
        final Path jarPath = jarFile.toPath();
        final Path destPath = destinationDir.resolve("PuffinPlot.jar");
        Files.copy(jarPath, destPath, StandardCopyOption.COPY_ATTRIBUTES);
        destPath.toFile().setExecutable(true, false);
    }
}
