package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * This class represents a list of unit vectors and a best-fit great-circle
 * path through them. The constructor calculates the best-fit circle from
 * a supplied set of vectors.
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
            Arrays.asList("GC dec (°)", "GC inc (°)", "GC npoints");

    /**
     * Constructs a best-fitting great circle for the supplied vectors.
     * The vectors do not need to be normalized, but only their directions
     * are considered when fitting the circle.
     * 
     * @param vectors the direction vectors for which to fit the circle
     */
    public GreatCircle(List<Vec3> vectors) {
        final List<Vec3> pointsUnscaled = vectors;
        points = new ArrayList<Vec3>(vectors.size());
        for (Vec3 p: pointsUnscaled) {
            this.points.add(p.normalize());
        }
        Eigens eigens = Eigens.fromVectors(points, true);
        this.pole = eigens.getVectors().get(2).normalize();
        mad1 = eigens.getMad1();
        
        // Calculate the direction of the point trend along the circle
        // (clockwise / anticlockwise). Can't be sure that all points
        // will be strictly in the right direction, so we'll add up
        // the offsets of all the point pairs and take the sign of the sum.
        // (Can't just take angle between first and last point, since
        // the path might be more than 180 degrees -- though of course
        // that would imply something more complex than a single component
        // peeling off.)
        double total = 0;
        for (int i=1; i<points.size(); i++) {
            total += nearestOnCircle(points.get(i-1)).
                    angleTo(nearestOnCircle(points.get(i)));
        }
        pointTrend = Math.signum(total);
        for (Vec3 p: points) {
            logger.fine(p.toString() + "   " + angleFromLast(nearestOnCircle(p)));
        }
    }

    /** Returns the normalized points to which the great circle was fitted.
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

    /** Returns the normalized final point used in the great-circle fit.
     * @return the normalized final point used in the great-circle fit
     */
    public Vec3 lastPoint() {
        return points.get(points.size()-1);
    }

    /** Returns the angle between the supplied direction and the last point
     * on the great-circle fit. A negative value indicates that the 
     * supplied direction is before the last great-circle point when 
     * travelling along the great circle in the direction in which the
     * original points were provided. A positive value indicates that
     * the supplied direction is beyond the final great-circle point.
     * 
     * @param v a direction
     * @return the angle between the supplied direction and the last point
     * on the great-circle fit
     */
    public double angleFromLast(Vec3 v) {
        return nearestOnCircle(lastPoint()).angleTo(v)
                * pointTrend;
    }
    
    private String fmt(double d) {
        return String.format(Locale.ENGLISH, "%.1f", d);
    }
    
    /** Returns the statistical parameters as a list of strings.
     * The order of the parameters is the same as the order of
     * the headers provided by {@link #getHeaders()}.
     * @return the statistical parameters as a list of strings
     */
    public List<String> toStrings() {
        return Arrays.asList(fmt(pole.getDecDeg()), fmt(pole.getIncDeg()),
                Integer.toString(points.size()));
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

    public double getMad1() {
        return mad1;
    }
}
