package net.talvi.puffinplot.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import net.talvi.puffinplot.PuffinApp;

public enum FileType {
	TWOGEE, ZPLOT, PUFFINPLOT_OLD, PUFFINPLOT_NEW, UNKNOWN;
	
	static public FileType guess(File file) {
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
                    } catch (IOException e) {
                        PuffinApp.getInstance().
                                errorDialog("Error opening file",
                                e.getLocalizedMessage());
                        return UNKNOWN;
                    } finally {
                        try { if (reader != null) reader.close(); }
                        catch (IOException e2) {}  
                    }
                    return result;
                }
		else return UNKNOWN;
	}
}
