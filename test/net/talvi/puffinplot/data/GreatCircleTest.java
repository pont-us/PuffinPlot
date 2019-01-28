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
import java.util.Random;
import java.util.stream.Collectors;

import net.talvi.puffinplot.TestUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author pont
 */
public class GreatCircleTest {
    
    @Test
    public void testCalculations() {
        // Pre-generated test data from a Python script.
        double[][][] testData = {
{{-22,-6,-25},{-55,97,81},{81,39,70},{-28,-71,-93},{-36,-1,92},{8,-35,29},
{0.824186, 0.408252, -0.392489, 45.7759}},
{{64,76,86},{3,-64,42},{-84,-64,-49},{-61,81,37},{44,76,-46},
{0.789367, -0.263772, -0.554368, 45.1258}},
{{39,-68,84},{64,-82,-20},{5,-79,30},{22,64,54},{-63,75,5},
{0.879881, 0.346402, -0.325294, 36.0564}},
{{-95,11,-4},{47,-86,93},{90,-9,-87},{23,-4,49},{99,-98,4},
{0.39603, 0.88335, 0.250703, 30.5143}},
{{-70,43,-45},{-36,35,-7},{-85,-31,-83},{-29,-50,92},
{0.172889, 0.886682, 0.428841, 44.4424}},
{{-35,69,-21},{-10,-18,-56},{-14,-75,48},{49,-30,-14},
{0.855913, 0.481309, 0.189087, 40.5577}},
{{-87,93,-10},{-95,20,91},{-30,22,63},
{0.667007, 0.693198, 0.273091, 19.6654}},
{{87,-49,-20},{-33,-80,92},{84,25,-94},
{0.497342, 0.579814, 0.645342, 3.39288}},
{{4,25,88},{52,1,-53},{-49,-35,-61},{74,-71,-45},
{0.0247533, 0.916315, -0.399692, 31.1403}},
{{38,-21,-19},{-19,-87,-58},{8,-76,77},
{0.918558, 0.24885, 0.307124, 44.3086}},
        };
        
        List<List<Vec3>> inputs = new ArrayList<>(testData.length);
        List<Vec3> poles = new ArrayList<>(testData.length);
        List<Double> mad1s = new ArrayList<>(testData.length);
        for (double[][] data: testData) {
            final List<Vec3> vecList = new ArrayList<>(data.length-1);
            inputs.add(vecList);
            for (int i=0; i<data.length-1; i++) {
                final double[] comps = data[i];
                vecList.add(new Vec3(comps[0], comps[1], comps[2]));
            }
            final double[] poleAndMad = data[data.length-1];
            poles.add(new Vec3(poleAndMad[0], poleAndMad[1], poleAndMad[2]));
            mad1s.add(poleAndMad[3]);
        }
        
        for (int i=0; i<inputs.size(); i++) {
            final GreatCircle actual = GreatCircle.fromBestFit(inputs.get(i));
            final Vec3 pole = actual.getPole().x > 0 ?
                    actual.getPole() : actual.getPole().invert();
            assertTrue(poles.get(i).equals(pole, 1e-6));
            assertEquals(mad1s.get(i), actual.getMad1(), 1e-3);
            assertEquals(inputs.get(i).stream().map(v -> v.normalize()).
                    collect(Collectors.toList()),
                    actual.getPoints());
        }
    }

    @Test
    public void testNearestOnCircle() {
        final Random random = new Random(23);
        for (int i=0; i<10; i++) {
            final int nVecs = random.nextInt(10)+4;
            final List<Vec3> vecs = new ArrayList<>(nVecs);
            for (int j=0; j<nVecs; j++) {
                vecs.add(new Vec3(random.nextDouble()-0.5*10,
                random.nextDouble()-0.5*10, random.nextDouble()-0.5*10));
            }
            final GreatCircle gc = GreatCircle.fromBestFit(vecs);
            final Vec3 otherVector = new Vec3(random.nextDouble()-0.5*10,
                random.nextDouble()-0.5*10, random.nextDouble()-0.5*10);
            assertTrue(gc.getPole().nearestOnCircle(otherVector).
                    equals(gc.nearestOnCircle(otherVector), 1e-6));
        }
    }

