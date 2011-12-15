package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PcaAnnotated {

    private final PcaValues pcaValues;
    private final double demagStart;
    private final double demagEnd;
    private final boolean contiguous;
    private static final List<String> HEADERS;

    static {
        List<String> hA = new ArrayList<String>();
        hA.addAll(PcaValues.getHeaders());
        hA.addAll(Arrays.asList("PCA start (°C or mT)", "PCA end (°C or mT)",
                "PCA contiguous"));
        HEADERS = Collections.unmodifiableList(hA);
    }

    private PcaAnnotated(PcaValues pcaValues, double demagStart,
            double demagEnd, boolean contiguous) {
        this.pcaValues = pcaValues;
        this.demagStart = demagStart;
        this.demagEnd = demagEnd;
        this.contiguous = contiguous;
    }

    static PcaAnnotated calculate(Sample s, Correction correction) {
        List<Datum> rawData = s.getVisibleData();
        List<Vec3> points = new ArrayList<Vec3>(rawData.size());
        List<Datum> data = new ArrayList<Datum>(rawData.size());
        
        int runEndsSeen = 0;
        boolean thisIsPca = false, lastWasPca = false;
        for (Datum d: rawData) {
            thisIsPca = d.isInPca();
            if (thisIsPca) {
                points.add(d.getMoment(correction));
                data.add(d);
            } else {
                if (lastWasPca) runEndsSeen++;
            }
            lastWasPca = thisIsPca;
        }
        if (thisIsPca) runEndsSeen++;
        boolean contiguous = (runEndsSeen <= 1);
        if (points.size() < 2) return null;
        PcaValues pca = PcaValues.calculate(points, s.isPcaAnchored());
        
        return new PcaAnnotated(pca,
                data.get(0).getTreatmentLevel(),
                data.get(data.size() - 1).getTreatmentLevel(),
                contiguous);
    }

    public PcaValues getPcaValues() {
        return pcaValues;
    }

    public List<String> toStrings() {
        ArrayList<String> result = new ArrayList<String>();
        result.addAll(pcaValues.toStrings());
        result.add(Double.toString(demagStart));
        result.add(Double.toString(demagEnd));
        result.add(contiguous ? "yes" : "no");
        return Collections.unmodifiableList(result);
    }

    public static List<String> getHeaders() {
        return HEADERS;
    }

    public static List<String> getEmptyFields() {
        return Collections.nCopies(HEADERS.size(), "");
    }

}
