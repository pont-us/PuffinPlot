/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.talvi.puffinplot;

import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.BitSet;
import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class UtilTest {

    private BitSet makeBitSet(String spec) {
        BitSet result = new BitSet();
        for (int i=0; i<spec.length(); i++) {
            result.set(i, spec.substring(i, i+1).equals("1"));
        }
        return result;
    }
    
    @Test
    public void testNumberRangeStringToBitSet() {
        final String[] inputsAndResults = {
            "1", "1",
            "1,3", "101",
            "4-6", "000111",
            "4-6,8-10,10,11,15-16", "0001110111100011",
            "1-4,3-5,10,12-14,17", "11111000010111001",
            "1-1000", "11111111111111111111",
            "1-5,10-85", "11111000011111111111"
        };
        final int limit = 20;
        for (int i=0; i<inputsAndResults.length; i+=2) {
            assertEquals(makeBitSet(inputsAndResults[i+1]),
                    Util.numberRangeStringToBitSet(inputsAndResults[i], limit));
        }
    }
    
    private boolean linesEqual(Line2D l1, Line2D l2) {
        final double tolerance = 0.00001;
        if (l1==null && l2==null) return true;
        if (l1==null ^ l2==null) return false;
        if (l1.getP1().distance(l2.getP1()) < tolerance &&
                l1.getP2().distance(l2.getP2()) < tolerance) return true;
        if (l1.getP1().distance(l2.getP2()) < tolerance &&
                l1.getP2().distance(l2.getP1()) < tolerance) return true;
        return false;
    }
    
    private class ClipTester {
        private final Rectangle2D[] rectangles;
        private final Line2D[] lines;
        private final Line2D[][] clips;
        
        public ClipTester(double[][] rectangles, double[][] lines,
                double[][] clips) {
            final int numRects = rectangles.length;
            final int numLines = lines.length;
            this.rectangles = new Rectangle2D[numRects];
            for (int i=0; i<numRects; i++) {
                double[] ps = rectangles[i];
                this.rectangles[i] =
                        new Rectangle2D.Double(ps[0], ps[1], ps[2], ps[3]);
            }
            this.lines = new Line2D[numLines];
            for (int i=0; i<numLines; i++) {
                double[] ps = lines[i];
                this.lines[i] =
                        new Line2D.Double(ps[0], ps[1], ps[2], ps[3]);
            }
            this.clips = new Line2D[numLines][numRects];
            for (int i=0; i<numLines; i++) {
                Line2D[] clipsForLine = new Line2D[numRects];
                this.clips[i] = clipsForLine;
                for (int j=0; j<numRects; j++) {
                    double[] ps = clips[i*numRects + j];
                    clipsForLine[j] = ps.length == 0 ? null :
                            new Line2D.Double(ps[0], ps[1], ps[2], ps[3]);
                }
            }
        }
        
        private void prettyPrint(Line2D line) {
            System.out.println(String.format(Locale.ENGLISH,
                    "%.3f %.3f %.3f %.3f",
                    line.getX1(), line.getY1(), line.getX2(), line.getY2()));
        }
        
        private void prettyPrint(Rectangle2D rect) {
            System.out.println(String.format(Locale.ENGLISH,
                    "%.3f %.3f %.3f %.3f",
                    rect.getMinX(), rect.getMinY(), rect.getWidth(), rect.getHeight()));
        }
        
        public void testAll() {
            for (int i=0; i<lines.length; i++) {
                final Line2D unclipped = lines[i];
                for (int j=0; j<rectangles.length; j++) {
                    final Line2D line0 = clips[i][j];
                    final Line2D line1 =
                            Util.clipLineToRectangle(unclipped, rectangles[j]);
                    if (!linesEqual(line0, line1)) {
                        prettyPrint(rectangles[j]);
                        prettyPrint(unclipped);
                        prettyPrint(line0);
                        prettyPrint(line1);
                        fail("Line "+i+" Rect "+j);
                    }
                    assertTrue(linesEqual(line0, line1));
                }
            }
        }

    }
    
    /**
     * Tests the {@link Util#clipLineToRectangle(java.awt.geom.Line2D, java.awt.geom.Rectangle2D)} method.
     */
    @Test
    public void testClipLineToRectangle() {
        
        double[][] rects = {
            {1, 3, 2, 2},
            {3, 3, 2, 2},
            {1, 1, 2, 2},
            {3, 1, 2, 2}
        };
        
        double[][] lines = {
            {0.5, 3.0, 3.0, 5.5},
            {1.0, 5.5, 4.0, 4.0},
            {4.5, 6.0, 4.5, 2.0},
            {3.5, 2.5, 4.5, 1.5},
            {2.0, 2.0, 4.0, 2.0},
            {0.5, 1.0, 2.0, 4.0}
        };
        
        double[][] results = {
            {1, 3.5, 2.5, 5}, {},               {},       {},
            {2, 5, 3, 4.5},   {3, 4.5, 4, 4},   {},       {},
            {},               {4.5, 3, 4.5, 5}, {},       {4.5, 2, 4.5, 3},
            {},               {},               {},       {3.5, 2.5, 4.5, 1.5},
            {},               {},               {2, 2, 3, 2},   {3, 2, 4, 2},
            {1.5, 3, 2, 4},   {},               {1, 2, 1.5, 3}, {}
        };
        
        final ClipTester clipTester =
                new ClipTester(rects, lines, results);
        clipTester.testAll();
        
        final Line2D line0 = new Line2D.Double(1, 1, 2, 2);
        assertNull(Util.clipLineToRectangle(line0,
                new Rectangle2D.Double(3, 3, 3, 3)));
        final Line2D clipped0 = Util.clipLineToRectangle(line0,
                new Rectangle2D.Double(0, 0, 4, 4));
        assertTrue(linesEqual(line0, clipped0));
    }
}
