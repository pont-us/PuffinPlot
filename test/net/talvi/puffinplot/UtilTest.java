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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UtilTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();
    
    private final double delta = 1e-10;
    
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
            "1-5,10-85", "11111000011111111111",
            "1,not numbers,5-not a number", "1",
            "-1,2", "11",
            "-99", "1"
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
    
    @Test
    public void testClipLineToRectangleWithNullLine() {
        assertNull(Util.clipLineToRectangle(null,
                new Rectangle2D.Double(1, 2, 3, 4)));
    }

    @Test
    public void testClipLineToRectangleWithNullRectangle() {
        assertNull(Util.clipLineToRectangle(new Line2D.Double(1, 2, 3, 4),
                null));
    }

    @Test
    public void testParseGitTimestamp() {
        final ZonedDateTime expected =
                ZonedDateTime.parse("2018-10-07T11:09:17+02:00");
        final String gitTimestap = "1538903357 +0200";
        assertEquals(expected, Util.parseGitTimestamp(gitTimestap));
    }
    
    @Test(expected = NullPointerException.class)
    public void testParseGitTimestampWithNull() {
        Util.parseGitTimestamp(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testParseGitTimestampWithEmptyString() {
        Util.parseGitTimestamp("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseGitTimestampWithThreeFields() {
        Util.parseGitTimestamp("1 2 3");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseGitTimestampWithOneField() {
        Util.parseGitTimestamp("1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseGitTimestampWithBadTimestamp() {
        Util.parseGitTimestamp("wibble +0000");
    }

    @Test(expected = DateTimeException.class)
    public void testParseGitTimestampWithBadTimezone() {
        Util.parseGitTimestamp("1538903357 +wibble");
    }

    @Test
    public void testCalculateSha1() throws
            FileNotFoundException, IOException, NoSuchAlgorithmException {
        final String inputData = String.join("", Collections.nCopies(20,
                "An arbitrary string for use as SHA-1 input data. "));
        final File inputFile = tempDir.newFile("sha1-input-data");
        try (PrintWriter out = new PrintWriter(inputFile)) {
            out.println(inputData);
        }
        final String actual = Util.calculateSHA1(inputFile);
        // Expected value calculated by sha1sum (GNU coreutils)
        final String expected =
                "79F6BF1176017EF4CA66824E5DEFFC7C609D06E8";
        assertEquals(expected, actual);
    }
    
    @Test
    public void testDownloadUrlToFile() throws IOException {
        final String originalData = "An arbitrary string.\n";
        final File source = tempDir.newFile("source");
        final Path destination =
                tempDir.getRoot().toPath().resolve("destination");
        try (PrintWriter out = new PrintWriter(source)) {
            out.print(originalData);
        }
        Util.downloadUrlToFile(source.toURI().toURL().toString(),
                destination);
        final String downloadedData =
                new String(Files.readAllBytes(destination));
        assertEquals(originalData, downloadedData);
    }
    
    @Test
    public void testParseDoubleSafely() {
        assertEquals(9.87, Util.parseDoubleSafely("9.87"), 1e-10);
        assertEquals(0, Util.parseDoubleSafely("not a number"), 1e-10);
    }
    
    @Test
    public void testScaleLine() {
        final Line2D line = new Line2D.Double(0, 0, 1, 1);
        checkLinesEqual(new Line2D.Double(-1, -1, 2, 2),
                Util.scaleLine(line, 3));
    }
    
    @Test
    public void testScaleLineNull() {
        assertNull(Util.scaleLine(null, 42));
    }

    @Test
    public void testScaleLineByOne() {
        final Line2D line = new Line2D.Double(2, 2, 3, 3);
        checkLinesEqual(line, Util.scaleLine(line, 1));
    }
    
    private void checkLinesEqual(Line2D expected, Line2D actual) {
        /*
         * Oddly, Line2D doesn't override equals, but Point2D does, so
         * we have to check the endpoints individually.
         */
        assertEquals(expected.getP1(), actual.getP1());
        assertEquals(expected.getP2(), actual.getP2());        
    }
    
    @Test
    public void testEnvelopeWithNoPoints() {
        assertNull(Util.envelope(Collections.emptyList()));
    }
    
    @Test
    public void testEnvelope() {
        final List<Point2D> points = Arrays.asList(
                new Point2D.Double(-2, -3),
                new Point2D.Double(0, 4),
                new Point2D.Double(3, -1),
                new Point2D.Double(-5, 2)
        );
        final Rectangle2D envelope = Util.envelope(points);
        assertEquals(-5, envelope.getMinX(), delta);
        assertEquals(-3, envelope.getMinY(), delta);
        assertEquals(3, envelope.getMaxX(), delta);
        assertEquals(4, envelope.getMaxY(), delta);
    }
}
