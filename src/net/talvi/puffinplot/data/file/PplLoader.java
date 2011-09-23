package net.talvi.puffinplot.data.file;

import java.util.regex.Matcher;
import net.talvi.puffinplot.data.Datum;
import java.util.List;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import java.util.regex.Pattern;
import static net.talvi.puffinplot.data.file.TwoGeeHelper.*;
import static java.lang.Double.isNaN;

public class PplLoader extends AbstractFileLoader {

    private static final Logger logger = Logger.getLogger(PplLoader.class.getName());
    private static final Pattern puffinHeader =
            Pattern.compile("^PuffinPlot file. Version (\\d+)");
    private LineNumberReader reader;
    private Datum.Reader datumReader;
    private int treatmentField;
    private int version;
    private List<String> extraLines = Collections.EMPTY_LIST;
    private final File file;

    public PplLoader(File file) {
        this.file = file;
        try {
            reader = new LineNumberReader(new FileReader(file));
            final String firstLine = reader.readLine();
            if (firstLine == null) {
                throw new IOException(file + " is empty.");
            }
            Matcher matcher = puffinHeader.matcher(firstLine);
            if (!matcher.matches()) {
                throw new IOException(file + "doesn't appear to be a "
                        + "PuffinPlot file.");
            }
            String versionString = matcher.group(1);
            version = Integer.parseInt(versionString);
            if (version != 2 && version != 3) {
                throw new IOException(String.format("%s is of version %d,"
                        + "which cannot be "
                        + "loaded by this version of PuffinPlot."));
            }
            final String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException(file + " contains no headers or data.");
            }
            List<String> headers = Arrays.asList(headerLine.split("\t"));
            treatmentField = headers.indexOf("TREATMENT");
            datumReader = new Datum.Reader(headers);
            readFile();
        } catch (IOException e) {
            addMessage(e.getMessage());
        } catch (MalformedFileException e) {
            addMessage(e.getMessage());
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Failed to close reader: ", ex);
            }
        }
    }

    private void readFile() throws IOException, MalformedFileException {
        String line;
        while ((line = reader.readLine()) != null) {
            if ("".equals(line)) break;
            List<String> values = Arrays.asList(line.split("\t"));
            if (version==2) {
                // Ppl 2 files still use the 2G strings for treatment types,
                // so we munge it into a suitable input for TreatType.valueOf
                // before passing it to the Datum reader.
                values.set(treatmentField, treatTypeFromString(values.get
                        (treatmentField)).toString());
                // Fortunately, measurement type strings happen to carry
                // across so we don't need to munge them.
            }
            Datum d = null;
            try {
                d = datumReader.fromStrings(values);
            } catch (NumberFormatException e) {
                final String msg = String.format("Error at line %d "+
                        "of file %s:\n%s", reader.getLineNumber(),
                        file.getName(), e.getMessage());
                throw new MalformedFileException(msg);
            }
            if (version==2) {
                // Ppl 2 files store magnetic data (except susceptibility)
                // in cgs units, which must be corrected on loading.
                d.setMoment(gaussToAm(d.getMoment(Correction.NONE)));
                if (!isNaN(d.getAfX())) d.setAfX(oerstedToTesla(d.getAfX()));
                if (!isNaN(d.getAfY())) d.setAfX(oerstedToTesla(d.getAfY()));
                if (!isNaN(d.getAfZ())) d.setAfX(oerstedToTesla(d.getAfZ()));
                if (!isNaN(d.getIrmField()))
                    d.setIrmField(oerstedToTesla(d.getIrmField()));
                if (!isNaN(d.getArmField()))
                    d.setArmField(oerstedToTesla(d.getArmField()));
            }
            addDatum(d);
        }
        if (line != null) {
            extraLines = new ArrayList<String>();
            while ((line = reader.readLine()) != null) {
                extraLines.add(line);
            }
        }
    }

    @Override
    public List<String> getExtraLines() {
        return extraLines;
    }
}
