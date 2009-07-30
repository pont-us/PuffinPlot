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
        hA.addAll(Arrays.asList("PCA start", "PCA end", "PCA contiguous"));
        HEADERS = Collections.unmodifiableList(hA);
    }

    private PcaAnnotated(PcaValues pcaValues, double demagStart,
            double demagEnd, boolean contiguous) {
        this.pcaValues = pcaValues;
        this.demagStart = demagStart;
        this.demagEnd = demagEnd;
        this.contiguous = contiguous;
    }

    static PcaAnnotated calculate(Sample s) {
        List<Vec3> points = s.getSelectedPoints();
        if (points.size() < 2) return null;
        PcaValues pca = PcaValues.calculate(points, s.isPcaAnchored());
        List<Datum> selection = s.getSelectedData();
        return new PcaAnnotated(pca,
                selection.get(0).getDemagLevel(),
                selection.get(selection.size() - 1).getDemagLevel(),
                s.isSelectionContiguous());
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