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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
    private final int N;
    private final double R;
    private final Vec3 meanDirection;
    private final List<Vec3> directions;
    private static final List<String> HEADERS =
        Arrays.asList("Fisher dec. (deg)", "Fisher inc. (deg)",
            "Fisher a95 (deg)", "Fisher k", "Fisher nDirs", "Fisher R");

    private FisherValues(List<Vec3> directions, double a95, double k,
            double R, Vec3 meanDirection) {
        this.directions = Collections.unmodifiableList(directions);
        this.a95 = a95;
        this.k = k;
        this.N = directions.size();
        this.R = R;
        this.meanDirection = meanDirection;
    }
    
    /**
     * Returns a set of Fisherian statistics, calculated using the 
     * Fisher (1953) method, for a collection of vectors. The vectors
     * do not need to be normalized; since Fisherian statistics are
     * purely directional, their magnitudes will not influence the result.
     * Passing an empty collection of vectors will return a null
     * FisherValues object. Passing a singleton collection will return
     * a FisherValues object with the direction of the single vector
     * as the mean, but the a95 and k values are entirely undefined; they may
     * be NaN or infinity.
     * 
     * The a95 value is not guaranteed to represent a real number:
     * some sets of directions may produce an undefined (NaN or
     * infinite) a95. This is a consequence of the definition, rather than
     * of the implementation here; see equation 6.21 in Butler (1992)
     * and note that R may be arbitrarily small, putting the RHS
     * out of the domain of the cosine function. (Theoretically R may
     * even be zero, but this is extremely unlikely for any real data set.)
     * 
     * @param vectors the points on which to calculate statistics
     * @return the Fisherian statistics for the supplied vectors
     */
    public static FisherValues calculate(Collection<Vec3> vectors) {
        if (vectors.isEmpty()) {
            // TODO Would probably be better to return a null *object*
            // rather than a null reference here.
            return null;
        }
        
        final List<Vec3> normPoints = new ArrayList<>(vectors.size());
        final double N = vectors.size();
        for (Vec3 point: vectors) {
            // TODO handle point = (0, 0, 0) case -- should never happen
            // with real data but currently results in NaNs for normPoints
            // so needs to be handled explicitly if it ever does occur.
            normPoints.add(point.normalize());
        }
        final double p = 0.05; // significance level, so 0.05 for 95%
        final double R = Vec3.sum(normPoints).mag(); // vector sum length
        // TODO Handle N=R case -- should probably give k = +infinity,
        // a95 = 0 (but existing calculation should already produce a95=0).
        final double k = (N-1)/(N-R);
        final double cosOfA95 = 1 - ((N-R)/R) * (pow(1/p,1/(N-1))-1);
        
        /* cosOfA95 is *not* guaranteed to be in the range of cos,
         * so it's possible to get a NaN here. It would of course be
         * possible to trap this case and return (e.g.) 180, but NaN
         * seems like the clearest way to indicate "no valid a95 for
         * this data set".
         */
        final double a95 = Math.toDegrees(acos(cosOfA95));

        return new FisherValues(normPoints, a95, k, R,
                Vec3.meanDirection(normPoints));
    }
    
    @Override
    public double getA95() {
        return a95;
    }
    
    @Override
    public boolean isA95Valid() {
        return Double.isFinite(a95);
    }

    @Override
    public double getK() {
        return k;
    }
    
    @Override
    public int getN() {
        return N;
    }
    
    @Override
    public double getR() {
        return R;
    }

    @Override
    public Vec3 getMeanDirection() {
        return meanDirection;
    }
    
    /** Returns a list of unit vectors representing
     * the directions of the vectors on which these statistics were
     * calculated. 
     * 
     * @return the directions of the vectors on which these statistics
     * were calculated */
    public List<Vec3> getDirections() {
        return directions;
    }

    private String fmt(double d) {
        return String.format(Locale.ENGLISH, "%.4f", d);
    }

    /** Returns the statistical parameters as a list of strings.
     * The order of the parameters is the same as the order of
     * the headers provided by {@link #getHeaders()}.
     * @return the statistical parameters as a list of strings
     * @see #toStrings(net.talvi.puffinplot.data.FisherValues) 
     */
    public List<String> toStrings() {
        return Arrays.asList(fmt(getMeanDirection().getDecDeg()),
                fmt(getMeanDirection().getIncDeg()), fmt(getA95()),
                fmt(getK()), Integer.toString(getN()),
                fmt(getR()));
    }
    
    /** Returns the statistical parameters as a list of strings.
     * The order of the parameters is the same as the order of
     * the headers provided by {@link #getHeaders()}.
     * It returns the same result as the corresponding instance method
     * for any non-null input, but (unlike the instance method)
     * also works for a {@code null} {@code FisherValues}.
     * @param fisherValues the Fisher parameters to return as strings
     * @return the statistical parameters as a list of strings,
     * or a list of empty strings if {@code fisherValues} was {@code null}
     * @see #toStrings() 
     */
    public static List<String> toStrings(FisherValues fisherValues) {
        if (fisherValues != null) {
            return fisherValues.toStrings();
        } else {
            return getEmptyFields();
        }
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
