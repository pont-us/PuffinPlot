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
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author pont
 */
public class CoreSectionsTest {

    private List<Sample> sampleList;
    
    @Before
    public void setUp() {
        sampleList = CoreSectionTest.makeSampleList();
    }
    
    @Test
    public void testSplitByDiscreteId() {
        final CoreSections sections = CoreSections.fromSampleListByDiscreteId(sampleList);
        final List<String> expectedPartition = Arrays.asList("0,1,2", "3,4,5", "6,7,8", "9");
        final List<String> actualPartition = new ArrayList<>();
        for (CoreSection section: sections.getSections().values()) {
            final String actualSampleDepths = section.getSamples().stream().map(Sample::getNameOrDepth).collect(Collectors.joining(","));
            actualPartition.add(actualSampleDepths);
        }
            assertEquals(expectedPartition, actualPartition);
    }
    
    
    
    
}
