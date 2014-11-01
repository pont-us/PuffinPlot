/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
 *
 * PuffinPlot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PuffinPlot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static java.lang.Math.atan;
import static java.lang.Math.toDegrees;

/**
 * A class to calculate and store the eigenvalues and eigenvectors of a matrix.
 * It can also construct an orientation matrix from a supplied collection
 * of vectors and perform eigen analysis on that matrix.
 *
 * @author pont
 */

public class Eigens {

    private final List<Vec3> vectors;
    private final List<Double> values;

    /**
     * Create an object holding the eigenvectors and eigenvalues of the
     * supplied matrix. They will be sorted in descending order of
     * eigenvalue.
     * 
     * @param matrix the matrix on which to perform eigen analysis
     */
    public Eigens(Matrix matrix) {
        final EigenvalueDecomposition eigDecomp = matrix.eig();
        final double[] eigenvalues = eigDecomp.getRealEigenvalues();

        /* There has to be a less horrific method of arranging the
         * results in order of decreasing eigenvalue magnitude,
         * but I haven't yet discovered it.
         */
        final int[] o = order(eigenvalues);
        final Matrix evs = eigDecomp.getV();
        final List<Vec3> vectorsTmp = new ArrayList<>(3);
        final List<Double> valuesTmp = new ArrayList<>(3);
        final double[] v1 = evs.getMatrix(0, 2, o[0], o[0]).getColumnPackedCopy();
        final double[] v2 = evs.getMatrix(0, 2, o[1], o[1]).getColumnPackedCopy();
        final double[] v3 = evs.getMatrix(0, 2, o[2], o[2]).getColumnPackedCopy();
        vectorsTmp.add(new Vec3(v1[0], v1[1], v1[2]));
        vectorsTmp.add(new Vec3(v2[0], v2[1], v2[2]));
        vectorsTmp.add(new Vec3(v3[0], v3[1], v3[2]));
        valuesTmp.add(abs(eigenvalues[o[0]]));
        valuesTmp.add(abs(eigenvalues[o[1]]));
        valuesTmp.add(abs(eigenvalues[o[2]]));

        vectors = Collections.unmodifiableList(vectorsTmp);
        values = Collections.unmodifiableList(valuesTmp);
    }
    
    /**
     * Create an orientation tensor from the supplied vectors, then return
     * the results of eigen analysis upon the constructed matrix.
     * 
     * @param vectors a collection of three-dimensional vectors
     * @param normalize {@code true} to normalize the vectors before analysis
     * @return the eigenvectors and eigenvalues of the orientation tensor
     */
    public static Eigens fromVectors(Collection<Vec3> vectors, boolean normalize) {
        final Matrix orientationTensor = new Matrix(3,3); // zeros
        for (Vec3 vector: vectors) {
            orientationTensor.plusEquals(normalize
                    ? vector.normalize().oTensor()
                    : vector.oTensor());
        }
        return new Eigens(orientationTensor);
    }

    /** Returns a matrix of the eigenvectors.
     * @return a matrix of the eigenvectors
     */
    public Matrix toMatrix() {
        final Vec3 v1 = getVectors().get(0);
        final Vec3 v2 = getVectors().get(1);
        final Vec3 v3 = getVectors().get(2);
        final double[] elts = {
            v1.x, v1.y, v1.z,
            v2.x, v2.y, v2.z,
            v3.x, v3.y, v3.z };
        return new Matrix(elts, 3);
    }

    private static int[] order(double[] x) {
        class Pair implements Comparable<Pair> {
            private final int index;
            private final double value;
            Pair(int index, double value) {
                this.index = index;
                this.value = value;
            }
            @Override
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

        ArrayList<Pair> ps = new ArrayList<>(x.length);
        for (int i=0; i<x.length; i++) {
            ps.add(new Pair(i, x[i]));
        }
        Collections.sort(ps);
        final int[] result = new int[x.length];
        for (int i=0; i<x.length; i++) {
            result[i] = ps.get(i).index;
        }
        return result;
    }

    /** Returns the eigenvectors in order of decreasing eigenvalue.
     * @return the eigenvectors in order of decreasing eigenvalue
     */
    public List<Vec3> getVectors() {
        return vectors;
    }

    /** Returns the eigenvalues in decreasing order.
     * @return the eigenvalues in decreasing order
     */
    public List<Double> getValues() {
        return values;
    }
    
    /**
     * @return the MAD1 (Maximum Angular Deviation planarity) index
     */
    public double getMad1() {
        final double lmax = getValues().get(0);
        final double lint = getValues().get(1);
        final double lmin = getValues().get(2);
        final double mad1 = (lint != 0 && lmax != 0) ?
                toDegrees(atan(sqrt(lmin/lint + lmin/lmax))) : 0;
        return mad1;
    }
    
    /**
     * @return the MAD3 (Maximum Angular Deviation linearity) index
     */
    public double getMad3() {
        final double lmax = getValues().get(0);
        final double lint = getValues().get(1);
        final double lmin = getValues().get(2);
        final double mad3 = toDegrees(atan(sqrt((lint + lmin) / lmax)));
        return mad3;
    }
}
