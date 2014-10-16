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
import java.util.logging.Logger;

/**
 * This fields of this enum represent the types of file that PuffinPlot
 * can read.
 * 
 * @author pont
 */
public enum FileType {
    
    
    TWOGEE,         // DAT file from 2G enterprises Long Core software
    ZPLOT,          // TXT file format used by Steve Hurst's Zplot program
    PUFFINPLOT_OLD, // the old PuffinPlot file format
    PUFFINPLOT_NEW, // the new PuffinPlot file format
    CALTECH,        // the Caltech (a.k.a. CIT, .sam) file format
    IAPD,           // The (Super-)IAPD format (Torsvik et al.)
    DIRECTIONS,     // Sample-level directional data
    CUSTOM_TABULAR, // custom tabular format defined by a FileFormat object
    UNKNOWN;        // unknown file format


    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");
    
    /**
     * Attempts to guess the type of a file from its name and contents.
     * 
     * @param file the file for which to guess the type
     * @return the type of the file; {@code UNKNOWN} if it could not be guessed
     * @throws IOException if an I/O error occurred
     */
	static public FileType guess(File file) throws IOException {
		String name = file.getName().toLowerCase();
		if (name.endsWith(".dat")) return TWOGEE;
                else if (name.endsWith(".iapd")) return IAPD;
		else if (name.endsWith(".txt")) return ZPLOT;
                else if (name.endsWith(".sam")) return CALTECH;
		else if (name.endsWith(".ppl")) {
                    BufferedReader reader = null;
                    FileType result = PUFFINPLOT_OLD;
                    try {
                         reader = new BufferedReader(new FileReader(file));
                         String line = reader.readLine();
                         if (line.startsWith("PuffinPlot file."))
                             result = PUFFINPLOT_NEW;
                    } finally {
                        try { if (reader != null) reader.close(); }
                        catch (IOException ex) {
                            logger.warning(ex.getLocalizedMessage());
                        }
                    }
                    return result;
                }
		else return UNKNOWN;
	}
}
