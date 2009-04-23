package net.talvi.puffinplot;

import java.io.File;

public enum FileType {
	TWOGEE, ZPLOT, PUFFINPLOT, UNKNOWN;
	
	static public FileType guessFromName(File f) {
		String name = f.getName().toLowerCase();
		if (name.endsWith(".dat")) return TWOGEE;
		else if (name.endsWith(".txt")) return ZPLOT;
		else if (name.endsWith(".ppl")) return PUFFINPLOT;
		else return UNKNOWN;
	}
}
