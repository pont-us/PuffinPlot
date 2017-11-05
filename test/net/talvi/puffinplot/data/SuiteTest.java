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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.talvi.puffinplot.PuffinUserException;
import net.talvi.puffinplot.data.file.IapdLoaderTest;
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
public class SuiteTest {
    
    private File file;
    private Suite suite;
    
    final String FILE_TEXT = "PuffinPlot file. Version 3\n"
            + "DISCRETE_ID	DEPTH	RUN_NUMBER	TIMESTAMP	SLOT_NUMBER	MEAS_TYPE	X_MOMENT	Y_MOMENT	Z_MOMENT	MAG_SUS	VOLUME	AREA	SAMPLE_AZ	SAMPLE_DIP	FORM_AZ	FORM_DIP	MAG_DEV	TREATMENT	AF_X	AF_Y	AF_Z	TEMPERATURE	IRM_FIELD	ARM_FIELD	ARM_AXIS	PP_SELECTED	PP_ANCHOR_PCA	PP_HIDDEN	PP_ONCIRCLE	PP_INPCA\n"
            + "31X-1W-143	null	4990	11/03/20 1327	-1	DISCRETE	-1.2249E-4	1.5351428571428572E-4	-3.520428571428571E-4	NaN	7.0	4.0	0.0	0.0	0.0	0.0	0.0	NONE	0.0	0.0	0.0	NaN	NaN	NaN	NONE	false	true	false	false	false\n"
            + "31X-1W-27.5	null	4988	11/03/20 1327	-1	DISCRETE	3.5331428571428574E-5	5.914714285714286E-4	-7.528142857142857E-4	NaN	7.0	4.0	0.0	0.0	0.0	0.0	0.0	NONE	0.0	0.0	0.0	NaN	NaN	NaN	NONE	false	true	false	false	false\n"
            + "31X-1W-27.5	null	4989	11/03/20 1327	-1	DISCRETE	9.302285714285713E-5	2.9701428571428574E-4	-4.464E-4	NaN	7.0	4.0	0.0	0.0	0.0	0.0	0.0	DEGAUSS_XYZ	0.005	0.005	0.005	NaN	NaN	NaN	NONE	false	true	false	false	false\n"
            + "31X-1W-27.5	null	4990	11/03/20 1327	-1	DISCRETE	8.297285714285714E-5	1.6674285714285715E-4	-3.347E-4	NaN	7.0	4.0	0.0	0.0	0.0	0.0	0.0	DEGAUSS_XYZ	0.01	0.01	0.01	NaN	NaN	NaN	NONE	false	true	false	false	false\n"
            + "\nSUITE	MEASUREMENT_TYPE	DISCRETE\n"
            + "SUITE	CREATION_DATE	2015-03-16T20:24:37.638+01:00\n"
            + "SUITE	MODIFICATION_DATE	2015-03-16T20:24:37.638+01:00\n"
            + "SUITE	ORIGINAL_FILE_TYPE	TWOGEE\n"
            + "SUITE	ORIGINAL_CREATOR_PROGRAM	PuffinPlot 6c33364b465f\n"
            + "SUITE	SAVED_BY_PROGRAM	PuffinPlot 6c33364b465f\n";
    
    public SuiteTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        // Create an empty temporary file.
        file = null;
        try {
            file = File.createTempFile("puffinplot-test-", ".ppl");
            //file.deleteOnExit();
        } catch (IOException ex) {
            Logger.getLogger(IapdLoaderTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Error creating temporary file.");
        }
        
        // Write our data into it.
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(FILE_TEXT);
        } catch (IOException ex) {
            Logger.getLogger(IapdLoaderTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Error writing data to temporary file.");
        }
        createSuite();
    }
    
