package net.talvi.puffinplot.data.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.Vec3;

public class ZplotLoader extends AbstractFileLoader {

    private LineNumberReader reader;
    final private static List<Pattern> HEADERS;
    final private static Pattern numberPattern  = Pattern.compile("\\d+(\\.\\d+)?");
    final private static Pattern whitespace = Pattern.compile("\\s+");
    final private static Pattern delimPattern = Pattern.compile("\\t");
    private File file;
    private String studyType;
    private MeasType measType = MeasType.UNSET;

    static {
        String[] fields = {"^Sample", "^Project", "^Demag.*", "^Declin.*", "^Inclin.*",
            "^Intens.*", "^Operation|^Depth"};
        List<Pattern> fieldPatterns = new ArrayList<Pattern>(fields.length);
        for (String field : fields) {
            fieldPatterns.add(Pattern.compile(field));
        }
        HEADERS = fieldPatterns;
    }
    
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
        final String firstLine = reader.readLine();
        if (firstLine == null) {
            addMessage("%s is empty", file.getName());
            return;
        }
        if (!firstLine.startsWith("File Name:")) {
            addMessage("Ignoring unrecognized file %s", file.getName());
            return;
        }
        // skip ancestor file, date, user name, project
        for (int i = 0; i < 4; i++) reader.readLine();

        // read the study type from the last header line
        studyType = reader.readLine();

        String headerLine;
        do {
            headerLine = reader.readLine();
        } while (headerLine != null && !headerLine.startsWith("Sample"));
        if (headerLine == null) {
            addMessage("Couldn't find header line in ZPlot file %s: ignoring it",
                    file.getName());
            return;
        }
        String[] headers = whitespace.split(headerLine);

        if (headers.length != 7) {
            addMessage("Wrong number of header fields in Zplot file %s:" +
                    ": expected 7, got %s", file.getName(), headers.length);
            return;
        }
        for (int i = 0; i < HEADERS.size(); i++) {
            if (!HEADERS.get(i).matcher(headers[i]).matches()) {
                addMessage("Unknown header field %s in file %s.",
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

    private Datum lineToDatum(String zPlotLine) {
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
        if (measType == MeasType.UNSET) {
         measType = (numberPattern.matcher(depthOrSample).matches())
                ? MeasType.CONTINUOUS
                : MeasType.DISCRETE;
        }
        d.setMeasType(measType);
        switch (measType) {
        case CONTINUOUS: d.setDepth(depthOrSample);
            break;
        case DISCRETE: d.setDiscreteId(depthOrSample);
            break;
        default: throw new Error("Unhandled measurement type "+measType);
        }

        d.setTreatType((project.toLowerCase().contains("therm") ||
                operation.toLowerCase().contains("therm") ||
                studyType.toLowerCase().contains("therm"))
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
