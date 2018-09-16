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
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author pont
 */
public class CoreSectionTest {

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
                final Datum d = new Datum((depth+1.)*(100.-demag),
                        depth*50, demag);
                d.setDepth(depthString);
                d.setMeasType(MeasType.CONTINUOUS);
                d.setAfX(demag);
                d.setAfY(demag);
                d.setAfZ(demag);
                d.setTreatType(TreatType.DEGAUSS_XYZ);
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
                map(Datum::getMoment).collect(Collectors.toList());
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
            assertTrue(expectedMeanDirection.equals(actualMeanDirection, 1e-10));
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

    private static Vec3 meanDirection(List<Sample> samples) {
        return FisherValues.calculate(samples.stream().
                        map(Sample::getDirection).
                        collect(Collectors.toList())).getMeanDirection();
    }
    
}
