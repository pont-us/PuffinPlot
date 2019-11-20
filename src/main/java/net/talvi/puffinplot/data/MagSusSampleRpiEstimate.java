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

import java.util.Locale;

/**
 * An estimate of relative palaeointensity for a single sample,
 * normalized by magnetic susceptibility.
 */
public class MagSusSampleRpiEstimate implements SampleRpiEstimate {
    
    private final Sample nrmSample;
    private final Sample normalizer;
    private final double ratio;

    MagSusSampleRpiEstimate(Sample nrmSample,
            Sample magSusSample, double ratio) {
        this.nrmSample = nrmSample;
        this.normalizer = magSusSample;
        this.ratio = ratio;
    }

    /**
     * @return the sample giving the NRM intensity
     */
    @Override
    public Sample getNrmSample() {
        return nrmSample;
    }

    /**
     * @return the sample used for MS intensity normalization
     */
    public Sample getNormalizer() {
        return normalizer;
    }

    /**
     * @return the ratio of the NRM to the magnetic susceptibility
     */
    public double getRatio() {
        return ratio;
    }

    @Override
    public String toCommaSeparatedString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getNrmSample().getTreatmentSteps().get(0).getDepth());

        final TreatmentStep normalizerTreatmentStep =
                normalizer.getTreatmentStepByIndex(0);
        final double normalizerMagSus = normalizerTreatmentStep.getMagSus();
        builder.append(String.format(Locale.ENGLISH, ",%g,%g",
                getRatio(), normalizerMagSus));
        builder.append("\n");
        return builder.toString();
    }

    @Override
    public String getCommaSeparatedHeader() {
        return "ratio,MS";
    }

    @Override
    public double getEstimate() {
        return getEstimate();
    }
    
}
