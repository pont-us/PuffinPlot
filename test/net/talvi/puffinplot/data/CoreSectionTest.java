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
package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.talvi.puffinplot.TestUtils;
import net.talvi.puffinplot.data.CoreSection.End;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author pont
 */
public class CoreSectionTest {

    private final double delta = 1e-10;
    
    private List<Sample> sampleList;
    private CoreSection section;
    
    @Before
    public void setUp() {
        sampleList = makeSampleList();
        section = CoreSection.fromSamples(sampleList);
    }
    
    static List<Sample> makeSampleList() {
        final List<Sample> samples = new ArrayList<>(10);
        for (int depth=0; depth<10; depth++) {
            final String depthString = String.format("%d", depth);
            final Sample sample = new Sample(depthString, null);
            for (int demag=0; demag<100; demag += 10) {
                final TreatmentStep d = new TreatmentStep((depth+1.)*(100.-demag),
                        depth*50, demag);
                d.setDepth(depthString);
                d.setMeasurementType(MeasurementType.CONTINUOUS);
                d.setAfX(demag);
                d.setAfY(demag);
                d.setAfZ(demag);
                d.setTreatmentType(TreatmentType.DEGAUSS_XYZ);
                d.setSample(sample);
                d.setMagSus(depth);
                d.setDiscreteId(String.format("%d", depth / 3));
                sample.addDatum(d);
            }
            samples.add(sample);
        }
        return samples;        
    }

    private static List<Sample> makeSampleListDirectionsOnly() {
        final List<Sample> samples = new ArrayList<>();
        for (int depth=0; depth<10; depth++) {
            final String depthString = String.format("%d", depth);
            final Sample sample = new Sample(depthString, null);
            sample.setDepth(depthString);
            sample.setImportedDirection(new Vec3(depth-4, 5-depth, depth-6));
            samples.add(sample);
        }
        return samples;
    }
    
    @Test(expected = NullPointerException.class)
    public void testFromSamplesWithNull() {
        CoreSection.fromSamples(null);
    }

