package net.talvi.puffinplot.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MDF {

    private final double demagLevel;
    private final double intensity;
    private final boolean halfIntReached;
    private static final List<String> HEADERS =
        Arrays.asList("MDF half-intensity", "MDF demagnetization",
        "MDF midpoint reached");

    private MDF(double demagLevel, double intensity,
            boolean halfIntReached) {
        this.demagLevel = demagLevel;
        this.intensity = intensity;
        this.halfIntReached = halfIntReached;
    }

    private static double interpolate(double x0, double x1,
            double y0, double y1, double y) {
        double x =  x0 + (x1 - x0) * (y1 - y) / (y1 - y0);
        return x;
    }

    public static MDF calculate(List<Datum> data) {
        if (data.size() < 2) return null;
        final double initialIntensity = data.get(0).getIntensity();
        final double halfIntensity = initialIntensity / 2;
        boolean halfIntReached = false;
        int i = 1;
        Datum d;
        do {
            d = data.get(i);
            i++;
        } while (i < data.size() &&
                d.getIntensity() > halfIntensity);
        if (d.getIntensity() <= halfIntensity) {
            halfIntReached = true;
        }
        Datum dPrev = data.get(i-2); // i can't be <=1 at this point

        return new MDF(
                interpolate(dPrev.getDemagLevel(), d.getDemagLevel(),
                d.getIntensity(), dPrev.getIntensity(), halfIntensity),
                halfIntensity,
                halfIntReached);
    }

    public double getDemagLevel() {
        return demagLevel;
    }

    public double getIntensity() {
        return intensity;
    }

    public boolean isHalfIntReached() {
        return halfIntReached;
    }

    public static List<String> getHeaders() {
        return HEADERS;
    }

    public static List<String> getEmptyFields() {
        return Collections.nCopies(HEADERS.size(), "");
    }
    
    public List<String> toStrings() {
        return Arrays.asList(String.format(Locale.ENGLISH, "%.3g", getIntensity()),
                String.format(Locale.ENGLISH, "%.3g", getDemagLevel()),
                isHalfIntReached() ? "yes" : "no");
    }
}
