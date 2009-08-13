package net.talvi.puffinplot.data.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.Vec3;

public class ZplotLoader implements FileLoader {

    private LineNumberReader reader;
    private List<Datum> data;
    final private List<String> loadWarnings = new LinkedList<String>();
    final private static String[] ZPLOT_HEADERS =
      {"Sample", "Project", "Demag", "Declin", "Inclin", "Intens", "Operation"};
    final private static Pattern numberPattern  = Pattern.compile("\\d+(\\.\\d+)?");
    final private static Pattern whitespace = Pattern.compile("\\s+");
    final private static Pattern delimPattern = Pattern.compile("\\t");
    private File file;

    public ZplotLoader(File file) {
        this.file = file;
        data = new LinkedList<Datum>();
        try {
            reader = new LineNumberReader(new FileReader(file));
            readFile();
        } catch (IOException e) {

        }
    }

    private void readFile() throws IOException {
        // Check first line for magic string
        if (!reader.readLine().startsWith("File Name:")) {
            addWarning("Ignoring unrecognized file %s", file.getName());
            return;
        }
        // skip remaining header fields
        for (int i = 0; i < 5; i++) reader.readLine();
        String headerLine = reader.readLine();
        if (headerLine == null) {
            addWarning("Ignoring malformed ZPlot file %s", file.getName());
            return;
        }
        String[] headers = whitespace.split(headerLine);

        if (headers.length != 7) {
            addWarning("Wrong number of header fields in Zplot file %s:" +
                    ": expected 7, got %s", file.getName(), headers.length);
            return;
        }
        for (int i = 0; i < ZPLOT_HEADERS.length; i++) {
            if (!ZPLOT_HEADERS[i].equals(headers[i])) {
                addWarning("Unknown header field %s in file %s.",
                        headers[i], file.getName());
                return;
            }
        }
        String line;
        while ((line = reader.readLine()) != null) {
            Datum d = lineToDatum(line);
            if (d != null) data.add(d);
        }
    }

    public List<Datum> getData() {
        return data;
    }

    public List<String> getMessages() {
        return Collections.unmodifiableList(loadWarnings);
    }

    private void addWarning(String s, Object... args) {
        loadWarnings.add(String.format(s, args));
    }

    private static Datum lineToDatum(String zPlotLine) {
        Scanner s = new Scanner(zPlotLine);
        s.useLocale(Locale.ENGLISH); // don't want to be using commas as decimal separators...
        s.useDelimiter(delimPattern);
        String depthOrSample = s.next();
        String project = s.next();
        double demag = s.nextDouble();
        double dec = s.nextDouble();
        double inc = s.nextDouble();
        double intens = s.nextDouble();
        String operation = s.next();

        Datum d = new Datum(Vec3.fromPolarDegrees(intens, inc, dec));
        MeasType measType = (numberPattern.matcher(depthOrSample).matches())
                ? MeasType.CONTINUOUS
                : MeasType.DISCRETE;
        d.setMeasType(measType);
        switch (measType) {
        case CONTINUOUS: d.setDepth(depthOrSample);
            break;
        case DISCRETE: d.setSampleId(depthOrSample);
            break;
        default: throw new Error("Unhandled measurement type "+measType);
        }

        d.setTreatType(project.toLowerCase().contains("therm") ||
                operation.toLowerCase().contains("therm")
            ? TreatType.THERMAL : TreatType.DEGAUSS_XYZ);
        switch (d.getTreatType()) {
        case DEGAUSS_XYZ:
            d.setAfX(demag);
            d.setAfY(demag);
            d.setAfZ(demag);
            break;
        case THERMAL:
            d.setTemp(demag);
            break;
        default: throw new Error("Unhandled treatment type "+d.getTreatType());
        }
        return d;
    }
}
