package net.talvi.puffinplot.data.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.TwoGeeField;

public class TwoGeeLoader implements FileLoader {

    // Note that sensors may have negative effective lengths, depending
    // on how they're mounted. These are the absolute values, and some
    // may be negated when calculating magnetization vectors for long cores.
    // (See the constructor for details.)
    private static final double sensorLengthX = 4.628,
     sensorLengthY = 4.404,
     sensorLengthZ = 6.280;

    private final LineNumberReader reader;
    private Map<String,Integer> fields;
    private Datum nextDatum;
    private LoadingStatus status;
    private static final Pattern emptyLine = Pattern.compile("^\\s*$");
    private MeasType measType;
    private final String fileName;
    private List<String> loadWarnings;
    private static final int MAX_WARNINGS = 10;

    public TwoGeeLoader(File file) throws IOException {
        fileName = file.getName();
        reader = new LineNumberReader(new FileReader(file));
        String fieldsLine = reader.readLine();
        if (fieldsLine == null) {
            addWarning("%s is empty.", fileName);
            reader.close();
            status = LoadingStatus.COMPLETE;
        } else {
            fields = new HashMap<String, Integer>();
            String[] fieldNames = fieldsLine.split(("\\t"));
            for (int i=0; i<fieldNames.length; i++)
                fields.put(fieldNames[i], i);
            fields = new Fields(fieldsLine);
            if (fields.areAllUnknown()) {
                addWarning("%s doesn't look like a 2G or PPL file. " +
                        "Ignoring it.", fileName);
                reader.close();
                status = LoadingStatus.ABORTED;
            } else {
                status = LoadingStatus.IN_PROGRESS;
                readNextDatum();
            }
        }
    }

    private static class NaScanner {

/*  The nice way to do this would be to define a DecimalFormat where "NA" is the
 *  NaN symbol. But we can't specify a NumberFormat for a scanner, only a locale
 *  -- in which case we'd have to define a custom locale with the correct
 *  DecimalFormat, register it via the Java Extension Mechanism, and then
 *  select it with Scanner.useLocale(). NumberFormatProviders aren't available
 *  in Java 5 so this would be impossible on OS X anyway, and either way
 *  it would be a lot of effort.
 */

        private final static Pattern delimPattern = Pattern.compile("\\t");
        private Scanner s;

        public NaScanner(String line) {
            s = new Scanner(line);
            s.useLocale(Locale.ENGLISH); // don't want to be using commas as decimal separators...
            s.useDelimiter(delimPattern); // might have spaces within fields
        }

        public double nextD() {
            String next = s.next();
            return  ("NA".equals(next))
                    ? Double.NaN
                    : Double.parseDouble(next);
        }

        public int nextInt() {
            return s.nextInt();
        }

        public boolean nextBoolean() {
            return s.nextBoolean();
        }

        public String next() {
            return s.next();
        }
    }

    private void readNextDatum() {
        Datum d = null;
        try {
            String line = reader.readLine();
            if (line == null) {
                status = LoadingStatus.COMPLETE;
            } else {
                final int lineNum = reader.getLineNumber();
                try {
                    d = lineToDatum(line, lineNum);
                } catch (IllegalArgumentException e) {
                    addWarning("%s at line %d in file %s -- " + "ignoring this line.",
                            e.getMessage(), lineNum, fileName);
                    if (loadWarnings.size() > MAX_WARNINGS) {
                        addWarning("Too many errors in %s -- " + "aborting load at line %d",
                                fileName, lineNum);
                        status = LoadingStatus.ABORTED;
                    // break;
                    }
                }
            }
        } catch (IOException ex) {
            addWarning("Error reading %s: %s", fileName, ex.getLocalizedMessage());
        }
    }

    public Datum getNext() {
        Datum d = nextDatum;
        readNextDatum();
        return d;
    }

    public LoadingStatus getStatus() {
        return status;
    }

    public Iterable<String> getMessages() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void addWarning(String s, Object... args) {
        loadWarnings.add(String.format(s, args));
    }

    private double getField(String[] values, String name) {

    }

    private Datum lineToDatum(String line, int lineNumber) {
        final boolean oldSquid = PuffinApp.getInstance().getPrefs().isUseOldSquidOrientations();
        String[] values = line.split("\\t");
        Datum d = new Datum(getField(values, "X corr"),
                getField(values, "Y corr"),
                getField(values, "Z corr"));
        if (!emptyLine.matcher(line).matches()) {
            Datum d = new Datum(line, fields, null /*getLineContainer(lineNumber)*/, oldSquid);
            if (d.getMeasType() != MeasType.NONE) {
                if (measType == MeasType.UNSET)
                    measType = d.getMeasType();
                if (d.getMeasType() != measType) {
                    throw new IllegalArgumentException
                            ("Can't mix long core and discrete measurements.");
                }
            }
            return d;
        } else return null;
    }

    private static class Fields {
        List<TwoGeeField> fields;
        List<String> unknown;

        Fields(String header) {
            fields = new LinkedList<TwoGeeField>();
            unknown = new LinkedList<String>();
            Scanner scanner = new Scanner(header);
            scanner.useDelimiter(Pattern.compile("\\t")); // might have spaces within fields
            while (scanner.hasNext()) {
                String name = scanner.next();
                TwoGeeField field = TwoGeeField.getByHeader(name);
                fields.add(field);
                if (field == TwoGeeField.UNKNOWN) unknown.add(name);
            }
        }

        public boolean areAllUnknown() {
            for (TwoGeeField field: fields)
                if (field != TwoGeeField.UNKNOWN)
                    return false;
            return true;
        }
    }

}
