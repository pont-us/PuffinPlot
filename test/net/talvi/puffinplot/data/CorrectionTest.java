/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
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

import java.util.InputMismatchException;
import net.talvi.puffinplot.data.Correction.Rotation;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class CorrectionTest {
    
    public CorrectionTest() {
    }
    
    @Test
    public void testSetAndGetRotation() {
        final Correction correction = new Correction(false, false, null, false);
        for (Rotation rotation: Rotation.values()) {
            correction.setRotation(rotation);
            assertEquals(rotation, correction.getRotation());
        }
    }

    @Test
    public void testSetAndIsMagDevAppliedToFormation() {
        final Correction correction = new Correction(false, false, null, false);
        for (boolean value: new boolean[] {false, true}) {
            correction.setMagDevAppliedToFormation(value);
            assertEquals(value, correction.isMagDevAppliedToFormation());
        }
    }

    @Test
    public void testGetDescription() {
        /*  The description format isn't rigidly defined; here we just test
         *  that it contains the "nice name" for the rotation.
         */
        for (Rotation rotation: Rotation.values()) {
            for (boolean tray : new boolean[]{false, true}) {
                for (boolean empty : new boolean[]{false, true}) {
                    for (boolean magdev : new boolean[]{false, true}) {
            
            final Correction correction = new Correction(tray, empty, null, magdev);
            correction.setRotation(rotation);
            assertTrue(correction.getDescription().contains(rotation.getNiceName()));
                    }}}
                    }
    }

    @Test
    public void testIncludesSample() {
        assertFalse(new Correction(false, false, Rotation.NONE, false).
                includesSample());
        assertTrue(new Correction(false, false, Rotation.SAMPLE, false).
                includesSample());
        assertTrue(new Correction(false, false, Rotation.FORMATION, false).
                includesSample());
    }

    @Test
    public void testIncludesFormation() {
        assertFalse(new Correction(false, false, Rotation.NONE, false).
                includesFormation());
        assertFalse(new Correction(false, false, Rotation.SAMPLE, false).
                includesFormation());
        assertTrue(new Correction(false, false, Rotation.FORMATION, false).
                includesFormation());
    }

    @Test
    public void testIncludesTray() {
        assertFalse(new Correction(false, false, Rotation.NONE, false).
                includesTray());
        assertTrue(new Correction(true, false, Rotation.NONE, false).
                includesTray());
    }

    @Test
    public void testIncludesEmpty() {
        assertFalse(new Correction(false, false, Rotation.NONE, false).
                includesEmpty());
        assertTrue(new Correction(false, true, Rotation.NONE, false).
                includesEmpty());
    }

    @Test
    public void testToAndFromString() {
        for (Rotation rotation : Rotation.values()) {
            for (boolean tray : new boolean[]{false, true}) {
                for (boolean empty : new boolean[]{false, true}) {
                    for (boolean magdev : new boolean[]{false, true}) {
                        final Correction c0 = new Correction(tray, empty,
                                rotation, magdev);
                        final Correction c1 = Correction.fromString(
                                c0.toString());
                        assertEquals(c0.includesTray(), c1.includesTray());
                        assertEquals(c0.includesEmpty(), c1.includesEmpty());
                        assertEquals(c0.isMagDevAppliedToFormation(),
                                c1.isMagDevAppliedToFormation());
                        assertEquals(c0.getRotation(), c1.getRotation());
                        
                    }
                }
            }
        }
    }
    
    @Test
    public void testFromStringMalformedInput() {
        assertEquals(Correction.NONE, Correction.fromString("gibberish"));
    }
    
    @Test
    public void testFromStringTruncatedInput() {
        final String noneString = Correction.NONE.toString();
        final String truncatedString = noneString.substring(0,
                noneString.lastIndexOf(" "));
        assertEquals(Correction.NONE, Correction.fromString(truncatedString));
    }

}
