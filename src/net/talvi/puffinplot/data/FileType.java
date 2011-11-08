package net.talvi.puffinplot.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

public enum FileType {
	TWOGEE, ZPLOT, PUFFINPLOT_OLD, PUFFINPLOT_NEW, UNKNOWN;

    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");
    
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
