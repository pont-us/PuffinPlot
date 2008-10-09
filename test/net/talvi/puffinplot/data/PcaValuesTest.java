/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class PcaValuesTest {

    public PcaValuesTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCalculate() {
        System.out.println("calculate");
        
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
        
        ArrayList<Point> points = new ArrayList(coords.length);
        for (double[] coord: coords)
            points.add(new Point(coord[0], coord[1], coord[2]));
        
        Point centreOfMass = Point.centreOfMass(points);
        
        PcaValues anchored = PcaValues.calculate(points, Point.ORIGIN);
        PcaValues unanchored = PcaValues.calculate(points, centreOfMass);

        /* Declination worked out by hand, other values from
         * Tauxe's programs, modified to allow anchoring to origin
         * and reporting both MAD values. In retrospect MAD1==0 is obvious
         * since of course three points on a line plus any one other point
         * will be coplanar!
         */
        assertEquals(Math.PI/4, anchored.dec, 1e-6);
        assertEquals(Math.toRadians(22.266), anchored.inc, 0.01);
        assertEquals(0, anchored.mad1, 0.01);
        assertEquals(6.06, anchored.mad3, 0.01);
        
        // These values worked out by hand.
        assertEquals(Math.atan(1/Math.sqrt(2)), unanchored.inc, 1e-6);
        assertEquals(Math.PI/4, unanchored.dec, 1e-6);
        assertEquals(0, unanchored.mad1, 1e-6);
        assertEquals(0, unanchored.mad3, 1e-6);
    }

}