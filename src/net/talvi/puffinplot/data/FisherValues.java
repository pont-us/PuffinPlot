package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import static java.lang.Math.acos;
import static java.lang.Math.pow;

public class FisherValues {

    private final double a95;
    private final double k;
    private final Vec3 meanDirection;
    private final List<Vec3> directions;
    private final static double p = 0.05;
    private static final List<String> HEADERS =
        Arrays.asList("Fisher inc.", "Fisher dec.", "Fisher a95", "Fisher k");

    private FisherValues(List<Vec3> directions, double a95, double k, Vec3 meanDirection) {
        this.directions = Collections.unmodifiableList(directions);
        this.a95 = a95;
        this.k = k;
        this.meanDirection = meanDirection;
    }
    
    public static FisherValues calculate(Collection<Vec3> points) {
        List<Vec3> normPoints = new ArrayList<Vec3>(points.size());
        double N = points.size();
        for (Vec3 point: points) normPoints.add(point.normalize());
        double R = Vec3.vectorSumLength(normPoints);
        double k = (N-1)/(N-R);
        double a95 = Math.toDegrees(acos( 1 - ((N-R)/R) * (pow(1/p,1/(N-1))-1) ));
        return new FisherValues(normPoints, a95, k, Vec3.meanDirection(normPoints));
    }
    
    public double getA95() {
        return a95;
    }

    public double getK() {
        return k;
    }

    public Vec3 getMeanDirection() {
        return meanDirection;
    }
    
    public List<Vec3> getDirections() {
        return directions;
    }

    private String fmt(double d) {
        return String.format("%.1f", d);
    }

    public List<String> toStrings() {
        return Arrays.asList(fmt(getMeanDirection().getIncDeg()),
                fmt(getMeanDirection().getDecDeg()), fmt(getA95()),
                fmt(getK()));
    }

    public static List<String> getHeaders() {
        return HEADERS;
    }

    public static List<String> getEmptyFields() {
        return Collections.nCopies(HEADERS.size(), "");
    }
}
