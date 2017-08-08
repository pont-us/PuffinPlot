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
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * A collection of RPI estimates.
 * 
 * @author pont
 */
public class RpiDataset {
    
    private static final Logger LOGGER = Logger.getLogger("net.talvi.puffinplot");
    private final double[] treatmentLevels;
    private final List<RpiDatum> rpis;
    
    public static class RpiDatum {
        
        private final Sample nrmSample;
        private final Sample armSample;
        private final List<Double> intensities;
        private final double meanRatio;
        private final double slope;
        private final double r;
        private final double rSquared;
        
        private RpiDatum(List<Double> intensities,
                Sample nrmSample, Sample armSample,
                double meanRatio, double slope, double r, double rSquared) {
            this.nrmSample = nrmSample;
            this.armSample = armSample;
            this.intensities = intensities;
            this.meanRatio = meanRatio;
            this.slope = slope;
            this.r = r;
            this.rSquared = rSquared;
        }
    }
    
    private RpiDataset(double[] treatmentLevels, List<RpiDatum> rpis) {
        this.treatmentLevels = treatmentLevels;
        this.rpis = rpis;
    }
    
    public void writeToFile(String path) {
        final File outFile = new File(path);
        FileWriter fw = null;
        try {
            fw = new FileWriter(outFile);
            
            fw.write("Depth");
            for (double level: treatmentLevels) {
                fw.write(String.format(Locale.ENGLISH, ",%g", level));
            }
            fw.write(", mean ratio, slope, r, r-squared\n");
            
            for (RpiDatum rpi: rpis) {
                fw.write(rpi.nrmSample.getData().get(0).getDepth());
                
                for (double intensity: rpi.intensities) {
                    if (intensity != -1) {
                    fw.write(String.format(Locale.ENGLISH,
                            ",%g", intensity));
                    } else {
                        fw.write(",");
                    }
                }
                fw.write(String.format(Locale.ENGLISH, ",%g,%g,%g,%g",
                        rpi.meanRatio,
                        rpi.slope,
                        rpi.r,
                        rpi.rSquared));
                fw.write("\n");
                
            }
            
            
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,
                    "calculateRpi: exception writing file.", e);
        } finally {
            try {
                if (fw != null) fw.close();
            } catch (IOException e2) {
                LOGGER.log(Level.WARNING,
                        "calculateRpi: exception closing file.", e2);
            }
        }
    }
    
    public static RpiDataset calculateWithArm(Suite nrmSuite, Suite armSuite,
            String outputPath) {
        final double[] treatmentLevels =
                nrmSuite.getSamples().get(0).getTreatmentLevels();

        List<RpiDatum> rpis = new ArrayList<>(); // TODO pick a sensible size
        for (Sample nrmSample: nrmSuite.getSamples()) {
            final String depth = nrmSample.getData().get(0).getDepth();
            final Sample armSample = armSuite.getSampleByName(depth);
            if (armSample != null) {
           
                final List<Double> nrmInts = new ArrayList<>(treatmentLevels.length);
                final List<Double> armInts = new ArrayList<>(treatmentLevels.length);
                final List<Double> ints = new ArrayList<>(treatmentLevels.length);
                for (double demagStep: treatmentLevels) {
                    final Datum nrmStep = nrmSample.getDatumByTreatmentLevel(demagStep);
                    // We have to treat the first ARM step as a special case,
                    // since its treatment level will correspond to the ARM AF
                    // field but we're actually interested in its AF demag
                    // step, which is 0. We assume that this will just be the first
                    // datum and fetch it by index.
                    final Datum armStep = demagStep == 0 ?
                            armSample.getDatum(0) :
                            armSample.getDatumByTreatmentLevel(demagStep);
                    if (nrmStep != null && armStep != null) {
                        final double nrmInt = nrmStep.getIntensity();
                        final double armInt = armStep.getIntensity();
                        ints.add(nrmInt/armInt);
                        //fw.write(String.format(Locale.ENGLISH,
                        //        ",%g", nrmInt/armInt));
                        nrmInts.add(nrmInt);
                        armInts.add(armInt);
                    } else {
                        ints.add(-1.);
                        // fw.write(",");
                    }
                }
                double totalRatio = 0;
                final SimpleRegression regression = new SimpleRegression();
                final int nPairs = nrmInts.size();
                for (int i=0; i<nPairs; i++) {
                    totalRatio += nrmInts.get(i) / armInts.get(i);
                    regression.addData(armInts.get(i), nrmInts.get(i));
                }
                rpis.add(new RpiDatum(ints, nrmSample, armSample, totalRatio/nPairs,
                regression.getSlope(),
                        regression.getR(),
                        regression.getRSquare()));
//                fw.write(String.format(Locale.ENGLISH, ",%g,%g,%g,%g",
//                        totalRatio/nPairs,
//                        regression.getSlope(),
//                        regression.getR(),
//                        regression.getRSquare()));
//                fw.write("\n");
            }
        }
        
        
        return new RpiDataset(treatmentLevels, rpis);
    }
    
}