    @Test
    public void testLastPoint() {
        final Random random = new Random(17);
        for (int i=0; i<10; i++) {
            final int nVecs = random.nextInt(10)+4;
            final List<Vec3> vecs = new ArrayList<>(nVecs);
            for (int j=0; j<nVecs; j++) {
                vecs.add(new Vec3(random.nextDouble()-0.5*10,
                random.nextDouble()-0.5*10, random.nextDouble()-0.5*10));
            }
            final GreatCircle gc = GreatCircle.fromBestFit(vecs);
            assertEquals(vecs.get(nVecs-1).normalize(), gc.lastPoint());
        }
    }

    @Test
    public void testAngleFromLast() {
        final Vec3 v0 = new Vec3(1, 0, 0);
        final Vec3 v1 = new Vec3(1, 1, 0);
        final Vec3 v2 = new Vec3(0, 1, 0);
        final Vec3 v3 = new Vec3(-1, 1, 0);
        final GreatCircle gc = GreatCircle.fromBestFit(Arrays.asList(new Vec3[] {v0, v1, v2}));
        assertEquals(0, gc.angleFromLast(v2), 1e-6);
        assertEquals(-Math.PI/2, gc.angleFromLast(v1), 1e-6);
        assertEquals(Math.PI/2, gc.angleFromLast(v3), 1e-6);
    }

    @Test
    public void testToStrings() {
        final Vec3 v0 = new Vec3(1, 0, 0);
        final Vec3 v1 = new Vec3(1, 1, 0);
        final Vec3 v2 = new Vec3(0, 1, 0);
        final GreatCircle gc = GreatCircle.fromBestFit(Arrays.asList(new Vec3[] {v0, v1, v2}));
        final List<Double> expected = Arrays.stream(new double[] {
            0, 90, 90, 0, 0, 3}).boxed().collect(Collectors.toList());
        final List<Double> actual = gc.toStrings().stream().
                map(Double::parseDouble).collect(Collectors.toList());
        assertEquals(expected, actual);
    }

    @Test
    public void testGetEmptyFields() {
        assertEquals(GreatCircle.getHeaders().size(),
                GreatCircle.getEmptyFields().size());
        assertTrue(GreatCircle.getEmptyFields().stream().
                allMatch(field -> "".equals(field)));
    }

    @Test
    public void testGetHeaders() {
        final String expected = "GC dec (deg),GC inc (deg)," +
            "GC strike (deg),GC dip (deg),GC MAD1,GC npoints";
        final String actual = String.join(",", GreatCircle.getHeaders());
        assertEquals(expected, actual);
        assertTrue(TestUtils.isPrintableAscii(actual));
    }
   
    /**
     * Test construction of a great circle directly by just supplying
     * its pole.
     */
    @Test
    public void testPoleConstructor() {
        final GreatCircle gc = GreatCircle.fromPole(Vec3.NORTH);
        assertEquals(Vec3.NORTH, gc.getPole());
    }
    
    /**
     * Test that trying to get the last point of a great circle with no
     * points throws an UnsupportedOperationException.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testLastPointWithNoPoints() {
        final GreatCircle gc = GreatCircle.fromPole(Vec3.NORTH);
        gc.lastPoint();
    }
    
    /**
     * Test that calling angleFromLast on a great circle with no points
     * throw an UnsupportedOperationException.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testAngleFromLastWithNoPoints() {
        final GreatCircle gc = GreatCircle.fromPole(Vec3.NORTH);
        gc.angleFromLast(Vec3.EAST);
    }
    
    /**
     * Test that calling getMad1 on a great circle with no points
     * throw an UnsupportedOperationException.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testMad1WithNoPoints() {
        final GreatCircle gc = GreatCircle.fromPole(Vec3.NORTH);
        gc.getMad1();
    }

}
