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
    
    /** DAT file from 2G enterprises Long Core software */
    TWOGEE,
    /** TXT file in the format used by Steve Hurst's Zplot program */
    ZPLOT,
    /** the old PuffinPlot file format */
    PUFFINPLOT_OLD,
    /** the new PuffinPlot file format */
    PUFFINPLOT_NEW,
    /** unknown file format */
    UNKNOWN;

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
		else if (name.endsWith(".txt")) return ZPLOT;
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
