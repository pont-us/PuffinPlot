/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2015 Pontus Lurcock.
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

import Jama.Matrix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A second-order symmetric tensor used to represent the anisotropy of
 * magnetic susceptibility.
 * 
 * @author pont
 */
public class Tensor {

    private final double k11, k22, k33, k12, k23, k13;
    private final List<Vec3> amsAxes;
    private static final List<String> HEADERS =
            Arrays.asList("AMS dec1", "AMS inc1", "AMS dec2", "AMS inc2",
            "AMS dec3", "AMS inc3");

    /** Creates a tensor with the specified components. Since the
     * tensor is symmetric, only six components need to be defined.
     * 
     * @param k11 (1,1) component
     * @param k22 (2,2) component
     * @param k33 (3,3) component
     * @param k12 (1,2) and (2,1) component
     * @param k23 (2,3) and (3,2) component
     * @param k13 (1,3) and (3,1) component
     */
    public Tensor(double k11, double k22, double k33,
            double k12, double k23, double k13) {
        this.k11 = k11;
        this.k22 = k22;
        this.k33 = k33;
        this.k12 = k12;
        this.k23 = k23;
        this.k13 = k13;
        final double[] elts = {k11, k12, k13, k12, k22, k23, k13, k23, k33};
        final Matrix ams = new Matrix(elts, 3);
        final Eigens amsEigens = new Eigens(ams);
        // For the present, we just keep the directions
        amsAxes = amsEigens.getVectors();
    }

    /** Creates a tensor with the specified components and
     * transformed using the specified matrices. The tensor
     * is constructed by first making a tensor with the
     * specified components, then sequentially transforming it
     * by the two specified matrices. Since the
     * tensor is symmetric, only six components need to be defined.
     * 
     * @param k11 (1,1) component
     * @param k22 (2,2) component
     * @param k33 (3,3) component
     * @param k12 (1,2) and (2,1) component
     * @param k23 (2,3) and (3,2) component
     * @param k13 (1,3) and (3,1) component
     * @param correct1 first correction matrix
     * @param correct2 second correction matrix
     */
    public Tensor(double k11, double k22, double k33,
            double k12, double k23, double k13, Matrix correct1, Matrix correct2) {
        final double[] elts = {k11, k12, k13, k12, k22, k23, k13, k23, k33};
        final Matrix ams1 = new Matrix(elts, 3);
        final Matrix ams2 = correct1.times(ams1).times(correct1.transpose());
        final Matrix ams3 = correct2.times(ams2).times(correct2.transpose());
        final Eigens amsEigens = new Eigens(ams3);
        double[][] k = ams3.getArray();
        this.k11 = k[0][0];
        this.k12 = k[0][1];
        this.k13 = k[0][2];
        this.k22 = k[1][1];
        this.k23 = k[1][2];
        this.k33 = k[2][2];
        amsAxes = amsEigens.getVectors();
    }

    /** Creates a tensor with the specified principal axes.
     * @param axes the principal axes of the tensor
     */
    public Tensor(List<Vec3> axes) {
        // TODO test this properly
        final Vec3 v1 = axes.get(0);
        final Vec3 v2 = axes.get(1);
        final Vec3 v3 = axes.get(2);
        k11 = v1.x;
        k12 = v1.y;
        k13 = v1.z;
        k22 = v2.y;
        k23 = v2.z;
        k33 = v3.z;
        amsAxes = axes;
    }

    /** Returns a string giving the components of the tensor, separated by spaces.
     * The order is k11, k22, k33, k12, k23, k13.
     * @return a string giving the components of the tensor, separated by spaces */
    public String toTensorComponentString() {
        final String fmt = "%.5f %.5f %.5f %.5f %.5f %.5f";
        return String.format(Locale.ENGLISH, fmt, k11, k22, k33, k12, k23, k13);
    }

    /** Creates a tensor with the specified axes.
     * @param k1 major axis
     * @param k2 intermediate axis
     * @param k3 minor axis
     * @return a tensor with the specified axes
     */
    public static Tensor fromDirections(Vec3 k1, Vec3 k2, Vec3 k3) {
        final List<Vec3> axes = new ArrayList<>(3);
        axes.add(k1);
        axes.add(k2);
        axes.add(k3);
        return new Tensor(axes);
    }
    
    private static Tensor fromDirections(double i1, double d1, double i2,
            double d2, double i3, double d3) {
        final Vec3 v1 = Vec3.fromPolarDegrees(1, i1, d1);
        final Vec3 v2 = Vec3.fromPolarDegrees(1, i2, d2);
        final Vec3 v3 = Vec3.fromPolarDegrees(1, i3, d3);
        return Tensor.fromDirections(v1, v2, v3);
    }

    /** Returns one of the tensor's three principal axes as a vector. 
     * @param axis 0 for major axis, 1 for intermediate, and 2 for minor
     * @return the requested axis
     */
    public Vec3 getAxis(int axis) {
        return amsAxes.get(axis);
    }
        
    private String fmt(double d) {
        return String.format(Locale.ENGLISH, "%.1f", d);
    }

    /** Returns the principal directions as a list of strings.
     * The order of the parameters is the same as the order of
     * the headers provided by {@link #getHeaders()}.
     * @return the principal directions as a list of strings
     */
    public List<String> toStrings() {
        final Vec3 p1 = amsAxes.get(0),
                p2 = amsAxes.get(1),
                p3 = amsAxes.get(2);
        return Arrays.asList(fmt(p1.getDecDeg()), fmt(p1.getIncDeg()),
                fmt(p2.getDecDeg()), fmt(p2.getIncDeg()),
                fmt(p3.getDecDeg()), fmt(p3.getIncDeg()));
    }

    /** Returns a list of empty strings equal in length to the number of parameters.
     * @return  a list of empty strings equal in length to the number of parameters
     */
    public static List<String> getEmptyFields() {
        return Collections.nCopies(HEADERS.size(), "");
    }

    /** Returns the headers describing the parameters as a list of strings.
     * @return the headers describing the parameters
     */
    public static List<String> getHeaders() {
        return HEADERS;
    }
}
