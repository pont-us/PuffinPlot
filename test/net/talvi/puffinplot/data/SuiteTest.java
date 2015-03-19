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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of convertDiscreteToContinuous method, of class Suite.
     */
    @Test
    public void testConvertDiscreteToContinuous() {
        System.out.println("convertDiscreteToContinuous");

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
        suite.convertDiscreteToContinuous(nameToDepth);
        
        assertEquals(MeasType.CONTINUOUS, suite.getMeasType());
        assertNotNull(suite.getSampleByName("3.14"));
        assertNotNull(suite.getSampleByName("5.67"));
    }
}
