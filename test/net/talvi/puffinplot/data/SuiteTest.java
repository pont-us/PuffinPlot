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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.talvi.puffinplot.PuffinUserException;
import net.talvi.puffinplot.data.file.IapdLoaderTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import net.talvi.puffinplot.TestUtils;

/**
 *
 * @author pont
 */
public class SuiteTest {
    
    private File puffinFile1;
    private Suite syntheticSuite1;
    
    final String FILE_TEXT = "PuffinPlot file. Version 3\n"
            + "DISCRETE_ID	DEPTH	RUN_NUMBER	TIMESTAMP	SLOT_NUMBER	MEAS_TYPE	X_MOMENT	Y_MOMENT	Z_MOMENT	MAG_SUS	VOLUME	AREA	SAMPLE_AZ	SAMPLE_DIP	FORM_AZ	FORM_DIP	MAG_DEV	TREATMENT	AF_X	AF_Y	AF_Z	TEMPERATURE	IRM_FIELD	ARM_FIELD	ARM_AXIS	PP_SELECTED	PP_ANCHOR_PCA	PP_HIDDEN	PP_ONCIRCLE	PP_INPCA\n"
            + "31X-1W-143	0	4990	11/03/20 1327	-1	DISCRETE	-1.2249E-4	1.5351428571428572E-4	-3.520428571428571E-4	NaN	7.0	4.0	0.0	0.0	0.0	0.0	0.0	NONE	0.0	0.0	0.0	NaN	NaN	NaN	NONE	false	true	false	false	false\n"
            + "31X-1W-27.5	0	4988	11/03/20 1327	-1	DISCRETE	3.5331428571428574E-5	5.914714285714286E-4	-7.528142857142857E-4	NaN	7.0	4.0	0.0	0.0	0.0	0.0	0.0	NONE	0.0	0.0	0.0	NaN	NaN	NaN	NONE	false	true	false	false	false\n"
            + "31X-1W-27.5	0	4989	11/03/20 1327	-1	DISCRETE	9.302285714285713E-5	2.9701428571428574E-4	-4.464E-4	NaN	7.0	4.0	0.0	0.0	0.0	0.0	0.0	DEGAUSS_XYZ	0.005	0.005	0.005	NaN	NaN	NaN	NONE	false	true	false	false	false\n"
            + "31X-1W-27.5	0	4990	11/03/20 1327	-1	DISCRETE	8.297285714285714E-5	1.6674285714285715E-4	-3.347E-4	NaN	7.0	4.0	0.0	0.0	0.0	0.0	0.0	DEGAUSS_XYZ	0.01	0.01	0.01	NaN	NaN	NaN	NONE	false	true	false	false	false\n"
            + "\nSUITE	MEASUREMENT_TYPE	DISCRETE\n"
            + "SUITE	CREATION_DATE	2015-03-16T20:24:37.638+01:00\n"
            + "SUITE	MODIFICATION_DATE	2015-03-16T20:24:37.638+01:00\n"
            + "SUITE	ORIGINAL_FILE_TYPE	TWOGEE\n"
            + "SUITE	ORIGINAL_CREATOR_PROGRAM	PuffinPlot 6c33364b465f\n"
            + "SUITE	SAVED_BY_PROGRAM	PuffinPlot 6c33364b465f\n";
    
