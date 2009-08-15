package net.talvi.puffinplot.data.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Arrays;
import net.talvi.puffinplot.data.Datum.Reader;

public class Ppl2Loader extends AbstractFileLoader {

    private LineNumberReader reader;
    private Reader datumReader;

    public Ppl2Loader(File file) {
        try {
            reader = new LineNumberReader(new FileReader(file));
            reader.readLine();
            datumReader = new Reader(Arrays.asList(reader.readLine().split("\t")));
            readFile();
        } catch (IOException e) {
            addMessage(e.getMessage());
        } finally {
            try { reader.close(); } catch (IOException e2) {}
        }
    }

    private void readFile() throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            addDatum(datumReader.fromStrings(Arrays.asList(line.split("\t"))));
        }
    }
}
