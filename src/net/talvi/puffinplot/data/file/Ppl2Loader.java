package net.talvi.puffinplot.data.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.talvi.puffinplot.data.Datum.Reader;

public class Ppl2Loader extends AbstractFileLoader {

    private static final Logger logger = Logger.getLogger(Ppl2Loader.class.getName());
    private LineNumberReader reader;
    private Reader datumReader;

    public Ppl2Loader(File file) {
        try {
            reader = new LineNumberReader(new FileReader(file));
            final String firstLine = reader.readLine();
            if (firstLine == null) {
                throw new IOException(file + " is empty.");
            }
            if (!firstLine.equals("PuffinPlot file. Version 2")) {
                throw new IOException(file + "is not a version 2 PuffinPlot file.");
            }
            final String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException(file + " contains no headers or data.");
            }
            datumReader = new Reader(Arrays.asList(headerLine.split("\t")));
            readFile();
        } catch (IOException e) {
            addMessage(e.getMessage());
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Failed to close reader: ", ex);
            }
        }
    }

    private void readFile() throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            addDatum(datumReader.fromStrings(Arrays.asList(line.split("\t"))));
        }
    }
}
