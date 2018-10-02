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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

public class CoreSectionsTest {

    private List<Sample> sampleList;
    private final double delta = 1e-10;
    
    @Before
    public void setUp() {
        sampleList = CoreSectionTest.makeSampleList();
    }
    
    @Test
    public void testSplitByDiscreteId() {
        final CoreSections sections =
                CoreSections.fromSampleListByDiscreteId(sampleList);
        final List<String> expectedPartition =
                Arrays.asList("0,1,2", "3,4,5", "6,7,8", "9");
        final List<String> actualPartition = new ArrayList<>();
        for (CoreSection section: sections.getSections().values()) {
            final String actualSampleDepths = section.getSamples().stream().
                    map(Sample::getNameOrDepth).
                    collect(Collectors.joining(","));
            actualPartition.add(actualSampleDepths);
        }
        assertEquals(expectedPartition, actualPartition);
    }
    
    /**
     * Simple alignment test of three sections with a margin of one sample. Each
     * section has a constant declination, so the final core is expected to have
     * the same declination throughout.
     */
    @Test
    public void testAlignSections1() {
        final List<Sample> samples =
                makeUniformSampleList(Vec3.fromPolarDegrees(1, 40, 20),
                        new double[] {0, 1, 2, 3}, "part0");
        samples.addAll(makeUniformSampleList(Vec3.fromPolarDegrees(1, 50, 30),
                        new double[] {4, 5, 6, 7}, "part1"));
        samples.addAll(makeUniformSampleList(Vec3.fromPolarDegrees(1, 60, -30),
                        new double[] {8, 9, 10, 11}, "part2"));
        final CoreSections cs =
                CoreSections.fromSampleListByDiscreteId(samples);
        final double topAlignment = 17;
        cs.alignSections(topAlignment, 1);
        assertTrue(samples.stream().allMatch(s -> Math.abs(s.getDirection().
                        getDecDeg() - topAlignment) < delta));
        assertArrayEquals(
                new double[] {40, 40, 40, 40, 50, 50, 50, 50, 60, 60, 60, 60},
                samples.stream().
                        mapToDouble(s -> s.getDirection().getIncDeg()).
                        toArray(),
                delta);
    }
    
    /**
     * Test section alignment with a margin of three samples. Expected results
     * worked out by hand. To keep things simple, all samples have flat
     * inclination and declinations are all multiples of 45 degrees.
     */
    @Test
    public void testAlignSections2() {
        final double[][] inputDecs = {
            {0, 45, 90, 90, 135, 180},
            {270, 315, 0, 225, 180, 270},
            {90, 45, 135, 0, 0, 0}
        };
        final double[][] expectedOutput = {
            {270, 315, 0, 0, 45, 90},
            {0, 45, 90, 315, 270, 0},
            {315, 270, 0, 225, 225, 225}
        };
        final List<Sample> samples = new ArrayList<>();
        int depth = 0;
        for (int i = 0; i < inputDecs.length; i += 1) {
            samples.addAll(makeFlatSamples(inputDecs[i], "part"+i, depth));
            depth += inputDecs[i].length;
        }
        final CoreSections cs =
                CoreSections.fromSampleListByDiscreteId(samples);
        final double topAlignment = 315;
        cs.alignSections(topAlignment, 3);
        final Iterator<String> csIterator =
                cs.getSections().keySet().iterator();
        for (int i = 0; i < inputDecs.length; i += 1) {
            final double[] expected = expectedOutput[i];
            final double[] actual =
                    cs.getSections().get(csIterator.next()).getSamples().
                            stream().mapToDouble(s -> s.getDirection().
                                    getDecDeg()).
                            map(d -> d > 360-delta ? d-360 : d).toArray();
            assertArrayEquals(expected, actual, delta);
        }
    }
    
    private List<Sample> makeUniformSampleList(Vec3 direction,
            double[] depths, String discreteId) {
        final List<Sample> samples = new ArrayList<>(10);
        for (int i=0; i<depths.length; i++) {
            Sample sample = makeSample(depths[i], discreteId, direction);
            samples.add(sample);
        }
        return samples;
    }

    private List<Sample> makeFlatSamples(double[] decs, String discreteId,
            int startDepth) {
        int depth = startDepth;
        final List<Sample> samples = new ArrayList<>();
        for (double dec: decs) {
            samples.add(makeSample(depth, discreteId,
                    Vec3.fromPolarDegrees(1, 0, dec)));
            depth++;
        }
        return samples;
    }
   
    private Sample makeSample(double depth, String discreteId, Vec3 direction) {
        final String depthString = String.format("%f", depth);
        final Sample sample = new Sample(depthString, null);
        for (int j=3; j>0; j--) {
            final Datum d = new Datum(direction.times(j));
            d.setInPca(true);
            sample.addDatum(d);
        }
        sample.setDiscreteId(discreteId);
        sample.doPca(Correction.NONE);
        return sample;
    }

    
}
