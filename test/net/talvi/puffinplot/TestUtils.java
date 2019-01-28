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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot. If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.talvi.puffinplot.data.*;
import net.talvi.puffinplot.data.TreatmentStep;
import org.junit.rules.TemporaryFolder;

/**
 * Utility methods used by unit tests.
 */
public class TestUtils {

    /**
     * Creates a simple sample with three demagnetization steps aligned
     * in the specified direction, so that a PCA on the sample will produce
     * that direction. This method also performs PCA on the sample before
     * returning it, setting the sample direction.
     * 
     * @param depth depth of sample
     * @param discreteId discrete ID of sample
     * @param direction direction of data to be created
     * 
     * @return 
     */
    public static Sample makeOneComponentSample(double depth, String discreteId,
            Vec3 direction) {
        final String depthString = String.format("%f", depth);
        final Sample sample = new Sample(depthString, null);
        for (int j = 3; j > 0; j--) {
            final TreatmentStep d = new TreatmentStep(direction.times(j));
            d.setMeasType(MeasType.CONTINUOUS);
            d.setInPca(true);
            sample.addDatum(d);
        }
        sample.setDiscreteId(discreteId);
        sample.doPca(Correction.NONE);
        return sample;
    }

    public static List<Sample> makeUniformSampleList(Vec3 direction, double[] depths, String discreteId) {
        final List<Sample> samples = new ArrayList<>(10);
        for (int i = 0; i < depths.length; i++) {
            Sample sample = TestUtils.makeOneComponentSample(depths[i], discreteId, direction);
            samples.add(sample);
        }
        return samples;
    }

    public static Suite createDiscreteSuite() {
        final Suite suite = new Suite("SuiteTest");
        for (int sampleIndex = 0; sampleIndex < 10; sampleIndex++) {
            final String sampleName = String.format("SAMPLE_%d", sampleIndex);
            final Sample sample = new Sample(sampleName, suite);
            for (int demag = 0; demag < 100; demag += 10) {
                final TreatmentStep d = new TreatmentStep((sampleIndex + 1.0) * (100.0 - demag), sampleIndex * 50, demag);
                d.setDiscreteId(sampleName);
                d.setSuite(suite);
                d.setMeasType(MeasType.DISCRETE);
                d.setAfX(demag);
                d.setAfY(demag);
                d.setAfZ(demag);
                d.setTreatType(TreatType.DEGAUSS_XYZ);
                d.setSample(sample);
                d.setMagSus(sampleIndex);
                d.setSampAz(0);
                d.setSampDip(0);
                d.setFormAz(0);
                d.setFormDip(0);
                sample.addDatum(d);
                suite.addDatum(d);
            }
        }
        suite.updateReverseIndex();
        return suite;
    }

    public static Suite createContinuousSuite() {
        final Suite suite = new Suite("SuiteTest");
        for (int depth = 0; depth < 10; depth++) {
            final String depthString = String.format("%d", depth);
            final Sample sample = new Sample(depthString, suite);
            for (int demag = 0; demag < 100; demag += 10) {
                final TreatmentStep d = new TreatmentStep((depth + 1.0) * (100.0 - demag),
                        depth * 50, demag);
                d.setDepth(depthString);
                d.setSuite(suite);
                d.setMeasType(MeasType.CONTINUOUS);
                d.setAfX(demag);
                d.setAfY(demag);
                d.setAfZ(demag);
                d.setTreatType(TreatType.DEGAUSS_XYZ);
                d.setSample(sample);
                d.setMagSus(depth);
                sample.addDatum(d);
                suite.addDatum(d);
            }
        }
        suite.updateReverseIndex();
        return suite;
    }
    
    public static class ListHandler extends Handler {

        public final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override public void flush() {}

        @Override public void close() throws SecurityException {}
        
        public boolean wasOneMessageLogged(Level level) {
            return records.size() == 1 &&
                    records.get(0).getLevel() == level;
        }
        
        public static ListHandler createAndAdd() {
            final ListHandler handler = new ListHandler();
            Logger.getLogger("net.talvi.puffinplot").addHandler(handler);
            return handler;
        }
    }
    
    public static Vec3 randomVector(Random rnd, double max) {
        return new Vec3(rnd.nextDouble()*2*max-max,
                        rnd.nextDouble()*2*max-max,
                        rnd.nextDouble()*2*max-max);
    }

    public static boolean isPrintableAscii(String string) {
        return string.chars().allMatch((int c) -> c >= 20 && c < 127);
    }
    
    public static boolean equalOrOpposite(Vec3 v0, Vec3 v1, double delta) {
        return v0.equals(v1, delta) || v0.invert().equals(v1, delta);
    }
    
    public static List<Vec3> makeVectorList(double[][] components,
            boolean normalize) {
        final List<Vec3> result = new ArrayList<Vec3>(components.length);
        for (double[] triplet: components) {
            assert(triplet.length == 3);
            final Vec3 v = new Vec3(triplet[0], triplet[1], triplet[2]);
            result.add(normalize ? v.normalize() : v);
        }
        return result;
    }
    
    public static File writeStringToTemporaryFile(String fileName,
            String fileContents,
            TemporaryFolder temporaryFolder) throws IOException {
        final Path filePath = temporaryFolder.getRoot().toPath().
                resolve(fileName);
        Files.write(filePath, fileContents.getBytes(),
                StandardOpenOption.CREATE);
        return filePath.toFile();
    }

}
