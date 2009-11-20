package net.talvi.puffinplot.data;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import static java.lang.Math.abs;

/**
 * A class to calculate the eigenvalues and vectors of the orientation
 * matrix of a supplied collection of vectors.
 *
 * @author pont
 */

public class Eigens {

    public final List<Vec3> vectors ;
    public final List<Double> values;

    public Eigens(Matrix m) {
        EigenvalueDecomposition eigDecomp = m.eig();
        double[] eigenvalues = eigDecomp.getRealEigenvalues();

        /* There has to be a less horrific method of arranging the
         * results in order of decreasing eigenvalue magnitude,
         * but I haven't yet discovered it.
         */
        int[] o = order(eigenvalues);
        Matrix evs = eigDecomp.getV();
        List<Vec3> vectorsTmp = new ArrayList<Vec3>(3);
        List<Double> valuesTmp = new ArrayList<Double>(3);
        double[] v1 = evs.getMatrix(0, 2, o[0], o[0]).getColumnPackedCopy();
        double[] v2 = evs.getMatrix(0, 2, o[1], o[1]).getColumnPackedCopy();
        double[] v3 = evs.getMatrix(0, 2, o[2], o[2]).getColumnPackedCopy();
        vectorsTmp.add(new Vec3(v1[0], v1[1], v1[2]));
        vectorsTmp.add(new Vec3(v2[0], v2[1], v2[2]));
        vectorsTmp.add(new Vec3(v3[0], v3[1], v3[2]));
        valuesTmp.add(abs(eigenvalues[o[0]]));
        valuesTmp.add(abs(eigenvalues[o[1]]));
        valuesTmp.add(abs(eigenvalues[o[2]]));

        vectors = Collections.unmodifiableList(vectorsTmp);
        values = Collections.unmodifiableList(valuesTmp);
    }
    
    public static Eigens fromVectors(Collection<Vec3> vs, boolean normalize) {
        Matrix oTensor = new Matrix(3,3); // zeros
        for (Vec3 p: vs) 
            oTensor.plusEquals(normalize ?
                p.normalize().oTensor() : p.oTensor());
        return new Eigens(oTensor);
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
                return (o == null) ? false : (o instanceof Pair)
                        ? (compareTo((Pair) o) == 0) : false;
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
}
