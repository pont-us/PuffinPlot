package net.talvi.puffinplot.data;

import java.util.Locale;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import static java.lang.Math.acos;
import static java.lang.Math.pow;

/**
 * This class calculates Fisher (1953) spherical statistics on sets of vectors.
 * 
 * @author pont
 */
public class FisherValues implements FisherParams {

    private final double a95;
    private final double k;
    private final Vec3 meanDirection;
    private final List<Vec3> directions;
    private final static double p = 0.05;
    private static final List<String> HEADERS =
        Arrays.asList("Fisher dec. (°)", "Fisher inc. (°)",
            "Fisher a95 (°)", "Fisher k", "Fisher nDirs");

    private FisherValues(List<Vec3> directions, double a95, double k, Vec3 meanDirection) {
        this.directions = Collections.unmodifiableList(directions);
        this.a95 = a95;
        this.k = k;
        this.meanDirection = meanDirection;
    }
    
    /**
     * Returns a set of Fisherian statistics, calculated using the 
     * Fisher (1953) method, for a collection of vectors. The vectors
     * do not need to be normalized; since Fisherian statistics are
     * purely directional, their magnitudes will not influence the result.
     * 
     * @param vectors the points on which to calculate statistics
     * @return the Fisherian statistics for the supplied vectors
     */
    public static FisherValues calculate(Collection<Vec3> vectors) {
        List<Vec3> normPoints = new ArrayList<Vec3>(vectors.size());
        double N = vectors.size();
        for (Vec3 point: vectors) normPoints.add(point.normalize());
        // R is the vector sum length
        double R = Vec3.sum(normPoints).mag();
        double k = (N-1)/(N-R);
        double a95 = Math.toDegrees(acos( 1 - ((N-R)/R) * (pow(1/p,1/(N-1))-1) ));
        return new FisherValues(normPoints, a95, k, Vec3.meanDirection(normPoints));
    }
    
    public double getA95() {
        return a95;
    }

    public double getK() {
        return k;
    }
    
    public int getNDirs() {
        return directions.size();
    }

    public Vec3 getMeanDirection() {
        return meanDirection;
    }
    
    /** Returns the directions of the vectors on which these statistics were calculated. 
     * @return the directions of the vectors on which these statistics were calculated */
    public List<Vec3> getDirections() {
        return directions;
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
        return Arrays.asList(fmt(getMeanDirection().getDecDeg()),
                fmt(getMeanDirection().getIncDeg()), fmt(getA95()),
                fmt(getK()), Integer.toString(getNDirs()));
    }

    /** Returns a string representation of the parameters.
     *  @return a string representation of the parameters */
    @Override
    public String toString() {
        List<String> values = toStrings();
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (int i=0; i<values.size(); i++) {
            if (!first) result.append(" / ");
            result.append(getHeaders().get(i).replace("Fisher ", "")).
                    append(" ").append(values.get(i));
            first = false;
        }
        return result.toString();
    }

    /** Returns the headers describing the parameters as a list of strings.
     * @return the headers describing the parameters
     */
    public static List<String> getHeaders() {
        return HEADERS;
    }

    /** Returns a list of empty strings equal in length to the number of parameters.
     * @return  a list of empty strings equal in length to the number of parameters
     */
    public static List<String> getEmptyFields() {
        return Collections.nCopies(HEADERS.size(), "");
    }
}
