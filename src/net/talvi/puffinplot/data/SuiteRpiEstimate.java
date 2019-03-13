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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * A collection of RPI estimates.
 *
 * Instances of this class contain a list of the treatment levels used for the
 * RPI calculation (if applicable), and a list of SampleRpiEstimate instances,
 * each containing the RPI estimate(s) for a single sample.
 *
 * @param <EstimateType>
 */
public class SuiteRpiEstimate<EstimateType extends SampleRpiEstimate> {
    
    private static final Logger LOGGER =
            Logger.getLogger("net.talvi.puffinplot");
    private final List<Double> treatmentLevels;
    private final List<EstimateType> rpis;
        
    private SuiteRpiEstimate(List<Double> treatmentLevels,
            List<EstimateType> rpis) {
        this.treatmentLevels = treatmentLevels;
        this.rpis = rpis;
    }
    
    public void writeToFile(String path) throws IOException {
        final File outFile = new File(path);
        try (FileWriter writer = new FileWriter(outFile)) {
            writer.write("Depth,");
            for (double level: getTreatmentLevels()) {
                writer.write(String.format(Locale.ENGLISH, "%g,", level));
            }
            writer.write(getRpis().get(0).getCommaSeparatedHeader() + "\n");
            for (SampleRpiEstimate rpi: getRpis()) {
                writer.write(rpi.toCommaSeparatedString());
            }
        }
    }
    
    /**
     * Estimate RPI by normalizing NRM to MS.
     * 
     * @param nrmSuite suite containing NRM data
     * @param msSuite suite containing magnetic susceptibility normalizer data
     * @return a collection of RPI estimates
     */
    public static SuiteRpiEstimate<MagSusSampleRpiEstimate>
        calculateWithMagSus(Suite nrmSuite, Suite msSuite) {
        final List<MagSusSampleRpiEstimate> rpis =
                new ArrayList<>(nrmSuite.getNumSamples());
        for (Sample nrmSample: nrmSuite.getSamples()) {
            final double nrm = nrmSample.getNrm();
            final String depth =
                    nrmSample.getTreatmentSteps().get(0).getDepth();
            final Sample msSample = msSuite.getSampleByName(depth);
            if (msSample != null) {
                final double ms =
                        msSample.getTreatmentSteps().get(0).getMagSus();
                final double rpi = nrm / ms;
                rpis.add(new MagSusSampleRpiEstimate(nrmSample, msSample, rpi));
            }
        }
        return new SuiteRpiEstimate<>(Collections.emptyList(), rpis);
    }
    
    /**
     * Estimate RPI by normalizing a stepwise demagnetized NRM to another
     * stepwise demagnetized remanence (ARM or IRM).
     * 
     * @param nrmSuite suite containing NRM data
     * @param normalizerSuite suite containing ARM normalizer data
     * @param minLevel lowest AF treatment level to consider
     * @param maxLevel highest AF treatment level to consider
     * @return a collection of RPI estimates
     */
    public static SuiteRpiEstimate<StepwiseSampleRpiEstimate>
        calculateWithStepwiseAF(Suite nrmSuite,
            Suite normalizerSuite, double minLevel, double maxLevel) {
        final double[] allTreatmentLevels =
                nrmSuite.getSamples().get(0).getTreatmentLevels();
        
        final List<Double> treatmentLevels =
                Arrays.stream(allTreatmentLevels).boxed().
                filter(x -> x >= minLevel-1e-10 && x <= maxLevel+1e-10).
                collect(Collectors.toList());
        final int nLevels = treatmentLevels.size();
        
        /*
         * We have to get steps by treatment type as well as level, to avoid
         * accidentally retrieving the initial remanence-application step when
         * looking for a demag step with the same AF level. We need both the
         * DEGAUSS_Z and DEGAUSS_XYZ types here, since both are routinely used
         * for demagnetization.
         *
         * With Java 9, the ugly initialization below will be replaceable with a
         * simple Set.of(TreatmentType.DEGAUSS_XYZ, TreatmentType.DEGAUSS_Z).
         */
        final Set<TreatmentType> demagTreatmentTypes =
                Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                        TreatmentType.DEGAUSS_XYZ, TreatmentType.DEGAUSS_Z)));
        
        final List<StepwiseSampleRpiEstimate> rpis =
                new ArrayList<>(nrmSuite.getNumSamples());
        for (Sample nrmSample: nrmSuite.getSamples()) {
            final String depth =
                    nrmSample.getTreatmentSteps().get(0).getDepth();
            final Sample normalizerSample =
                    normalizerSuite.getSampleByName(depth);
            if (normalizerSample != null) {
                final List<Double> nrmIntensities = new ArrayList<>(nLevels);
                final List<Double> normalizerIntensities =
                        new ArrayList<>(nLevels);
                final List<Double> ratios = new ArrayList<>(nLevels);
                
                for (double demagStep: treatmentLevels) {
                    final TreatmentStep nrmStep =
                            nrmSample.getTreatmentStepByLevel(demagStep);
                    /*
                     * We have to treat the first step as a special case, since
                     * it's where the remanence is imparted so its treatment
                     * level will correspond to the ARM or IRM field but we're
                     * actually interested in its AF demag step, which is 0. We
                     * assume that this will just be the first datum and fetch
                     * it by index.
                     */
                    final TreatmentStep normalizerStep = demagStep == 0 ?
                            normalizerSample.getTreatmentStepByIndex(0) :
                            normalizerSample.getTreatmentStepByTypeAndLevel(
                                    demagTreatmentTypes, demagStep);
                    if (nrmStep != null && normalizerStep != null) {
                        final double nrmInt = nrmStep.getIntensity();
                        final double armInt = normalizerStep.getIntensity();
                        ratios.add(nrmInt / armInt);
                        nrmIntensities.add(nrmInt);
                        normalizerIntensities.add(armInt);
                    } else {
                        ratios.add(-1.); // code for "leave blank"
                    }
                }
                double totalRatio = 0;
                final SimpleRegression regression = new SimpleRegression();
                final int nPairs = nrmIntensities.size();
                for (int i = 0; i < nPairs; i++) {
                    totalRatio += nrmIntensities.get(i)
                            / normalizerIntensities.get(i);
                    regression.addData(normalizerIntensities.get(i),
                            nrmIntensities.get(i));
                }
                rpis.add(new StepwiseSampleRpiEstimate(ratios,
                        nrmSample, normalizerSample,
                        totalRatio / nPairs,
                        regression.getSlope(),
                        regression.getR(),
                        regression.getRSquare()));
            }
        }
        return new SuiteRpiEstimate<>(treatmentLevels, rpis);
    }

    /**
     * @return the treatment levels
     */
    public List<Double> getTreatmentLevels() {
        return treatmentLevels;
    }

    /**
     * @return the RPIs
     */
    public List<EstimateType> getRpis() {
        return rpis;
    }
}
