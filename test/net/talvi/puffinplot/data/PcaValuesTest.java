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
        
        double[][] coords = 
        {{1,1,-1},
         {1,1,0},
         {1,1,1}};
        
        ArrayList<Point> points = new ArrayList(coords.length);
        for (double[] coord: coords)
            points.add(new Point(coord[0], coord[1], coord[2]));
        
        Point centreOfMass = Point.centreOfMass(points);
        
        PcaValues anchored = PcaValues.calculate(points, Point.ORIGIN);
        PcaValues unanchored = PcaValues.calculate(points, centreOfMass);
        
        System.out.println("Anchored: "+anchored);
        System.out.println("Unnchored: " + unanchored);

        assertEquals(Math.PI/4, anchored.dec, 1e-6);
        assertEquals(0, anchored.inc, 1e-6);
        assertEquals(0, anchored.mad1, 1e-6);
        assertEquals(30, anchored.mad3, 1e-6);
        assertEquals(0, unanchored.dec, 1e-6);
        assertEquals(Math.PI/2, unanchored.inc, 1e-6);
        assertEquals(0, unanchored.mad1, 1e-6);
        assertEquals(0, unanchored.mad3, 1e-6);
    }

}