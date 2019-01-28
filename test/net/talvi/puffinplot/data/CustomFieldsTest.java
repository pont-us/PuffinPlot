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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CustomFieldsTest {

    private CustomFields<String> testFields0;

    @Before
    public void setUp() {
        testFields0 = new CustomFields<>();
        testFields0.add(0, "zero");
        testFields0.add(1, "one");
        testFields0.add(2, "two");        
    }
    
    @Test
    public void testNoArgumentConstructor() {
        assertEquals(Collections.emptyList(),
                new CustomFields<String>().toStrings());
    }
    
    @Test
    public void testListConstructor() {
        final List<String> list = Arrays.asList("celery", "apples",
                "walnuts", "grapes");
        assertEquals(list, new CustomFields<String>(list).toStrings());
    }
    
    @Test
    public void testSet() {
        testFields0.set(0, "jam");
        assertEquals(Arrays.asList("jam", "one", "two"),
                testFields0.toStrings());
        testFields0.set(1, "bees");
        assertEquals(Arrays.asList("jam", "bees", "two"),
                testFields0.toStrings());        
    }

    @Test
    public void testGet() {
        assertEquals("zero", testFields0.get(0));
        assertEquals("one", testFields0.get(1));
        assertEquals("two", testFields0.get(2));
    }

    @Test
    public void testSetSize() {
        final int nCopies = 23;
        final String fillValue = "bees";
        testFields0.setSize(nCopies, fillValue);
        assertEquals(Collections.nCopies(nCopies, fillValue),
                testFields0.toStrings());
    }

    @Test
    public void testAdd() {
        final String newValue = "one-half";
        testFields0.add(1, newValue);
        assertEquals(Arrays.asList("zero", newValue, "one", "two"),
                testFields0.toStrings());
    }

    @Test
    public void testRemove() {
        testFields0.remove(1);
        assertEquals(Arrays.asList("zero", "two"), testFields0.toStrings());
    }

    @Test
    public void testSwapAdjacent() {
        testFields0.swapAdjacent(0);
        assertEquals(Arrays.asList("one", "zero", "two"),
                testFields0.toStrings());
    }

    @Test
    public void testExportAsString() {
        assertEquals("zero\tone\ttwo", testFields0.exportAsString());
    }

    @Test
    public void testToStrings() {
        assertEquals(Arrays.asList("zero", "one", "two"),
                testFields0.toStrings());
    }

    @Test
    public void testSize() {
        assertEquals(3, testFields0.size());
    }
    
}
