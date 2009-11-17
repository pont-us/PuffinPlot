package net.talvi.puffinplot.data;

import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;

import java.util.ArrayList;
import java.util.Collections;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import java.util.Arrays;
import java.util.List;

public class PcaValues {
    private final double mad1;
    private final double mad3;
    private final Vec3 direction;
    private final Vec3 origin;
    private final boolean anchored;
    private static final List<String> HEADERS =
        Arrays.asList("PCA inc.", "PCA dec.", "PCA MAD1", "PCA MAD3", "PCA anchored");
    
    private PcaValues(Vec3 direction, double mad1, double mad3,
            Vec3 origin, boolean anchored) {
        this.direction = direction;
        this.mad1 = mad1;
        this.mad3 = mad3;
        this.origin = origin;
        this.anchored = anchored;
    }

    public static PcaValues calculate(List<Vec3> points, boolean anchored) {
        // We use Kirschvink's procedure but append a direction correction.

        List<Vec3> movedPoints = points;
        Vec3 origin = anchored ? Vec3.ORIGIN : Vec3.centreOfMass(points);
        if (!anchored) {
            // translate points to be centred on centre of mass
            movedPoints = new ArrayList<Vec3>(points.size());
            for (Vec3 p: points) movedPoints.add(p.minus(origin));
        }

        Eigens eigen = new Eigens(movedPoints, false);
        Vec3 pComp = eigen.vectors.get(0);

        /*
         * If the points are linearly arranged, the first eigenvector should now
         * give us the direction of the line; however, we want to make sure that
         * it's pointing in the direction of the magnetic component -- that is,
         * opposite to the trend during progressive demagnetization. We check
         * this by taking the vector pointing from the first to the last of the
         * points under consideration, and calculating the scalar product with
         * the eigenvector.
         */
        
        // We want these in opposite directions, thus negative scalar product
        Vec3 trend = movedPoints.get(movedPoints.size()-1).
            minus(movedPoints.get(0));
        if (trend.dot(pComp) > 0) pComp = pComp.invert();

        double lmax = eigen.values.get(0);
        double lint = eigen.values.get(1);
        double lmin = eigen.values.get(2);
        double mad3 = toDegrees(atan(sqrt((lint + lmin) / lmax)));
        double mad1 = (lint != 0 && lmax != 0) ?
                toDegrees(atan(sqrt(lmin/lint + lmin/lmax))) : 0;
        
        return new PcaValues(pComp, mad1, mad3, origin, anchored);
    }

    public double getIncRadians() {
        return direction.getIncRad();
    }

    public double getDecRadians() {
        return direction.getDecRad();
    }
    
    public double getIncDegrees() {
        return direction.getIncDeg();
    }

    public double getDecDegrees() {
        return direction.getDecDeg();
    }

    public double getMad1() {
        return mad1;
    }

    public double getMad3() {
        return mad3;
    }

    public Vec3 getOrigin() {
        return origin;
    }

    public Vec3 getDirection() {
        return direction;
    }

    private String fmt(double d) {
        return String.format("%.1f", d);
    }

    public static List<String> getHeaders() {
        return HEADERS;
    }

    public List<String> toStrings() {
        return Arrays.asList(fmt(getIncDegrees()), fmt(getDecDegrees()),
            fmt(getMad1()), fmt(getMad3()), anchored ? "yes" : "no");
    }
}