    @Test
    public void testFromSamples() {
        final List<Sample> samples = new ArrayList<>();
        samples.add(new Sample("sample1", null));
        final CoreSection cs = CoreSection.fromSamples(samples);
        assertEquals(1, cs.getSamples().size());
        assertEquals("sample1", cs.getSamples().get(0).getNameOrDepth());
    }
    
    
    @Test
    public void testRotateDeclinations() {
        final List<Vec3> initialDirections = extractDatumDirections(section);
        final double angle = 35;
        section.rotateDeclinations(angle);
        final List<Vec3> rotatedDirections = extractDatumDirections(section);
        for (int i=0; i<initialDirections.size(); i++) {
            final double expectedDeclination = (initialDirections.
                    get(i).getDecDeg() + angle + 360) % 360;
            assertEquals(expectedDeclination,
                    rotatedDirections.get(i).getDecDeg(), 1e-10);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSamplesNearEndWithTooManySamples() {
        section.getSamplesNearEnd(CoreSection.End.TOP,
                section.getSamples().size() + 1);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testGetSamplesNearEndWithNegativeSamples() {
        section.getSamplesNearEnd(CoreSection.End.TOP, -1);
    }

    @Test
    public void testGetSamplesNearEndTop() {
        final int nSamples = 3;
        final List<Sample> actual =
                section.getSamplesNearEnd(CoreSection.End.TOP, nSamples);
        assertEquals(section.getSamples().subList(0, nSamples),
                actual);
    }
    
    @Test
    public void testGetSamplesNearEndBottom() {
        final int nSamples = 3;
        final List<Sample> actual =
                section.getSamplesNearEnd(CoreSection.End.BOTTOM, nSamples);
        assertEquals(section.getSamples().subList(sampleList.size() - nSamples,
                sampleList.size()), actual);
    }
    
    private static List<Vec3> extractDatumDirections(CoreSection section) {
        return section.getSamples().stream().flatMap(s -> s.getData().stream()).
                map(TreatmentStep::getMoment).collect(Collectors.toList());
    }
    
    @Test
    public void testGetDirectionNearEndTop() {
        final List<Sample> samples = makeSampleListDirectionsOnly();
        for (int nSamples = 1; nSamples < samples.size(); nSamples++) {
            final Vec3 expectedMeanDirection =
                    meanDirection(samples.subList(0, nSamples));
            final Vec3 actualMeanDirection = CoreSection.
                    fromSamples(samples).getDirectionNearEnd(
                            CoreSection.End.TOP, nSamples);
            assertTrue(expectedMeanDirection.
                    equals(actualMeanDirection, 1e-10));
        }
    }

    @Test
    public void testGetDirectionNearEndBottom() {
        final List<Sample> samples = makeSampleListDirectionsOnly();
        for (int nSamples = 1; nSamples < samples.size(); nSamples++) {
            final Vec3 expectedMeanDirection =
                    meanDirection(samples.subList(samples.size() - nSamples,
                            samples.size()));
            assertTrue(expectedMeanDirection.equals(CoreSection.
                    fromSamples(samples).getDirectionNearEnd(
                            CoreSection.End.BOTTOM, nSamples), 1e-10));
        }        
    }

    @Test(expected = IllegalStateException.class)
    public void testGetDirectionNearEndInvalid() {
        final List<Sample> samples = makeSampleListDirectionsOnly();
        samples.get(2).setImportedDirection(null);
        CoreSection.fromSamples(samples).getDirectionNearEnd(End.TOP, 3);
    }
    
    @Test
    public void testIsDirectionDefinedNearEnd() {
        final List<Sample> samples = makeSampleListDirectionsOnly();
        samples.get(2).setImportedDirection(null);
        samples.get(6).setImportedDirection(null);
        final CoreSection section = CoreSection.fromSamples(samples);
        assertTrue(section.isDirectionDefinedNearEnd(End.TOP, 1));
        assertTrue(section.isDirectionDefinedNearEnd(End.TOP, 2));
        assertFalse(section.isDirectionDefinedNearEnd(End.TOP, 3));
        assertTrue(section.isDirectionDefinedNearEnd(End.BOTTOM, 3));
        assertFalse(section.isDirectionDefinedNearEnd(End.BOTTOM, 4));
    }
    
    private static Vec3 meanDirection(List<Sample> samples) {
        return FisherValues.calculate(samples.stream().
                        map(Sample::getDirection).
                        collect(Collectors.toList())).getMeanDirection();
    }

    @Test
    public void testRotateDeclinationsWithImportedDirections() {
        List<Vec3> inputDirs = makeRandomDirections();
        final double angle = 19;
        final List<Vec3> expectedDirs = inputDirs.stream().
                map((Vec3 v) -> v.rotZ(Math.toRadians(angle))).
                collect(Collectors.toList());
        final List<Sample> samples = IntStream.range(0, inputDirs.size()).
                mapToObj(i -> {
                    Sample s = new Sample("test" + i, null);
                    s.setImportedDirection(inputDirs.get(i));
                    return s;
                }).collect(Collectors.toList());
        CoreSection.fromSamples(samples).rotateDeclinations(angle);
        assertTrue(IntStream.range(0, 10).
                allMatch((int i) -> expectedDirs.get(i).
                        equals(samples.get(i).getDirection(), delta)));
    }

    @Test
    public void testRotateDeclinationsWithData() {
        final List<Vec3> inputDirs = makeRandomDirections();
        final double angle = 56;
        final List<Vec3> expectedDirs = inputDirs.stream().map((Vec3 v) ->
                v.rotZ(Math.toRadians(angle))).collect(Collectors.toList());
        final List<Sample> samples = IntStream.range(0,
                inputDirs.size()).mapToObj((int i) -> {
            Sample s = new Sample("test" + i, null);
            for (Vec3 v: new Vec3[] {inputDirs.get(i),
                inputDirs.get(i).times(0.5)} ) {
                    TreatmentStep d = new TreatmentStep(v);
                    s.addDatum(d);
                    d.setInPca(true);
                    d.setPcaAnchored(true);
                    s.doPca(Correction.NONE);
            }
            return s;
        }).collect(Collectors.toList());
        CoreSection.fromSamples(samples).rotateDeclinations(angle);
        samples.forEach(s -> s.doPca(Correction.NONE));
        assertTrue(IntStream.range(0, 10).allMatch((int i) ->
                expectedDirs.get(i).equals(samples.get(i).getDirection(),
                        delta)));
    }

    private static List<Vec3> makeRandomDirections() {
        final Random rnd = new Random(42);
        final List<Vec3> inputDirs = IntStream.range(0, 10).
                mapToObj(i -> TestUtils.randomVector(rnd, 1).normalize()).
                collect(Collectors.toList());
        return inputDirs;
    }
    
}
