/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * This class calculates and stores the median destructive field (MDF) for 
 * a set of {@link TreatmentStep} objects. This is the AF field required to
 * reduce the magnetic moment of a sample to half of its original
 * value. Despite the name of the class, it will calculate a 
 * ‘median destructive’ value for any quantifiable treatment type, so
 * it can also provide a ‘median destructive temperature’.
 * <p>
 * It is unlikely that a particular step will reduce the sample's
 * magnetic moment to <i>precisely</i> half the original value, so
 * the MDF value is calculated by linear interpolation between the
 * two closest data points on either side of the halfway line.
 * <p>
 * The MDF is not guaranteed to be well-defined for all samples,
 * since some samples may never reach half the original intensity.
 * The intensity of others may fluctuate and cross the midpoint 
 * multiple times; in this case only this first crossing is considered.
 * 
 * @author pont
 */
public class MedianDestructiveField {
    
    private final double demagLevel;
    private final double intensity;
    private final boolean halfIntReached;
    private static final List<String> HEADERS =
        Arrays.asList("MDF half-intensity (A/m)",
            "MDF demagnetization (degC or T)", "MDF midpoint reached");

    private MedianDestructiveField(double demagLevel, double intensity,
            boolean halfIntReached) {
        this.demagLevel = demagLevel;
        this.intensity = intensity;
        this.halfIntReached = halfIntReached;
    }

    private static double interpolate(double x0, double x1,
            double y0, double y1, double y) {
        return x0 + (x1 - x0) * (y1 - y) / (y1 - y0);
    }

    /**
     * Calculate and create a median destructive field (or temperature) value
     * for the supplied data. If there are fewer than two treatment steps in the
     * supplied list, {@code null} will be returned.
     *
     * @param steps a list of data representing successive demagnetization steps
     * for a single sample
     * @return the treatment level at which the sample retains half of its
     * original magnetic moment
     */
    public static MedianDestructiveField calculate(List<TreatmentStep> steps) {
        if (steps.size() < 2) {
            return null;
        }
        final double initialIntensity = steps.get(0).getIntensity();
        final double halfIntensity = initialIntensity / 2;
        boolean halfIntReached = false;
        int i = 1;
        TreatmentStep step;
        do {
            step = steps.get(i);
            i++;
        } while (i < steps.size() && step.getIntensity() > halfIntensity);
        if (step.getIntensity() <= halfIntensity) {
            halfIntReached = true;
        }
        // i can't be <=1 at this point, so we're safe doing get(i-2)
        final TreatmentStep previousStep = steps.get(i-2);

        /*
         * If the half-intensity was never reached, the MDF is undefined.
         * We set it to 0 as a marker value, because that's an impossible value
         * for a real MDF.
         */
        final double demagLevel = halfIntReached
                ? interpolate(previousStep.getTreatmentLevel(),
                        step.getTreatmentLevel(), step.getIntensity(),
                        previousStep.getIntensity(), halfIntensity)
                : 0;
        return new MedianDestructiveField(demagLevel, halfIntensity,
                halfIntReached);
    }

    /**
     * Returns the treatment level at which the intensity of the sample's
     * magnetic moment reached half of its initial value. If this did not
     * happen, this method may return an arbitrary value.
     *
     * @return the treatment level at which the intensity of the sample's
     * magnetic moment reached half of its initial value, if possible
     */
    public double getDemagLevel() {
        return demagLevel;
    }

    /**
     * Returns half of the intensity of the sample's initial magnetic moment.
     *
     * @return half of the intensity of the sample's initial magnetic moment
     */
    public double getIntensity() {
        return intensity;
    }

    /**
     * Reports whether this sample reached half its initial intensity.
     *
     * @return {@code true} if this sample reached half its initial intensity
     */
    public boolean isHalfIntReached() {
        return halfIntReached;
    }

    /**
     * Returns the headers describing the parameters as a list of strings.
     *
     * @return the headers describing the parameters
     */
    public static List<String> getHeaders() {
        return HEADERS;
    }

    /**
     * Returns a list of empty strings equal in length to the number of
     * parameters.
     *
     * @return a list of empty strings equal in length to the number of
     * parameters
     */
    public static List<String> getEmptyFields() {
        return Collections.nCopies(HEADERS.size(), "");
    }
    
    /**
     * Returns the parameters as a list of strings. The order of the parameters
     * is the same as the order of the headers provided by
     * {@link #getHeaders()}.
     *
     * @return the parameters as a list of strings
     */
    public List<String> toStrings() {
        return Arrays.asList(
                String.format(Locale.ENGLISH, "%.3g", getIntensity()),
                String.format(Locale.ENGLISH, "%.3g", getDemagLevel()),
                isHalfIntReached() ? "Y" : "N");
    }
}
