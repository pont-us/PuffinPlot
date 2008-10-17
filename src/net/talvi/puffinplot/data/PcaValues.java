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

    public final double inc;
    public final double dec;
    public final double mad1;
    public final double mad3;
    public final Point origin;
    
    private PcaValues(double inc, double dec, double mad1, double mad3, Point origin) {
        super();
        this.inc = inc;
        this.dec = dec;
        this.mad1 = mad1;
        this.mad3 = mad3;
        this.origin = origin;
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
    
    public static PcaValues calculate(Iterable<Point> points, Point origin) {
        // We use Kirschvink's procedure but append a direction correction.
        
        // translate points to be centred on centre of mass
        ArrayList<Point> movedPoints = new ArrayList<Point>();
        // Point origin = Point.centreOfMass(points);
        for (Point p: points) movedPoints.add(p.minus(origin));
        
        // construct the orientation tensor
        Matrix oTensor = new Matrix(3,3); // zeros
        for (Point p: movedPoints) oTensor.plusEquals(p.oTensor());
        
        EigenvalueDecomposition eigDecomp = oTensor.eig();
        double[] eigenvalues = eigDecomp.getRealEigenvalues();
        int[] eigenOrder = order(eigenvalues);
        
        int[] rowIndices = {0, 1, 2};
        int[] colIndices = {eigenOrder[0]};
        double[] vmax = eigDecomp.getV().
            getMatrix(rowIndices, colIndices).getColumnPackedCopy();
        
        Point pComp = new Point(vmax[0], vmax[1], vmax[2]);
        
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
        Point trend = movedPoints.get(movedPoints.size()-1).
            minus(movedPoints.get(0));
        if (trend.scalarProduct(pComp) > 0) pComp = pComp.invert();

        double inc = pComp.incRadians();
        double dec = pComp.decRadians();

        double mad3 = toDegrees(atan(sqrt((lint + lmin) / lmax)));
        double mad1 = (lint != 0 && lmax != 0) ?
                toDegrees(atan(sqrt(lmin/lint + lmin/lmax))) : 0;
        
        return new PcaValues(inc, dec, mad1, mad3, origin);
    }
    
    @Override
    public String toString() {
        return getDecInc() + " / "+ getMads();
    }
    
    public String getDecInc() {
        return String.format("dec %3.3f / inc %3.3f",
                toDegrees(dec), toDegrees(inc));
    }
    
    public String getMads() {
        return String.format("mad1 %3.3f / mad3 %3.3f",
                mad1, mad3);
    }
    
    
}
