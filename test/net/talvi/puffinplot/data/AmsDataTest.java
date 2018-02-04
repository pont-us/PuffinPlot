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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author pont
 */
@RunWith(Parameterized.class)
public class AmsDataTest {
    
    private final String expectedName;
    private final double[] expectedTensor;
    private final double expectedSampleAz, expectedSampleDip;
    private final double expectedFTest;
    private final AmsData amsData;
    
    public AmsDataTest(String name, List<Double> tensorList, double sampleAz,
            double sampleDip, double fTest) {
        final double[] tensor =
                tensorList.stream().mapToDouble(Double::doubleValue).toArray();
        this.expectedName = name;
        this.expectedTensor = tensor;
        this.expectedSampleAz = sampleAz;
        this.expectedSampleDip = sampleDip;
        this.expectedFTest = fTest;
        this.amsData = new AmsData(name, tensor, sampleAz, sampleDip, fTest);
    }
    
    private static List<Double> listOf(double x0, double x1, double x2,
            double x3, double x4, double x5) {
        return Arrays.asList(new Double[] {x0, x1, x2, x3, x4, x5});
    }
    
    @Parameters
    public static Collection<Object[]> data() {
        final int nTests = 10;
        final long seed = 17;
        final Random rnd = new Random(seed);
        
        final Object[][] dataArray = new Object[nTests][];
        
        for (int i=0; i<nTests; i++) {
            dataArray[i] = new Object[] {
              "test"+i,
               listOf(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(),
                      rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble()),
               rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble()
            };
        }
        
        return Arrays.asList(dataArray);
    }

    @Test
    public void testGetName() {
        assertEquals(expectedName, amsData.getName());
    }

    @Test
    public void testGetTensor() {
        assertArrayEquals(expectedTensor, amsData.getTensor(), 0);
    }

    @Test
    public void testGetSampleAz() {
        assertEquals(expectedSampleAz, amsData.getSampleAz(), 0);
    }

    @Test
    public void testGetSampleDip() {
        assertEquals(expectedSampleDip, amsData.getSampleDip(), 0);
    }

    @Test
    public void testGetfTest() {
        assertEquals(expectedFTest, amsData.getfTest(), 0);
    }
}
