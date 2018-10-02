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
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author pont
 */
public class CoreSectionsTest {

    private List<Sample> sampleList;
    private double delta = 1e-10;
    
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
    
    @Test
    public void testAlignSections() {
        final List<Sample> samples =
                makeUniformSampleList(Vec3.fromPolarDegrees(1, 40, 20),
                        new double[] {0, 1, 2, 3}, "part0");
        samples.addAll(makeUniformSampleList(Vec3.fromPolarDegrees(1, 50, 30),
                        new double[] {4, 5, 6, 7}, "part1"));
        samples.forEach(s -> s.doPca(Correction.NONE));
        final CoreSections cs =
                CoreSections.fromSampleListByDiscreteId(samples);
        final double topAlignment = 0;
        cs.alignSections(topAlignment);
        assertTrue(cs.getSections().get("part0").getSamples().stream().
                allMatch(s -> Math.abs(s.getDirection().getDecDeg() - topAlignment) < delta));
    }
    
    private List<Sample> makeUniformSampleList(Vec3 direction,
            double[] depths, String name) {
        final List<Sample> samples = new ArrayList<>(10);
        for (int i=0; i<depths.length; i++) {
            final String depthString = String.format("%f", depths[i]);
            final Sample sample = new Sample(depthString, null);
            for (int j=3; j>0; j--) {
                final Datum d = new Datum(direction.times(j));
                d.setInPca(true);
                sample.addDatum(d);
            }
            sample.setDiscreteId(name);
            samples.add(sample);
        }
        return samples;
    }
    
    
}
