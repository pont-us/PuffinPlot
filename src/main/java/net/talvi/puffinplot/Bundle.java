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
import java.security.CodeSource;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Site;
import net.talvi.puffinplot.data.Suite;

import static net.talvi.puffinplot.PuffinApp.getGreatCirclesValidityCondition;

/**
 * Provides a static method for saving a PuffinPlot suite as a self-contained
 * bundle comprising data, calculation results, scripts for automated
 * processing, and optionally the PuffinPlot jar itself.
 *
 * @author pont
 */
public class Bundle {
    
    private static final Logger LOGGER =
            Logger.getLogger("net.talvi.puffinplot");

    /**
     * Create and save a PuffinPlot data bundle.
     * <p>
     * Exceptions that occur during jar file copying are handled specially,
     * in order to allow an archive to be created even when the jar file can't
     * be copied: in this case the exception is caught internally, the archive
     * is created without the jar file, and the exception is wrapped in an
     * {@code Optional} and returned as the <i>return value</i> of the method.
     * 
     * @param suite the suite from which to create the bundle
     * @param bundlePath the path to which to save the bundle
     * @param correction the correction to apply to the data when performing
     *        calculations
     * @param samples the samples to include in suite mean calculations
     * @param sites the sites to include in suite mean calculations
     * @param copyJarFile if {@code true}, attempt to copy the PuffinPlot jar
     *        file into the bundle
     * 
     * @return an optional exception; if an exception is thrown while copying
     *         the jar file, it is returned, wrapped in an {@code Optional},
     *         and the archive is created without the jar file. If no
     *         exception is thrown during copying, the returned {@code Optional}
     *         contains no value.
     * 
     * @throws IOException if an I/O exception occurred while saving the bundle
     *         (other than while copying the jar file)
     * @throws PuffinUserException if an exception occurred which saving the
     *         suite or any of the results files
     */          
    public static Optional<Exception> createBundle(Suite suite, Path bundlePath,
            Correction correction, List<Sample> samples, List<Site> sites,
            boolean copyJarFile)
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
        /*
         * NB: this file reading technique will convert all line endings to \n.
         * It's not a problem here since they're all \n anyway.
         */
        final String readMeContents =
                new BufferedReader(new InputStreamReader(readMeStream)).
                        lines().collect(Collectors.joining("\n", "", "\n"));        
        writeFile(tempDir, "README.md", false, readMeContents);
        Optional<Exception> jarCopyException = Optional.empty();
        if (copyJarFile) {
            try {
                copyPuffinPlotJarFile(tempDir);
            } catch (IOException | URISyntaxException exception) {
                LOGGER.log(Level.WARNING, "Exception thrown while "
                        + "copying jar file", exception);
                jarCopyException = Optional.of(exception);
            }
        }
        /*
         * The zipDirectory method will preserve permissions on a Posix
         * filesystem (so executable scripts will remain executable), but
         * it can't do anything on a file system which doesn't support
         * an executable bit.
         * 
         * TODO: consider modifying zipDirectory to take a collection of
         * Paths on which to set the executable bit: this can be done in
         * the zip file regardless of whether the source FS supports it.
         */
        Util.zipDirectory(tempDir, bundlePath);
        return jarCopyException;
    }
    
    private static void writeFile(Path parent, String filename,
            boolean executable, String contents) throws IOException {
        final Path path = parent.resolve(Paths.get(filename));
        try (FileWriter fw = new FileWriter(path.toFile())) {
            fw.write(contents);
        }
        if (executable) {
            // On Windows, this will do nothing.
            path.toFile().setExecutable(true, false);
        }
    }
    
    private static void copyPuffinPlotJarFile(Path destinationDir)
            throws IOException, URISyntaxException {
        final File jarFile = findJarFile();
        if (jarFile == null) {
            throw new IOException("jar file not found");
        }
        LOGGER.log(Level.INFO, "PuffinPlot jar file location: {0}",
                jarFile.getAbsolutePath());
        final Path jarPath = jarFile.toPath();
        final Path destPath = destinationDir.resolve("PuffinPlot.jar");
        Files.copy(jarPath, destPath, StandardCopyOption.COPY_ATTRIBUTES);
        destPath.toFile().setExecutable(true, false);
    }
    
    private static File findJarFile() {
        final CodeSource codeSource =
                PuffinApp.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            return null;
        }
        final File file = new File(codeSource.getLocation().getPath());
        /*
         * If PuffinPlot is running from non-jarred class files, file will be
         * the "classes" directory.
         */
        return file.isFile() ? file : null;
    }
}
