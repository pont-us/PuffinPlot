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

import net.talvi.puffinplot.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SuiteCalcsTest {

    List<Vec3> upperDirs;
    List<Vec3> lowerDirs;
    List<Vec3> allDirs;
    
    @Before
    public void setUp() {
        upperDirs = TestUtils.makeVectorList(new double[][] {
            {0, 8, -9}, {0, 9, -8}, {1, 8, -9}, {1, 9, -8}
        }, true);
        lowerDirs = TestUtils.makeVectorList(new double[][] {
            {0, 8, 9}, {0, 9, 8}, {1, 8, 9}, {1, 9, 8}
        }, true);
        allDirs = new ArrayList<>();
        allDirs.addAll(upperDirs);
        allDirs.addAll(lowerDirs);
    }
    
    @Test
    public void testMeansCalculate() {
        final SuiteCalcs.Means means =
                SuiteCalcs.Means.calculate(allDirs);
        assertEquals(FisherValues.calculate(upperDirs).toStrings(),
                means.getUpper().toStrings());
        assertEquals(FisherValues.calculate(lowerDirs).toStrings(),
                means.getLower().toStrings());
        assertEquals(FisherValues.calculate(allDirs).toStrings(),
                means.getAll().toStrings());
    }
    
    @Test
    public void testConstructorAndGetters() {

        /*
         * The directions are meaningless in terms of samples, sites,
         * and VGPs: we just need four distinct groups to make sure that
         * the results are correctly placed in the output string matrix.
         * This is a characterization test: the expected output was
         * generated from SuiteCalcs itself.
         */

        final SuiteCalcs.Means dirsBySite =
                SuiteCalcs.Means.calculate(allDirs);
        final SuiteCalcs.Means dirsBySample =
                SuiteCalcs.Means.calculate(rotateAroundZAxis(allDirs, 1));
        final SuiteCalcs.Means vgpsBySite =
                SuiteCalcs.Means.calculate(rotateAroundZAxis(allDirs, 2));
        final SuiteCalcs.Means vgpsBySample =
                SuiteCalcs.Means.calculate(rotateAroundZAxis(allDirs, 3));

        final SuiteCalcs suiteCalcs = new SuiteCalcs(dirsBySite, dirsBySample,
                vgpsBySite, vgpsBySample);
        assertEquals(dirsBySite, suiteCalcs.getDirsBySite());
        assertEquals(dirsBySample, suiteCalcs.getDirsBySample());
        assertEquals(vgpsBySite, suiteCalcs.getVgpsBySite());
        assertEquals(vgpsBySample, suiteCalcs.getVgpsBySample());
        assertEquals("Site dir,All,86.6393,0.0000,38.9719,2.9751,8,5.6471\n" +
                "Site dir,Upper,86.6393,-44.9507,5.3971,290.7890,4,3.9897\n" +
                "Site dir,Lower,86.6393,44.9507,5.3971,290.7890,4,3.9897\n" +
                "Sample dir,All,143.9351,0.0000,38.9719,2.9751,8,5.6471\n" +
                "Sample dir,Upper,143.9351,-44.9507,5.3971,290.7890,4,3.9897\n"+
                "Sample dir,Lower,143.9351,44.9507,5.3971,290.7890,4,3.9897\n" +
                "Site VGP,All,201.2309,-0.0000,38.9719,2.9751,8,5.6471\n" +
                "Site VGP,Upper,201.2309,-44.9507,5.3971,290.7890,4,3.9897\n" +
                "Site VGP,Lower,201.2309,44.9507,5.3971,290.7890,4,3.9897\n" +
                "Sample VGP,All,258.5266,-0.0000,38.9719,2.9751,8,5.6471\n" +
                "Sample VGP,Upper,258.5266,-44.9507,5.3971,290.7890,4,3.9897\n"+
                "Sample VGP,Lower,258.5266,44.9507,5.3971,290.7890,4,3.9897",
                suiteCalcs.toStrings().stream().
                map(list -> list.stream().collect(Collectors.joining(","))).
                collect(Collectors.joining("\n")));
    }
    
    private static List<Vec3> rotateAroundZAxis(List<Vec3> vs, double angle) {
        return vs.stream().map(v -> v.rotZ(angle)).collect(Collectors.toList());
    }

    @Test
    public void testGetHeaders() {
        assertEquals(Arrays.asList("Type", "Group", "Fisher dec. (deg)",
                "Fisher inc. (deg)", "Fisher a95 (deg)", "Fisher k",
                "Fisher nDirs", "Fisher R"),
                SuiteCalcs.getHeaders());
    }
    
}
