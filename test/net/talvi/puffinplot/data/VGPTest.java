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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for the VGP class.
 * 
 * Uses the worked example from Butler (1992).
 * 
 * @author pont
 */
public class VGPTest {

    /**
     * Test of calculate method, of class VGP.
     */
    @Test
    public void testCalculate() {
        System.out.println("calculate");
        Vec3 direction = Vec3.fromPolarDegrees(1, 45, 25);
        double a95 = 5.0;
        final Location loc = Location.fromDegrees(30, 250);
        final VGP vgp = VGP.calculate(direction, a95, loc);
        final double delta = 0.1;
        assertEquals(67.8, vgp.getLocation().getLatDeg(), delta);
        assertEquals(342.7, vgp.getLocation().getLongDeg(), delta);
        assertEquals(4.0, vgp.getDp(), delta);
        assertEquals(6.3, vgp.getDm(), delta);
    }

}
