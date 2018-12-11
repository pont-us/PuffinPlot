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
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;

/**
 * A collection of static methods for managing the Jython jar.
 *
 * PuffinPlot can make use of Jython, but for reasons of size Jython is not
 * included in the build. To use Jython in PuffinPlot, the Jython jar must be
 * downloaded and installed locally. This class provides some static utility
 * methods to check for a local Jython jar, verify its integrity, download it
 * from a known URL, or delete it from the local cache.
 *
 * @author pont
 */
public class JythonJarManager {

    private static final long CORRECT_SIZE = 41135585;
    private static final String CORRECT_SHA1 =
            "942C3294840DC9DFB3528D775F4D02A6D57C421F";
    public static final String SOURCE_URL_STRING =
            "http://central.maven.org/maven2/org/python/jython-standalone/" +
            "2.7.1/jython-standalone-2.7.1.jar";

    /**
     * Returns the local path for the Jython jar file with the application data
     * directory. The method does <b>not</b> guarantee that the jar is currently
     * present at this location.
     *
     * @return the local path to the jython jar file
     * @throws IOException if there was an error determining the path
     */
    public static Path getPath() throws IOException {
        return Util.getAppDataDirectory().
                resolve("jython-standalone-2.7.1.jar");
    }

    /**
     * Calculates the SHA-1 digest of the Jython jar file and verifies it
     * against a hard-coded reference value.
     *
     * @param deleteIfIncorrect If this parameter is true, and if the SHA-1
     * digest is not correct, the jar file will be deleted
     * @return true iff the Jython jar file has the correct SHA-1 digest
     * @throws IOException if there was an error reading the jar file
     * @throws NoSuchAlgorithmException if the SHA-1 algorithm was not available
     */
    public static boolean checkSha1Digest(boolean deleteIfIncorrect)
            throws IOException, NoSuchAlgorithmException {
        final String actualSha1 = Util.calculateSHA1(getPath().toFile());
        if (CORRECT_SHA1.equals(actualSha1)) {
            return true;
        } else {
            if (deleteIfIncorrect) {
                getPath().toFile().delete();
            }
            return false;
        }
    }

    /**
     * Checks whether the Jython jar file is installed locally and has the
     * correct size. The size is checked against a known, hard-coded value. This
     * method does <b>not</b> check the SHA-1 digest.
     *
     * @param deleteIfWrongSize if true, and if the jar file
     * does not have the correct size, it will be deleted.
     * @return true iff the Jython jar file is locally installed
     * and has the correct size
     * @throws IOException if there was an error checking or deleting
     * the jar file
     */
    public static boolean checkInstalled(boolean deleteIfWrongSize)
            throws IOException {
        final File jythonFile = getPath().toFile();

        if (!jythonFile.exists()) {
            // no jar installed
            return false;
        } else if (jythonFile.length() != CORRECT_SIZE) {
            // jar installed, but wrong size
            if (deleteIfWrongSize) {
                jythonFile.delete();
            }
            return false;
        } else {
            // jar installed and correct size (NB: we don't checksum it.)
            return true;
        }
    }

    /**
     * Downloads the Jython jar from a hard-coded URL and saves it in
     * the application data directory.
     * 
     * This method may block indefinitely.
     * 
     * @throws IOException if there is an error reading from the URL
     * or writing to the file
     */
    public static void download() throws IOException {
        // NB: Files.copy can block indefinitely.
        final URL url = new URL(SOURCE_URL_STRING);
        try (InputStream in = url.openStream()) {
            Files.copy(in, getPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Deletes the locally cached copy of the Jython jar, if it exists.
     * 
     * @throws IOException if there was an error deleting the file
     */
    static void delete() throws IOException {
        getPath().toFile().delete();
    }
    
    /**
     * @return The expected size, in bytes, of the Jython jar.
     */
    public static long getExpectedDownloadSize() {
        return CORRECT_SIZE;
    }
}
