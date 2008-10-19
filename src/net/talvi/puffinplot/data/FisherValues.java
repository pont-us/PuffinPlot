package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import static java.lang.Math.acos;
import static java.lang.Math.pow;

public class FisherValues {

    private final double a95;
    private final double k;
    private final Point meanDirection;

    private FisherValues(double a95, double k, Point meanDirection) {
        this.a95 = a95;
        this.k = k;
        this.meanDirection = meanDirection;
    }
    
    public static FisherValues calculate(Collection<Point> points) {
        List<Point> normPoints = new ArrayList<Point>(points.size());
        double N = points.size();
        for (Point p: points) normPoints.add(p.normalize());
        double R = Point.vectorSumLength(normPoints);
        double k = (N-1)/(N-R);
        double p = 0.05;
        double a95 = Math.toDegrees(acos( 1 - ((N-R)/R) * (pow(1/p,1/(N-1))-1) ));
        return new FisherValues(a95, k, Point.meanDirection(points));
    }
    
    public double getA95() {
        return a95;
    }

    public double getK() {
        return k;
    }

    public Point getMeanDirection() {
        return meanDirection;
    }
    
}
