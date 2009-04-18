package net.talvi.puffinplot.data;

import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;

import java.util.ArrayList;
import java.util.Collections;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

public class PcaValues {
    private final double mad1;
    private final double mad3;
    private final Vec3 origin;
    private final Vec3 direction;
    
    private PcaValues(Vec3 direction, double mad1, double mad3, Vec3 origin) {
        this.mad1 = mad1;
        this.mad3 = mad3;
        this.origin = origin;
        this.direction = direction;
    }

    private static int[] order(double[] x) {
        class Pair implements Comparable<Pair> {
            private final int index;
            private final double value;
            Pair(int index, double value) {
                this.index = index;
                this.value = value;
            }            
            public int compareTo(Pair p) {
                return Double.compare(p.value, value);
            }
            @Override
            public boolean equals(Object o) {
                return (o==null)
                        ? false
                        : (o instanceof Pair)
                        ? (compareTo((Pair) o)==0)
                        : false;
            }
            @Override
            public int hashCode() {
                assert false : "hashCode not designed";
                return 42; // any arbitrary constant will do 
            }
        }
        
        ArrayList<Pair> ps = new ArrayList<Pair>(x.length);
        for (int i=0; i<x.length; i++) ps.add(new Pair(i, x[i]));
        Collections.sort(ps);
        int[] result = new int[x.length];
        for (int i=0; i<x.length; i++) result[i] = ps.get(i).index;
        return result;
    }
    
    public static PcaValues calculate(Iterable<Vec3> points, Vec3 origin) {
        // We use Kirschvink's procedure but append a direction correction.
        
        // translate points to be centred on centre of mass
        ArrayList<Vec3> movedPoints = new ArrayList<Vec3>();
        // Point origin = Point.centreOfMass(points);
        for (Vec3 p: points) movedPoints.add(p.minus(origin));
        
        // construct the orientation tensor
        Matrix oTensor = new Matrix(3,3); // zeros
        for (Vec3 p: movedPoints) oTensor.plusEquals(p.oTensor());
        
        EigenvalueDecomposition eigDecomp = oTensor.eig();
        double[] eigenvalues = eigDecomp.getRealEigenvalues();
        int[] eigenOrder = order(eigenvalues);
        
        int[] rowIndices = {0, 1, 2};
        int[] colIndices = {eigenOrder[0]};
        double[] vmax = eigDecomp.getV().
            getMatrix(rowIndices, colIndices).getColumnPackedCopy();
        
        Vec3 pComp = new Vec3(vmax[0], vmax[1], vmax[2]);
        
        double lmax = abs(eigenvalues[eigenOrder[0]]);
        double lint = abs(eigenvalues[eigenOrder[1]]);
        double lmin = abs(eigenvalues[eigenOrder[2]]);
        
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
        if (trend.scalarProduct(pComp) > 0) pComp = pComp.invert();

        double mad3 = toDegrees(atan(sqrt((lint + lmin) / lmax)));
        double mad1 = (lint != 0 && lmax != 0) ?
                toDegrees(atan(sqrt(lmin/lint + lmin/lmax))) : 0;
        
        return new PcaValues(pComp, mad1, mad3, origin);
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
    
    public static String getHeader(String separator) {
        return String.format("PCA inc.%sPCA dec.%sMAD1%sMAD3",
                separator, separator, separator);
    }

    public String toLine(String separator) {
        return String.format("%.1f%s%.1f%s%.1f%s%.1f",
                getIncDegrees(), separator, getDecDegrees(), separator,
                getMad1(), separator, getMad3());
    }


}
