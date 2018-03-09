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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class RpiDatasetTest {
    
    @Test
    public void testCalculateWithMagSus() {
        final Suite nrmSuite = new Suite("test");
        nrmSuite.addDatum(makeDatum(0.005, TreatType.NONE, 0, 0));
        final Suite msSuite = new Suite("test");
        msSuite.addDatum(makeDatum(0, TreatType.NONE, 0, 10));
        final RpiDataset result = RpiDataset.calculateWithMagSus(nrmSuite, msSuite);
        assertEquals(0.0005, result.getRpis().get(0).getSlope(), 1e-8);
        assertEquals(0.0005, result.getRpis().get(0).getMeanRatio(), 1e-8);
        assertEquals(1, result.getTreatmentLevels().size());
        assertEquals(0, result.getTreatmentLevels().get(0), 1e-8);
        checkWrittenFile(
                "Depth,0.00000, mean ratio, slope, r, r-squared, ARM\n" +
                "0,0.000500000,0.000500000,0.000500000,0.00000,0.00000,10.0000\n",
                result);
    }

    @Test
    public void testCalculateWithArm() {

        /* We fake up very simple NRM and ARM suites (one sample with two
         * steps each) and compare the calculated RPI estimates with
         * known, hand-calculated values.
         */
        
        final Suite nrmSuite = new Suite("test");
        nrmSuite.addDatum(makeDatum(0.005, TreatType.NONE, 0, 0));
        nrmSuite.addDatum(makeDatum(0.0032, TreatType.DEGAUSS_XYZ, 0.02, 0));
        
        final Suite armSuite = new Suite("test");
        armSuite.addDatum(makeDatum(0.1, TreatType.ARM, 0.1, 0));
        armSuite.addDatum(makeDatum(0.08, TreatType.DEGAUSS_XYZ, 0.02, 0));
        
        final RpiDataset result =
                RpiDataset.calculateWithArm(nrmSuite, armSuite, 0, 1);
        assertEquals(0.05, result.getRpis().get(0).getIntensities().get(0),
                1e-8);
        assertEquals(0.04, result.getRpis().get(0).getIntensities().get(1),
                1e-8);
        
        // The line defined by the two ratios doesn't trend anywhere near
        // the origin, which is why the two estimates are so different.
        
        assertEquals(0.045, result.getRpis().get(0).getMeanRatio(), 1e-8);
        assertEquals(0.09, result.getRpis().get(0).getSlope(), 1e-8);
        assertEquals(1.00, result.getRpis().get(0).getR(), 1e-8);
        assertEquals(1.00, result.getRpis().get(0).getrSquared(), 1e-8);
        checkWrittenFile(
                "Depth,0.00000,0.0200000, mean ratio, slope, r, r-squared, ARM\n" +
                "0,0.0500000,0.0400000,0.0450000,0.0900000,1.00000,1.00000,0.100000\n"
                , result);

    }

    private static Datum makeDatum(double magnetization,
            TreatType treatmentType, double afIntensity,
            double susceptibility) {
        final Datum datum = new Datum(0, 0, magnetization);
        datum.setTreatType(treatmentType);
        datum.setMeasType(MeasType.CONTINUOUS);
        datum.setAfX(afIntensity);
        datum.setAfY(afIntensity);
        datum.setAfZ(afIntensity);
        datum.setMagSus(susceptibility);
        datum.setDepth("0");
        return datum;
    }
    
    private static void checkWrittenFile(String expected, RpiDataset rpis) {
        try {
            final Path tempDir = Files.createTempDirectory("puffintest");
            final Path tempFile = tempDir.resolve(Paths.get("rpifile"));
            rpis.writeToFile(tempFile.toString());
            final String actual = new String(Files.readAllBytes(tempFile));
            assertEquals(expected, actual);
        } catch (IOException ex) {
            Logger.getLogger(RpiDatasetTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
        
    }
    
}
