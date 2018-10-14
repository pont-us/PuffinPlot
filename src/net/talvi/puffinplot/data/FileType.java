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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * This fields of this enum represent the types of file that PuffinPlot
 * can read.
 * 
 * @author pont
 */
public enum FileType {
    
    /** DAT, 2G Long Core software */
    TWOGEE("2G cryomagnetometer", '2'),
    /** TXT, Steve Hurst's Zplot program */
    ZPLOT("Zplot (Hurst)", 'z'),
    /** PPL, old PuffinPlot file format */
    PUFFINPLOT_OLD("Old PuffinPlot format", 'o'),
    /** PPL, new PuffinPlot file format */
    PUFFINPLOT_NEW("PuffinPlot", 'p'),
    /** SAM, Caltech (a.k.a. CIT) */
    CALTECH("Caltech (.sam)", 's'),
    /** DAT, [Super-]IAPD[2000] (Torsvik et al.) */
    IAPD("IAPD", 'i'),
    /** Ancient UC Davis 2G format */
    UCDAVIS("UC Davis (old)", 'a'),
    /** Sample-level directional data */
    DIRECTIONS("Sample directions", 'd'),
    /** Custom tabular, defined by a FileFormat */
    CUSTOM_TABULAR("Custom format", 'c'),
    /** Textual PMD format originated by R. Enkin */
    PMD_ENKIN("PMD (Enkin)", 'm'),
    /** JR6 format used by AGICO software */
    JR6("JR6", 'j'),
    /** Unknown file format */
    UNKNOWN("Unknown", 'u');

    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");
    private final String niceName;
    private final int shortcut;
    
    private FileType(String niceName, int shortcut) {
        this.shortcut = shortcut;
        this.niceName = niceName;
    }
    
    /**
     * Attempts to guess the type of a file from its name and contents.
     * 
     * @param file the file for which to guess the type
     * @return the type of the file; {@code UNKNOWN} if it could not be guessed
     * @throws IOException if an I/O error occurred
     */
    static public FileType guess(File file) throws IOException {
        final String name = file.getName().toLowerCase(Locale.ENGLISH);
        if (name.endsWith(".dat")) return TWOGEE;
        else if (name.endsWith(".iapd")) return IAPD;
        else if (name.endsWith(".txt")) return ZPLOT;
        else if (name.endsWith(".sam")) return CALTECH;
        else if (name.endsWith(".ppl")) {
            FileType result = PUFFINPLOT_OLD;
            try (BufferedReader reader =
                    new BufferedReader(new FileReader(file))) {
                final String line = reader.readLine();
                if (line != null && line.startsWith("PuffinPlot file.")) {
                    result = PUFFINPLOT_NEW;
                }
            }
            return result;
        }
        else return UNKNOWN;
    }

    /**
     * Returns a user-friendly name for this filetype.
     * 
     * The returned string is suitable for display in a user interface.
     * 
     * @return the name of this filetype
     */
    public String getNiceName() {
        return niceName;
    }

    /**
     * @return the shortcut
     */
    public int getShortcut() {
        return shortcut;
    }
}