    private void createSuite() {
        suite = new Suite("SuiteTest");
        for (int depth=0; depth<10; depth++) {
            String depthString = String.format("%d", depth);
            Sample sample = new Sample(depthString, suite);
            for (int demag=0; demag<100; demag += 10) {
                final Datum d = new Datum((depth+1.)*(100.-demag), 0, 0);
                d.setDepth(depthString);
                d.setSuite(suite);
                d.setMeasType(MeasType.CONTINUOUS);
                d.setAfX(demag);
                d.setAfY(demag);
                d.setAfZ(demag);
                d.setTreatType(TreatType.DEGAUSS_XYZ);
                d.setSample(sample);
                sample.addDatum(d);
                suite.addDatum(d);
            }
        }
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testConvertDiscreteToContinuous() {
        final Map<String, String> nameToDepth = new HashMap<>();
        nameToDepth.put("31X-1W-143", "3.14");
        nameToDepth.put("31X-1W-27.5", "5.67");
        Suite suite = new Suite("testConvertDiscreteToContinuous");
        final File[] files = {file};
        try {
            suite.readFiles(Arrays.asList(files));
        } catch (IOException ex) {
            Logger.getLogger(SuiteTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Error reading file.");
        }
        try {
            suite.convertDiscreteToContinuous(nameToDepth);
        } catch (Suite.MissingSampleNameException ex) {
            Logger.getLogger(SuiteTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
        
        assertEquals(MeasType.CONTINUOUS, suite.getMeasType());
        assertNotNull(suite.getSampleByName("3.14"));
        assertNotNull(suite.getSampleByName("5.67"));
    }
    
    @Test
    public void testAmsCalcType() {
        assertSame(Suite.AmsCalcType.HEXT,
                Suite.AmsCalcType.valueOf("HEXT"));
        assertSame(Suite.AmsCalcType.BOOT,
                Suite.AmsCalcType.valueOf("BOOT"));
        assertSame(Suite.AmsCalcType.PARA_BOOT,
                Suite.AmsCalcType.valueOf("PARA_BOOT"));
    }
    
    @Test
    public void testMissingSampleNameException() {
        final Suite suite = new Suite("SuiteTest");
        suite.new MissingSampleNameException("test");
    }
    
    @Test
    public void testSaveCalcsSample() {
        try {
            for (Sample sample: suite.getSamples()) {
                sample.getData().stream().forEach(d -> d.setSelected(true));
                sample.useSelectionForPca();
                sample.useSelectionForCircleFit();
                sample.doPca(Correction.NONE);
                sample.fitGreatCircle(Correction.NONE);
                sample.calculateMdf();
            }
            final File csvFile = File.createTempFile("puffinplot-test-", ".ppl");
            suite.saveCalcsSample(csvFile);
            suite.saveCalcsSample(new File("/home/pont/test.csv"));
            List<String> lines = Files.readAllLines(csvFile.toPath());
            // Check that header line is correct
            assertEquals("Suite,Depth,NRM intensity (A/m),"+
                    "MS jump temp. (degC),Steps,PCA dec. (deg),"+
                    "PCA inc. (deg),PCA MAD1,PCA MAD3,PCA anchored,"+
                    "PCA equation,PCA npoints,PCA start (degC or mT),"+
                    "PCA end (degC or mT),PCA contiguous,GC dec (deg),"+
                    "GC inc (deg),GC strike (deg),GC dip (deg),GC MAD1,"+
                    "GC npoints,MDF half-intensity (A/m),"+
                    "MDF demagnetization (degC or T),MDF midpoint reached,"+
                    "Fisher dec. (deg),Fisher inc. (deg),Fisher a95 (deg),"+
                    "Fisher k,Fisher nDirs,Fisher R,AMS dec1,AMS inc1,"+
                    "AMS dec2,AMS inc2,AMS dec3,AMS inc3",
                    lines.get(0));
            assertEquals(suite.getSamples().size()+1, lines.size());
            // Check that lines have right number of fields
            for (String line: lines) {
                assertEquals(35, line.chars().filter(c -> c == ',').count());
            }
            
        } catch (PuffinUserException | IOException ex) {
            Logger.getLogger(SuiteTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
    }
}
