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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * This class performs three-dimension principal component analysis on
 * a supplied collection of vectors and stores the results.
 * 
 * @see PcaAnnotated
 * @author pont
 */
public class PcaValues {
    private final double mad1;
    private final double mad3;
    private final Vec3 direction;
    private final Vec3 origin;
    private final boolean anchored;
    private static final List<String> HEADERS =
        Arrays.asList("PCA dec. (°)", "PCA inc. (°)", "PCA MAD1", "PCA MAD3", "PCA anchored", "PCA equation");
    
    private PcaValues(Vec3 direction, double mad1, double mad3,
            Vec3 origin, boolean anchored) {
        this.direction = direction;
        this.mad1 = mad1;
        this.mad3 = mad3;
        this.origin = origin;
        this.anchored = anchored;
    }

    /**
     * Performs principal component analysis (PCA) on the supplied vectors
     * and returns an object containing the results of the analysis.
     * 
     * @param points the points upon which to perform PCA
     * @param anchored {@code true} to anchor the PCA to the origin
     * @return the results of the PCA
     */
    public static PcaValues calculate(List<Vec3> points, boolean anchored) {
        // We use Kirschvink's procedure but append a direction correction.

        List<Vec3> movedPoints = points;
        Vec3 origin = anchored ? Vec3.ORIGIN : Vec3.mean(points);
        if (!anchored) {
            // translate points to be centred on centre of mass
            movedPoints = new ArrayList<>(points.size());
            for (Vec3 p: points) movedPoints.add(p.minus(origin));
        }

        Eigens eigen = Eigens.fromVectors(movedPoints, false);
        Vec3 pComp = eigen.getVectors().get(0);

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

        return new PcaValues(pComp, eigen.getMad1(), eigen.getMad3(), origin, anchored);
    }

    /** Returns the maximum angle of planar deviation.
     * This is defined by Kirschvink (1980) p. 703.
     * @return the maximum angle of planar deviation
     */
    public double getMad1() {
        return mad1;
    }

    /** Returns the maximum angle of linear deviation.
     * This is defined by Kirschvink (1980) p. 703.
     * @return the maximum angle of linear deviation
     */
    public double getMad3() {
        return mad3;
    }

    /**
     * Returns the origin for the PCA fit. If this is an anchored PCA fit,
     * this will be the zero vector, the origin for the co-ordinate system.
     * Otherwise it will be the centre of mass of the points.
     * 
     * @return the origin for the PCA fit
     */
    public Vec3 getOrigin() {
        return origin;
    }

    /** Returns the direction of the principal PCA axis. 
     * @return the direction of the principal PCA axis
     */
    public Vec3 getDirection() {
        return direction;
    }
    
    /** Reports whether the PCA calculation was anchored.
     * @return {@code true} is the PCA calculation was anchored */
    public boolean isAnchored() {
        return anchored;
    }

    private String fmt(double d) {
        return String.format(Locale.ENGLISH, "%.1f", d);
    }

    /** Returns the headers describing the parameters as a list of strings.
     * @return the headers describing the parameters */
    public static List<String> getHeaders() {
        return HEADERS;
    }

    /**
     * Return a Cartesian equation describing the PCA line.
     *
     * @return a String giving an equation for the PCA line
     */
    public String getEquation() {
        StringBuilder sb = new StringBuilder();
        if (origin != Vec3.ORIGIN) {
            sb.append(origin.toCustomString("(", ")", " ", 2, true));
            sb.append(" + ");
        }
        sb.append(direction.toCustomString("(", ")", " ", 2, false));
        sb.append("t");
        return sb.toString();
    }

    /** Returns the parameters as a list of strings.
     * The order of the parameters is the same as the order of
     * the headers provided by {@link #getHeaders()}.
     * @return the parameters as a list of strings
     */
    public List<String> toStrings() {
        return Arrays.asList(fmt(direction.getDecDeg()), 
                fmt(direction.getIncDeg()),
            fmt(getMad1()), fmt(getMad3()), anchored ? "Y" : "N",
            getEquation());
    }
}
