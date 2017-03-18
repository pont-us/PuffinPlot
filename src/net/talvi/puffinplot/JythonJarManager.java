/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
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
 *
 * @author pont
 */
public class JythonJarManager {
    
    private static final long CORRECT_SIZE = 37021723;
    private static final String CORRECT_SHA1 =
            "CDFB38BC6F8343BCF1D6ACCC2E1147E8E7B63B75";
    public static final String SOURCE_URL_STRING =
            //"http://central.maven.org/maven2/"
            //    + "org/python/jython-standalone/2.7.0/"
            //    + "jython-standalone-2.7.0.jar";
            "http://localhost:8001/jython-standalone-2.7.0.jar";
    
    public static Path getPath() throws IOException {
        return Util.getAppDataDirectory().
                resolve("jython-standalone-2.7.0.jar");
    }
    
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
    
    public static boolean checkInstalled(boolean deleteIfWrongSize)
            throws IOException {
        final File jythonFile = getPath().toFile();
        
        if (!jythonFile.exists()) {
            // no jar installed
            return false;
        } else {
            if (jythonFile.length() != CORRECT_SIZE) {
                // jar installed, but wrong size
                if (deleteIfWrongSize) {
                    jythonFile.delete();
                }
                return false;
            } else {
                // jar installed and correct size
                // (NB: we don't checksum it.)
                return true;
            }
        }
    }
    
    public static void download() throws IOException {
        // NB: Files.copy can block indefinitely.
        final URL url = new URL(SOURCE_URL_STRING);
        try (InputStream in = url.openStream()) {
            Files.copy(in, getPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static void delete() throws IOException {
        getPath().toFile().delete();
    }
}
