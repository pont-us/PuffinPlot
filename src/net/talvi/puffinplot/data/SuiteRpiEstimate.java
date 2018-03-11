/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2017 Pontus Lurcock.
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * A collection of RPI estimates.
 *
 * Instances of this class contain a list of the treatment levels used for the
 * RPI calculation, and a list of SampleRpiEstimate instances, each containing
 * the RPI estimates for a single sample.
 *
 * @author pont
 */
public class SuiteRpiEstimate {
    
    private static final Logger LOGGER =
            Logger.getLogger("net.talvi.puffinplot");
    private final List<Double> treatmentLevels;
    private final List<SampleRpiEstimate> rpis;
        
    private SuiteRpiEstimate(List<Double> treatmentLevels,
            List<SampleRpiEstimate> rpis) {
        this.treatmentLevels = treatmentLevels;
        this.rpis = rpis;
    }
    
    public void writeToFile(String path) {
        final File outFile = new File(path);
        try (FileWriter writer = new FileWriter(outFile)) {
            writer.write("Depth");
            for (double level: getTreatmentLevels()) {
                writer.write(String.format(Locale.ENGLISH, ",%g", level));
            }
            writer.write(", mean ratio, slope, r, r-squared, ARM\n");
            
            for (SampleRpiEstimate rpi: getRpis()) {
                writer.write(rpi.toCommaSeparatedString());
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,
                    "calculateRpi: exception writing file.", e);
        }
    }
    
    /**
     * Currently a bit of a hack, shoehorning MS-calculated data into an
     * SampleRpiEstimate designed for ARM-calculated data. SampleRpiEstimate
     * needs to be redesigned to handle MS RPIs nicely.
     *
     * @param nrmSuite
     * @param msSuite
     * @return 
     */
    public static SuiteRpiEstimate calculateWithMagSus(Suite nrmSuite, Suite msSuite) {
        List<SampleRpiEstimate> rpis = new ArrayList<>(nrmSuite.getNumSamples());
        for (Sample nrmSample: nrmSuite.getSamples()) {
            final double nrm = nrmSample.getNrm();
            final String depth = nrmSample.getData().get(0).getDepth();
            final Sample msSample = msSuite.getSampleByName(depth);
            if (msSample == null) {
                throw new IllegalArgumentException("No MS sample for depth " +
                                                   depth);
            }
            final double ms = msSample.getData().get(0).getMagSus();
            final double rpi = nrm/ms;
            rpis.add(new SampleRpiEstimate(Collections.singletonList(rpi),
                        nrmSample, msSample,
                        rpi,
                        rpi,
                        0,
                        0));
        }
        return new SuiteRpiEstimate(Collections.singletonList(Double.valueOf(0)), rpis);
    }
    
    public static SuiteRpiEstimate calculateWithArm(Suite nrmSuite, Suite armSuite,
            double minLevel, double maxLevel) {
        final double[] allTreatmentLevels =
                nrmSuite.getSamples().get(0).getTreatmentLevels();
        
        final List<Double> treatmentLevels =
                Arrays.stream(allTreatmentLevels).boxed().
                filter(x -> x >= minLevel-1e-10 && x <= maxLevel+1e-10).
                collect(Collectors.toList());
        final int nLevels = treatmentLevels.size();
        
        /* We have to get steps by treatment type as well as level, to avoid
         * accidentally retrieving the initial ARM-application step when looking
         * for a demag step with the same AF level. We need both the DEGAUSS_Z
         * and DEGAUSS_XYZ types here, since both are routinely used for
         * ARM demagnetization.
         * 
         * With Java 9, the ugly initialization below will be replaceable with
         * a simple Set.of(TreatType.DEGAUSS_XYZ, TreatType.DEGAUSS_Z).
         */
        final Set<TreatType> demagTreatTypes =
                Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                        TreatType.DEGAUSS_XYZ, TreatType.DEGAUSS_Z)));
        
        final List<SampleRpiEstimate> rpis = new ArrayList<>(nrmSuite.getNumSamples());
        for (Sample nrmSample: nrmSuite.getSamples()) {
            final String depth = nrmSample.getData().get(0).getDepth();
            final Sample armSample = armSuite.getSampleByName(depth);
            if (armSample != null) {
                final List<Double> nrmInts = new ArrayList<>(nLevels);
                final List<Double> armInts = new ArrayList<>(nLevels);
                final List<Double> ratios = new ArrayList<>(nLevels);
                
                for (double demagStep: treatmentLevels) {
                    final Datum nrmStep =
                            nrmSample.getDatumByTreatmentLevel(demagStep);

                    // We have to treat the first ARM step as a special case,
                    // since its treatment level will correspond to the ARM AF
                    // field but we're actually interested in its AF demag
                    // step, which is 0. We assume that this will just be the
                    // first datum and fetch it by index. 
                            
                    final Datum armStep = demagStep == 0 ?
                            armSample.getDatum(0) :
                            armSample.getDatumByTreatmentTypeAndLevel(
                                    demagTreatTypes, demagStep);
                    if (nrmStep != null && armStep != null) {
                        final double nrmInt = nrmStep.getIntensity();
                        final double armInt = armStep.getIntensity();
                        ratios.add(nrmInt/armInt);
                        nrmInts.add(nrmInt);
                        armInts.add(armInt);
                    } else {
                        ratios.add(-1.); // code for "leave blank"
                    }
                }
                double totalRatio = 0;
                final SimpleRegression regression = new SimpleRegression();
                final int nPairs = nrmInts.size();
                for (int i=0; i<nPairs; i++) {
                    totalRatio += nrmInts.get(i) / armInts.get(i);
                    regression.addData(armInts.get(i), nrmInts.get(i));
                }
                rpis.add(new SampleRpiEstimate(ratios, nrmSample, armSample,
                        totalRatio/nPairs,
                        regression.getSlope(),
                        regression.getR(),
                        regression.getRSquare()));
            }
        }
        return new SuiteRpiEstimate(treatmentLevels, rpis);
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
    public List<SampleRpiEstimate> getRpis() {
        return rpis;
    }
}
