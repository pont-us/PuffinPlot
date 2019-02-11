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
package net.talvi.puffinplot;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasurementType;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.plots.Plot;
import net.talvi.puffinplot.plots.testdata.TestFileLocator;
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
            d.setMeasurementType(MeasurementType.CONTINUOUS);
            d.setInPca(true);
            sample.addTreatmentStep(d);
        }
        sample.setDiscreteId(discreteId);
        sample.doPca(Correction.NONE);
        return sample;
    }

    public static List<Sample> makeUniformSampleList(Vec3 direction,
            double[] depths, String discreteId) {
        final List<Sample> samples = new ArrayList<>(10);
        for (int i = 0; i < depths.length; i++) {
            final Sample sample = makeOneComponentSample(depths[i],
                    discreteId, direction);
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
                final TreatmentStep step =new TreatmentStep(
                        (sampleIndex + 1.0) * (100.0 - demag),
                        sampleIndex * 50, demag);
                step.setDiscreteId(sampleName);
                step.setSuite(suite);
                step.setMeasurementType(MeasurementType.DISCRETE);
                step.setAfX(demag);
                step.setAfY(demag);
                step.setAfZ(demag);
                step.setTreatmentType(TreatmentType.DEGAUSS_XYZ);
                step.setSample(sample);
                step.setMagSus(sampleIndex);
                step.setSampAz(0);
                step.setSampDip(0);
                step.setFormAz(0);
                step.setFormDip(0);
                sample.addTreatmentStep(step);
                suite.addTreatmentStep(step);
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
                final TreatmentStep step = new TreatmentStep(
                        (depth + 1.0) * (100.0 - demag),
                        depth * 50, demag);
                step.setDepth(depthString);
                step.setSuite(suite);
                step.setMeasurementType(MeasurementType.CONTINUOUS);
                step.setAfX(demag);
                step.setAfY(demag);
                step.setAfZ(demag);
                step.setTreatmentType(TreatmentType.DEGAUSS_XYZ);
                step.setSample(sample);
                step.setMagSus(depth);
                sample.addTreatmentStep(step);
                suite.addTreatmentStep(step);
            }
        }
        suite.updateReverseIndex();
        return suite;
    }
    
    /**
     * Create a discrete suite of 15 samples with magnetic moment directions
     * arranged in an arc spanning the upper and lower hemispheres.
     * 
     * @return a discrete suite
     */
    public static Suite createDiscreteSuiteArc() {
        final Suite suite = new Suite("SuiteTest");
        final List<Vec3> directions = Vec3.spherInterpDir(
                new Vec3(-0.2, 0.2, 1),
                new Vec3(0.2, -0.2, -1),
                new Vec3(-1, -1, 0),
                0.25);
        for (int i=0; i<directions.size(); i++) {
            final String name = String.format("SAMPLE_%d", i);
            final Sample sample = new Sample(name, suite);
            for (int demag = 0; demag < 100; demag += 10) {
                final TreatmentStep step =
                        new TreatmentStep(directions.get(i).times(100.0 - demag));
                step.setDiscreteId(name);
                step.setSuite(suite);
                step.setMeasurementType(MeasurementType.DISCRETE);
                step.setAfX(demag);
                step.setAfY(demag);
                step.setAfZ(demag);
                step.setTreatmentType(TreatmentType.DEGAUSS_XYZ);
                step.setSample(sample);
                step.setSampAz(0);
                step.setSampDip(0);
                step.setFormAz(0);
                step.setFormDip(0);
                sample.addTreatmentStep(step);
                suite.addTreatmentStep(step);
            }
        }
        suite.updateReverseIndex();
        return suite;
        
    }
    
    public static void doPcaOnAllSamples(Suite suite, boolean anchored) {
        suite.getSamples().stream().
                flatMap(sample -> sample.getTreatmentSteps().stream()).
                forEach(step -> step.setInPca(true));
        suite.getSamples().forEach(sample -> {
            sample.setPcaAnchored(anchored);
            sample.doPca(Correction.NONE);
        });
    }

    public static boolean isImageCorrect(String expected, BufferedImage actual)
            throws IOException {
        final InputStream stream =
                TestFileLocator.class.getResourceAsStream(expected + ".png");
        final BufferedImage expectedImage = ImageIO.read(stream);
        return areImagesEqual(expectedImage, actual);
    }

    private static boolean areImagesEqual(BufferedImage image1,
            BufferedImage image2) {
        if (image1.getHeight() != image2.getHeight()) {
            return false;
        }
        if (image1.getWidth() != image2.getWidth()) {
            return false;
        }
        for (int y = 0; y < image1.getHeight(); y++) {
            for (int x = 0; x < image1.getWidth(); x++) {
                if (image1.getRGB(x, y) != image2.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * This is a convenience method for creating reference images for use
     * as expected values in characterization tests. It saves PNG images
     * to the user home directory.
     * 
     * @param image the image to save
     * @param filename the filename under which to save the image (without
     * extension)
     * @throws IOException if there was an error writing the image
     */
    public static void saveImage(BufferedImage image, String filename)
            throws IOException {
        final String home = System.getProperty("user.home");
        final Path path = Paths.get(home, filename + ".png");
        ImageIO.write(image, "PNG", path.toFile());
    }

    public static BufferedImage makeImage(Plot plot) {
        final BufferedImage actual = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = actual.createGraphics();
        plot.draw(graphics);
        return actual;
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
    
    public static <E> Set<E> setOf(E... items) {
        return Arrays.asList(items).stream().collect(Collectors.toSet());
    }

}
