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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link PcaValues} class.
 * 
 * @author pont
 */
public class PcaValuesTest {

    /**
     * Tests the {@link PcaValues#calculate(java.util.List, boolean)} method.
     */
    @Test
    public void testCalculateWithSimpleData() {
        /* Not the most rigorous of tests. Ideally I would like to
         * tweak Lisa Tauxe's Python code to produce corresponding results,
         * and check its output against PcaValues's on a slightly
         * larger, noisier dataset (the current dataset doesn't test
         * MAD calculation very well, for example). But this will have to
         * do for now.
         */
        double[][] coords = 
        {{3,3,2},
         {2,2,1},
         {1,1,0}};
        
        ArrayList<Vec3> points = new ArrayList<>(coords.length);
        for (double[] coord: coords)
            points.add(new Vec3(coord[0], coord[1], coord[2]));
        
        PcaValues anchored = PcaValues.calculate(points, true);
        PcaValues unanchored = PcaValues.calculate(points, false);

        /* Declination worked out by hand, other values from
         * Tauxe's programs, modified to allow anchoring to origin
         * and reporting both MAD values. In retrospect MAD1==0 is obvious
         * since of course three points on a line plus any one other point
         * will be coplanar!
         */
        final Vec3 aDir = anchored.getDirection();
        assertEquals(Math.PI/4, aDir.getDecRad(), 1e-6);
        assertEquals(Math.toRadians(22.266), aDir.getIncRad(), 0.01);
        assertEquals(0, anchored.getMad1(), 0.01);
        assertEquals(6.06, anchored.getMad3(), 0.01);
        
        // These values worked out by hand.
        final Vec3 uaDir = unanchored.getDirection();
        assertEquals(Math.atan(1/Math.sqrt(2)), uaDir.getIncRad(), 1e-6);
        assertEquals(Math.PI/4, uaDir.getDecRad(), 1e-6);
        assertEquals(0, unanchored.getMad1(), 1e-6);
        assertEquals(0, unanchored.getMad3(), 1e-6);
    }
    
    @Test
    public void testCalculateWithPseudorandomData() {
        /* Generate input data from a PRNG with a fixed seed
         * -- concise and deterministic.
         */
        final Random rnd = new Random(23);
        final List<Vec3> vectors = new ArrayList<>(100);
        for (int i=0; i<100; i++) {
            vectors.add(new Vec3(rnd.nextInt(100)-50,
                    rnd.nextInt(100)-50, rnd.nextInt(100)-50));
        }
        
        /* The expected output was generated from PmagPy's pca.py
         * script using identical input data.
         */
        final double[][] expectedValues = {
            //ldec   linc   mad3   pdec   pinc   mad1
            {235.9, -18.0,  36.2, 184.5,  48.5,  46.1},
            { 16.6,   7.9,  24.4, 105.7, -38.5,  27.3},
            { 44.5, -56.0,  40.5, 355.4,  23.7,  46.7},
            { 69.4,  27.3,  45.4, 129.9, -44.3,  31.2},
            { 29.6,  40.4,  39.9, 239.6,  32.3,  46.5},
            {231.0, -28.5,  45.0, 310.1,  15.3,  46.2},
            {335.0,  -5.8,  38.3, 257.4,  45.7,  36.2},
            { 30.5,   7.0,  45.8, 322.9, -25.1,  50.1},
            {166.6,  50.9,  42.6,  27.1,  35.3,  35.7},
            {  8.7,  28.9,  47.3,  71.7, -41.0,  47.8},
        };
        
        /* Step through the input array, slicing out chunks of increasing
         * length from 5 to 14 and running a PCA on each one. pca.py 
         * writes results to 1 d.p. so we only check them to this precision.
         */
        int startPos = 0;
        for (int sublistLength=5, i=0;
                sublistLength<15;
                sublistLength++, i++) {
            final List<Vec3> sublist =
                    vectors.subList(startPos, startPos+sublistLength);
            
            // Check unanchored fit against expected direction and MAD3
            final PcaValues pca = PcaValues.calculate(sublist, false);
            assertEquals(expectedValues[i][0],
                    pca.getDirection().getDecDeg(), 0.1);
            assertEquals(expectedValues[i][1],
                    pca.getDirection().getIncDeg(), 0.1);
            assertEquals(expectedValues[i][2], pca.getMad3(), 0.1);
            
            /* PmagPy automatically normalizes and anchors vectors
             * for a planar PCA, so we do the same in order to match
             * the expected values.
             */
            final PcaValues pcaNormalizedAnchored = PcaValues.calculate(
                    sublist.stream().map(v -> v.normalize()).
                            collect(Collectors.toList()), true);
            assertEquals(expectedValues[i][5],
                    pcaNormalizedAnchored.getMad1(), 0.1);
            /* PcaValues doesn't produce a normal to the plane fit,
             * but of course the linear fit lies within this plane,
             * so we check that it's at right angles to the precalculated
             * plane-fit normal.
             */
            final Vec3 expectedNormal = Vec3.fromPolarDegrees(1,
                    expectedValues[i][4], expectedValues[i][3]);
            assertEquals(0,
                    expectedNormal.dot(pcaNormalizedAnchored.getDirection()),
                    0.001);
            
            // May as well test isAnchored and getOrigin while we're here.
            assertEquals(pca.isAnchored(), false);
            assertEquals(pcaNormalizedAnchored.isAnchored(), true);
            assertTrue(Vec3.ORIGIN.equals(pcaNormalizedAnchored.getOrigin()));
            
            startPos += sublistLength;
        }
    }
}
