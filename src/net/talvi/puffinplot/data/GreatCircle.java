/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2018 Pontus Lurcock.
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents a great circle, created either directly by
 * specifying a pole direction or indirectly by giving a list of
 * vectors. If a list of vectors is given, the constructor calculates the
 * best-fit circle.
 * 
 */
public final class GreatCircle {

    private final static Logger logger =
            Logger.getLogger(GreatCircle.class.getName());

    private final List<Vec3> points;
    private final Vec3 pole;
    private final double mad1;
    private final double pointTrend; // direction of points along circle: +1 or -1 
    // TODO: need a way to represent sector constraints here...
    private static final List<String> HEADERS =
            Arrays.asList("GC dec (deg)", "GC inc (deg)",
            "GC strike (deg)", "GC dip (deg)",
            "GC MAD1", "GC npoints");

    private GreatCircle(Vec3 pole, List<Vec3> points, double mad1) {
        this.pole = pole;
        this.points = points;
        this.mad1 = mad1;
        
        /* Calculate the direction of the point trend along the circle
         * (clockwise / anticlockwise). Can't be sure that all points
         * will be strictly in the right direction, so we'll add up
         * the offsets of all the point pairs and take the sign of the sum.
         * (Can't just take angle between first and last point, since
         * the path might be more than 180 degrees -- though of course
         * that would imply something more complex than a single component
         * peeling off.)
         */
        double total = 0;
        for (int i=1; i<points.size(); i++) {
            total += nearestOnCircle(points.get(i-1)).
                    angleTo(nearestOnCircle(points.get(i)));
        }
        pointTrend = total == 0 ? Double.NaN : Math.signum(total);
        
        for (Vec3 p: points) {
            logger.log(Level.FINEST, "{0}   {1}", new Object[] {p.toString(),
                angleFromLast(nearestOnCircle(p))});
        }
    }

    /**
     * Create a great circle with the given pole. This produces a great
     * circle with no associated points; methods which require points
     * to return a meaningful result will throw an UnsupportedOperationException
     * if called on a great circle instantiated using this constructor.
     * 
     * @param pole pole direction for the great circle
     * @return a great circle with the supplied pole
     */
    public static GreatCircle fromPole(Vec3 pole) {
        return new GreatCircle(pole, Collections.emptyList(),
                Double.NaN);
    }
    
    /**
     * Constructs a best-fitting great circle for the supplied vectors.
     * The vectors do not need to be normalized, but only their directions
     * are considered when fitting the circle.
     * 
     * @param vectors the direction vectors for which to fit the circle
     * @return a great circle fitted to the supplied vectors
     */
    public static GreatCircle fromBestFit(List<Vec3> vectors) {
                final List<Vec3> pointsUnscaled = vectors;
        final List<Vec3> points = new ArrayList<>(vectors.size());
        for (Vec3 p: pointsUnscaled) {
            points.add(p.normalize());
        }
        final Eigens eigens = Eigens.fromVectors(points, true);
        final Vec3 pole = eigens.getVectors().get(2).normalize();
        final double mad1 = eigens.getMad1();
        return new GreatCircle(pole, points, mad1);
    }

    
    /** Returns the normalized points to which the great circle was fitted,
     * if any. If there are none, an empty list will be returned.
     * 
     * @return the normalized points to which the great circle was fitted */
    public List<Vec3> getPoints() {
        return points;
    }

    /** Returns a pole to the fitted great circle. There is no guarantee
     * as to which of the two possible pole directions will be returned.
     * @return a pole to the fitted great circle
     */
    public Vec3 getPole() {
        return pole;
    }

    /** For a supplied direction, returns the nearest direction which lies
     * on this great circle.
     * @param point a vector specifying a direction
     * @return a vector on this great circle, as close as possible to the specified direction
     */
    public Vec3 nearestOnCircle(Vec3 point) {
        return pole.nearestOnCircle(point);
    }

    /** Returns the normalized final point used in the great-circle fit,
     * if any. If the great circle was instantiated from a pole rather
     * than a set of points, there is no final point to return, and
     * this method will throw a {@code UnsupportedOperationException}.
     * 
     * @return the normalized final point used in the great-circle fit
     * @throws UnsupportedOperationException if this great circle
     *    has no points
     */
    public Vec3 lastPoint() {
        if (points.isEmpty()) {
            throw new UnsupportedOperationException(
                    "This great circle has no points.");
        }
        return points.get(points.size()-1);
    }

    /** Returns the angle between the supplied direction and the last point
     * on the great-circle fit. A negative value indicates that the 
     * supplied direction is before the last great-circle point when 
     * travelling along the great circle in the direction in which the
     * original points were provided. A positive value indicates that
     * the supplied direction is beyond the final great-circle point.
     * 
     * If the great circle has no points, this method will throw an
     * UnsupportedOperationException.
     * 
     * @param v a direction
     * @return the angle between the supplied direction and the last point
     * on the great-circle fit
     * @throws UnsupportedOperationException if this great circle has
     * no points
     */
    public double angleFromLast(Vec3 v) {
        /* We don't need to check and throw UnsupportOperationException
         * explicitly here: lastPoint() will throw it if there are
         * no points.
         */
        return nearestOnCircle(lastPoint()).angleTo(v)
                * pointTrend;
    }
    
    private String fmt(double d) {
        return String.format(Locale.ENGLISH, "%.4f", d);
    }
    
    /** Returns the statistical parameters as a list of strings.
     * The order of the parameters is the same as the order of
     * the headers provided by {@link #getHeaders()}.
     * @return the statistical parameters as a list of strings
     */
    public List<String> toStrings() {
        return Arrays.asList(fmt(pole.getDecDeg()), fmt(pole.getIncDeg()),
                fmt(pole.getStrikeDeg()), fmt(pole.getDipDeg()),
                fmt(getMad1()), Integer.toString(points.size()));
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

    /**
     * Returns the MAD1 (planar maximum angular deviation) value for
     * the great-circle fit. If this great circle was not created by
     * fitting points, this method will throw an UnsupportedOperationException.
     * 
     * @return the MAD1 value for the great-circle fit, if any
     * @throws UnsupportedOperationException if no fit was performed to
     * produce this great circle
     */
    public double getMad1() {
        if (Double.isNaN(mad1)) {
            throw new UnsupportedOperationException("This great circle "
                    + "was not created by a fitting procedure; there is"
                    + "no MAD1 value.");
        }
        return mad1;
    }
}
