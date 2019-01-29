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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot. If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data;

import java.util.List;
import java.util.Locale;

/**
 * An estimate of relative palaeointensity for a single sample, normalized
 * by ARM.
 */
public class ArmSampleRpiEstimate implements SampleRpiEstimate {
    
    private final Sample nrmSample;
    private final Sample normalizer;
    private final List<Double> intensities;
    private final double meanRatio;
    private final double slope;
    private final double r;
    private final double rSquared;

    ArmSampleRpiEstimate(List<Double> intensities, Sample nrmSample,
            Sample armSample, double meanRatio,
            double slope, double r, double rSquared) {
        this.nrmSample = nrmSample;
        this.normalizer = armSample;
        this.intensities = intensities;
        this.meanRatio = meanRatio;
        this.slope = slope;
        this.r = r;
        this.rSquared = rSquared;
    }

    /**
     * @return the sample giving the NRM intensity
     */
    public Sample getNrmSample() {
        return nrmSample;
    }

    /**
     * @return the sample used for ARM intensity normalization
     */
    public Sample getNormalizer() {
        return normalizer;
    }

    /**
     * @return the intensities
     */
    public List<Double> getIntensities() {
        return intensities;
    }

    /**
     * @return the mean of the NRM/ARM intensity ratios
     */
    public double getMeanRatio() {
        return meanRatio;
    }

    /**
     * @return the slope of the linear regression
     */
    public double getSlope() {
        return slope;
    }

    /**
     * @return the r-value for the linear regression
     */
    public double getR() {
        return r;
    }

    /**
     * @return the r-squared value for the linear regression
     */
    public double getrSquared() {
        return rSquared;
    }

    @Override
    public String toCommaSeparatedString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getNrmSample().getTreatmentSteps().get(0).getDepth());
        for (double intensity : getIntensities()) {
            if (intensity != -1) {
                builder.append(String.format(Locale.ENGLISH, ",%g", intensity));
            } else {
                builder.append(",");
            }
        }
        builder.append(String.format(Locale.ENGLISH, ",%g,%g,%g,%g,%g",
                getMeanRatio(), getSlope(), getR(), getrSquared(),
                normalizer.getTreatmentStepByIndex(0).getIntensity()));
        builder.append("\n");
        return builder.toString();
    }

    @Override
    public String getCommaSeparatedHeader() {
        return "mean ratio,slope,r,r-squared,ARM";
    }
    
}