    @Before
    public void setUp() {
        // Create an empty temporary file.
        puffinFile1 = null;
        try {
            puffinFile1 = File.createTempFile("puffinplot-test-", ".ppl");
            //file.deleteOnExit();
        } catch (IOException ex) {
            Logger.getLogger(IapdLoaderTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Error creating temporary file.");
        }
        
        // Write our data into it.
        try (FileWriter fw = new FileWriter(puffinFile1)) {
            fw.write(FILE_TEXT);
        } catch (IOException ex) {
            Logger.getLogger(IapdLoaderTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Error writing data to temporary file.");
        }
        
        createSuite();
    }
    
    private void createSuite() {
        syntheticSuite1 = new Suite("SuiteTest");
        for (int depth=0; depth<10; depth++) {
            String depthString = String.format("%d", depth);
            Sample sample = new Sample(depthString, syntheticSuite1);
            for (int demag=0; demag<100; demag += 10) {
                final Datum d = new Datum((depth+1.)*(100.-demag), 0, 0);
                d.setDepth(depthString);
                d.setSuite(syntheticSuite1);
                d.setMeasType(MeasType.CONTINUOUS);
                d.setAfX(demag);
                d.setAfY(demag);
                d.setAfZ(demag);
                d.setTreatType(TreatType.DEGAUSS_XYZ);
                d.setSample(sample);
                sample.addDatum(d);
                syntheticSuite1.addDatum(d);
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
        Suite suiteFromFile = new Suite("testConvertDiscreteToContinuous");
        loadFileDataIntoSuite(puffinFile1, suiteFromFile);
        try {
            suiteFromFile.convertDiscreteToContinuous(nameToDepth);
        } catch (Suite.MissingSampleNameException ex) {
            Logger.getLogger(SuiteTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
        
        assertEquals(MeasType.CONTINUOUS, suiteFromFile.getMeasType());
        assertNotNull(suiteFromFile.getSampleByName("3.14"));
        assertNotNull(suiteFromFile.getSampleByName("5.67"));
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
            for (Sample sample: syntheticSuite1.getSamples()) {
                sample.getData().stream().forEach(d -> d.setSelected(true));
                sample.useSelectionForPca();
                sample.useSelectionForCircleFit();
                sample.doPca(Correction.NONE);
                sample.fitGreatCircle(Correction.NONE);
                sample.calculateMdf();
            }
            final File csvFile = File.createTempFile("puffinplot-test-", ".ppl");
            syntheticSuite1.saveCalcsSample(csvFile);
            syntheticSuite1.saveCalcsSample(new File("/home/pont/test.csv"));
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
            assertEquals(syntheticSuite1.getSamples().size()+1, lines.size());
            // Check that lines have right number of fields
            for (String line: lines) {
                assertEquals(35, line.chars().filter(c -> c == ',').count());
            }
            
        } catch (PuffinUserException | IOException ex) {
            Logger.getLogger(SuiteTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
    }
    
    /**
     * Test that loading a suite from a file and saving it produces
     * an identical file (except for the SAVED_BY_PROGRAM field, which
     * is expected to change).
     */
    @Test
    public void testLoadAndSaveSuite() {
        final DateFormat iso8601format =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        final Suite suite = new Suite("testLoadAndSaveSuite");
        loadFileDataIntoSuite(puffinFile1, suite);
        final File savedFile = saveSuiteToTempFile(suite);
        try {
            suite.saveAs(savedFile);
        } catch (PuffinUserException ex) {
            Logger.getLogger(SuiteTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Exception saving suite");
        }
        try (FileReader reader = new FileReader(savedFile);
                BufferedReader br = new BufferedReader(reader)) {
            for (String expectedLine: FILE_TEXT.split("\n")) {
                final String actualLine = br.readLine();
                if (expectedLine.contains("SAVED_BY_PROGRAM")) {
                    // this isn't expected to match
                } else if (expectedLine.contains("CREATION_DATE") ||
                        expectedLine.contains("MODIFICATION_DATE")) {
                    // The actual datestamp should match, but the string
                    // representation may differ since it's expressed in
                    // local time with a timezone suffix.
                    assertEquals(iso8601format.parse(expectedLine.split("\t")[2]),
                            iso8601format.parse(actualLine.split("\t")[2]));
                } else {
                    assertEquals(expectedLine, actualLine);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(SuiteTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Exception loading suite");
        } catch (ParseException ex) {
            Logger.getLogger(SuiteTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Exception parsing date");
        }
    }
    
    /**
     * Test that saving a suite, then loading it from the saved file produces
     * an identical suite.
     * 
     */
    @Test
    public void testSaveAndLoadSuite() {
        final File savedFile = saveSuiteToTempFile(syntheticSuite1);
        final Suite loadedSuite = new Suite("testSaveAndLoadSuite");
        loadFileDataIntoSuite(savedFile, loadedSuite);
        final Iterator<Sample> actualSamples =
                loadedSuite.getSamples().iterator();
        for (Sample expectedSample: syntheticSuite1.getSamples()) {
            final Sample actualSample = actualSamples.next();
            final Iterator<Datum> actualData =
                    actualSample.getData().iterator();
            for (Datum expectedDatum: expectedSample.getData()) {
                final Datum actualDatum = actualData.next();
                assertTrue(expectedDatum.getMoment().equals(actualDatum.getMoment()));
                assertEquals(expectedDatum.getTreatType(),
                        actualDatum.getTreatType());
                assertEquals(expectedDatum.getIdOrDepth(), actualDatum.getIdOrDepth());
                assertEquals(expectedDatum.getMeasType(), actualDatum.getMeasType());
                assertEquals(expectedDatum.getAfX(), actualDatum.getAfX(), 0.0001);
            }
        }
    }
    
    private void loadFileDataIntoSuite(File file, Suite suiteFromFile) {
        final File[] files = {file};
        try {
            suiteFromFile.readFiles(Arrays.asList(files));
        } catch (IOException ex) {
            Logger.getLogger(SuiteTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Error reading file.");
        }
    }
    
    private File saveSuiteToTempFile(Suite suite) {
        try {
            final File savedFile = File.createTempFile("puffinplot-test-", ".ppl");
            savedFile.delete();
            suite.saveAs(savedFile);
            return savedFile;
        } catch (IOException | PuffinUserException ex) {
            Logger.getLogger(SuiteTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Exception creating temporary file.");
            return null;
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testReadFilesEmptyList() {
        final Suite suite = new Suite("test");
        try {
            suite.readFiles(Collections.emptyList());
        } catch (IOException ex) {
            Logger.getLogger(SuiteTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
    }
    
    @Test(expected = NullPointerException.class)
    public void testReadFilesNullList() {
        try {
            new Suite("test").readFiles(null);
        } catch (IOException ex) {
            Logger.getLogger(SuiteTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
    }
    
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    @Test
    public void testNonExistentFile() {
        Path path;
        final File file =
                folder.getRoot().toPath().resolve("nonexistent").toFile();
        try {
            Suite suite = new Suite("test");
            suite.readFiles(Collections.singletonList(file));
            assertTrue(suite.isEmpty());
            assertEquals(1, suite.getLoadWarnings().size());
        } catch (IOException ex) {
            Logger.getLogger(SuiteTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
    }
    
    @Test
    public void testCalculateSuiteMeans() {
        final List<Vec3> testData = TestUtils.makeVectorList(new double[][] {
            {1, 2, 3},
            {1, 2, 4},
            {4, 5, -6},
            {4, 5, -7},
            {1, 2, 5},
            {1, 3, 6},
            {2, 3, -6},
            {2, 4, -7},
            {1, 1, 1},
            {1, 2, 1},
            {2, 1, -2},
            {1, 2, -2},
            {3, 2, 2},
            {4, 2, 2},
            {3, 2, -1},
            {4, 2, -1}
        }, true);
        // (might need more samples to get a meaningful result for each site)

        final List<Site> sites = new ArrayList<>();
        final double[][] siteLocations = {
            {45, 45},
            {-45, 45},
            {30, 50},
            {-30, -50}
        };
        final Suite suite = new Suite("test");

        for (int siteIndex = 0; siteIndex < siteLocations.length; siteIndex++) {
            final Site site = suite.getOrCreateSite(Integer.toString(siteIndex));
            site.setLocation(Location.fromDegrees(siteLocations[siteIndex][0],
                    siteLocations[siteIndex][1]));
            sites.add(site);
        }
        
        final List<Sample> samples = new ArrayList<>();
        for (int i=0; i<testData.size(); i++) {
            final Sample sample = new Sample(String.format("%d", i), suite);
            sample.setImportedDirection(testData.get(i));
            final Site site = sites.get(i % siteLocations.length);
            sample.setSite(site);
            site.addSample(sample);
            samples.add(sample);
            suite.addSample(sample, Integer.toString(i));
        }
        
        suite.doAllCalculations(Correction.NONE, "true");
        for (Site site: sites) {
            site.calculateFisherStats(Correction.NONE);
        }
        suite.calculateSuiteMeans(suite.getSamples(), suite.getSites());
        
        final SuiteCalcs.Means dirsBySample =
                SuiteCalcs.Means.calculate(testData);
        final SuiteCalcs.Means vgpsBySample =
                SuiteCalcs.Means.calculate(samples.stream().map(
                        (s) -> VGP.calculate(s.getDirection(), 0,
                                s.getSite().getLocation()).getLocation().
                                toVec3()).collect(Collectors.toList()));
        final SuiteCalcs.Means dirsBySite =
                SuiteCalcs.Means.calculate(sites.stream().map((s) ->
                s.getMeanDirection()).collect(Collectors.toList()));
        final SuiteCalcs.Means vgpsBySite =
                SuiteCalcs.Means.calculate(sites.stream().map((s) ->
                VGP.calculate(s.getMeanDirection(), 0, s.getLocation()).
                        getLocation().toVec3()).collect(Collectors.toList()));
                
        // Calculate directly using SuiteMeans. We're not checking the
        // maths here; that should happen in the SuiteMeans unit tests
        // (or maybe even lower down).
        final SuiteCalcs expectedCalcs = new SuiteCalcs(dirsBySite,
                dirsBySample, vgpsBySite, vgpsBySample);
        
        assertEquals(expectedCalcs.toStrings(),
                suite.getSuiteMeans().toStrings());
        }
}
