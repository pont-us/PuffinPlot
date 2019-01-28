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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.talvi.puffinplot.PuffinUserException;
import net.talvi.puffinplot.TestUtils;
import net.talvi.puffinplot.TestUtils.ListHandler;
import net.talvi.puffinplot.data.file.TwoGeeLoader;
import net.talvi.puffinplot.data.file.testdata.TestFileLocator;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author pont
 */
public class SuiteTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    final private static double delta = 1e-10;
    
    private File puffinFile1;
    private Suite syntheticSuite1;
    private Suite syntheticSuite2;
    
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
            puffinFile1 = temporaryFolder.newFile("puffinplot-test.ppl");
        } catch (IOException ex) {
            Logger.getLogger(SuiteTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Error creating temporary file.");
        }
        
        // Write our data into it.
        try (FileWriter fw = new FileWriter(puffinFile1)) {
            fw.write(FILE_TEXT);
        } catch (IOException ex) {
            Logger.getLogger(SuiteTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Error writing data to temporary file.");
        }
        
        createSuites();
    }
    
    private void createSuites() {
        syntheticSuite1 = TestUtils.createContinuousSuite();
        syntheticSuite2 = TestUtils.createDiscreteSuite();
    }
    
    @Test
    public void testConvertDiscreteToContinuousWithMap()
            throws Suite.MissingSampleNameException, IOException {
        final Map<String, String> nameToDepth = new HashMap<>();
        nameToDepth.put("31X-1W-143", "3.14");
        nameToDepth.put("31X-1W-27.5", "5.67");
        Suite suiteFromFile = new Suite("testConvertDiscreteToContinuous");
        loadFileDataIntoSuite(puffinFile1, suiteFromFile);
            suiteFromFile.convertDiscreteToContinuous(nameToDepth);
        
        assertEquals(MeasurementType.CONTINUOUS, suiteFromFile.getMeasurementType());
        assertNotNull(suiteFromFile.getSampleByName("3.14"));
        assertNotNull(suiteFromFile.getSampleByName("5.67"));
    }
    
    @Test
    public void testConvertDiscreteToContinuousWithFile()
            throws IOException, Suite.MissingSampleNameException {
        // SAMPLE_%d
        final File depthFile = TestUtils.writeStringToTemporaryFile(
                "depths.txt",
                "SAMPLE_0, 50\nSAMPLE_1, 51\n" +
                        "SAMPLE_2, 52\nSAMPLE_3, 53\n" +
                        "SAMPLE_4, 54\nSAMPLE_5, 55\n" +
                        "SAMPLE_6, 56\nSAMPLE_7, 57\n" +
                        "SAMPLE_8, 58\nSAMPLE_9, 59\n",
                temporaryFolder);
        syntheticSuite2.convertDiscreteToContinuous(depthFile);
        assertEquals("50,51,52,53,54,55,56,57,58,59",
                syntheticSuite2.getSamples().stream().
                        map(sample -> sample.getNameOrDepth()).
                        collect(Collectors.joining(",")));
    }

    @Test(expected = IllegalStateException.class)
    public void testConvertDiscreteToContinuousOnContinuousSuite()
            throws Suite.MissingSampleNameException {
        /*
         * We supply a valid map to make sure that the exception is triggered
         * by the suite being continuous, not by the map being incomplete.
         */
        final Map<String, String> nameToDepth =
                syntheticSuite1.getSamples().stream().
                        map(sample -> sample.getNameOrDepth()).
                        collect(Collectors.toMap(x -> x, x -> x));
        syntheticSuite1.convertDiscreteToContinuous(nameToDepth);
    }
    
    @Test(expected = Suite.MissingSampleNameException.class)
    public void testConvertDiscreteToContinuousWithIncompleteMap()
            throws Suite.MissingSampleNameException {
        final Map<String, String> nameToDepth = new HashMap<>();
        final int omittedSampleIndex = 2;
        for (int sampleIndex=0; sampleIndex < syntheticSuite2.getNumSamples();
                sampleIndex++) {
            if (sampleIndex != omittedSampleIndex) {
                nameToDepth.put(syntheticSuite2.getSampleByIndex(sampleIndex).
                        getNameOrDepth(),
                        String.format("%d", sampleIndex));
            }
        }
        syntheticSuite2.convertDiscreteToContinuous(nameToDepth);
    }
    
    @Test
    public void testAmsCalculationType() {
        assertSame(AmsCalculationType.HEXT, AmsCalculationType.valueOf("HEXT"));
        assertSame(AmsCalculationType.BOOT, AmsCalculationType.valueOf("BOOT"));
        assertSame(AmsCalculationType.PARA_BOOT,
                AmsCalculationType.valueOf("PARA_BOOT"));
    }
    
    @Test
    public void testMissingSampleNameException() {
        final Suite suite = new Suite("SuiteTest");
        suite.new MissingSampleNameException("test");
    }
    
    @Test
    public void testSaveCalcsSample() throws IOException, PuffinUserException {
        for (Sample sample: syntheticSuite1.getSamples()) {
            sample.getTreatmentSteps().stream().forEach(d -> d.setSelected(true));
            sample.useSelectionForPca();
            sample.useSelectionForCircleFit();
            sample.doPca(Correction.NONE);
            sample.fitGreatCircle(Correction.NONE);
            sample.calculateMdf();
            sample.calculateFisher(Correction.NONE);
        }
        testSaveCalcsSampleHelper();
    }

    @Test
    public void testSaveCalcsSampleMdfOnly()
            throws IOException, PuffinUserException {
        for (Sample sample: syntheticSuite1.getSamples()) {
            sample.getTreatmentSteps().stream().forEach(d -> d.setSelected(true));
            sample.calculateMdf();
        }
        testSaveCalcsSampleHelper();
    }

    @Test
    public void testSaveCalcsSamplePcaOnly()
            throws IOException, PuffinUserException {
        for (Sample sample: syntheticSuite1.getSamples()) {
            sample.getTreatmentSteps().stream().forEach(d -> d.setSelected(true));
            sample.useSelectionForPca();
            sample.doPca(Correction.NONE);
        }
        testSaveCalcsSampleHelper();
    }

    private void testSaveCalcsSampleHelper()
            throws PuffinUserException, IOException {
        final File csvFile = File.createTempFile("puffinplot-test-",
                ".csv", temporaryFolder.getRoot());
        syntheticSuite1.saveCalcsSample(csvFile);
        final List<String> lines = Files.readAllLines(csvFile.toPath());
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
    }
    
    /**
     * Test that loading a suite from a file and saving it produces
     * an identical file (except for the SAVED_BY_PROGRAM field, which
     * is expected to change).
     */
    @Test
    public void testLoadAndSaveSuite() throws IOException, PuffinUserException {
        final DateFormat iso8601format =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        final Suite suite = new Suite("testLoadAndSaveSuite");
        loadFileDataIntoSuite(puffinFile1, suite);
        final File savedFile = saveSuiteToTempFile(suite);
        suite.saveAs(savedFile);
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
            Logger.getLogger(SuiteTest.class.getName()).
                    log(Level.SEVERE, null, ex);
            fail("Exception loading suite");
        } catch (ParseException ex) {
            Logger.getLogger(SuiteTest.class.getName()).
                    log(Level.SEVERE, null, ex);
            fail("Exception parsing date");
        }
    }
    
    /**
     * Test that saving a suite, then loading it from the saved file produces
     * an identical suite.
     */
    @Test
    public void testSaveAndLoadSuite() throws IOException, PuffinUserException {
        syntheticSuite1.getCustomFlagNames().add(0, "customgflag1");
        syntheticSuite1.getCustomNoteNames().add(0, "customgnote1");
        final String customFlags =
                syntheticSuite1.getCustomFlagNames().exportAsString();
        final String customNotes =
                syntheticSuite1.getCustomNoteNames().exportAsString();
        syntheticSuite1.setSiteNamesByDepth(syntheticSuite1.getSamples(), 100);
        syntheticSuite1.getSiteByName("0.00").setLocation(
                Location.fromDegrees(30, 60));
        final File savedFile = saveSuiteToTempFile(syntheticSuite1);
        final Suite loadedSuite = new Suite("testSaveAndLoadSuite");
        loadFileDataIntoSuite(savedFile, loadedSuite);
        final Iterator<Sample> actualSamples =
                loadedSuite.getSamples().iterator();
        for (Sample expectedSample: syntheticSuite1.getSamples()) {
            final Sample actualSample = actualSamples.next();
            final Iterator<TreatmentStep> actualData =
                    actualSample.getTreatmentSteps().iterator();
            for (TreatmentStep expectedTreatmentStep : expectedSample.getTreatmentSteps()) {
                final TreatmentStep actualTreatmentStep = actualData.next();
                assertEquals(expectedTreatmentStep.getMoment(),
                        actualTreatmentStep.getMoment());
                assertEquals(expectedTreatmentStep.getTreatmentType(),
                        actualTreatmentStep.getTreatmentType());
                assertEquals(expectedTreatmentStep.getIdOrDepth(),
                        actualTreatmentStep.getIdOrDepth());
                assertEquals(expectedTreatmentStep.getMeasurementType(),
                        actualTreatmentStep.getMeasurementType());
                assertEquals(expectedTreatmentStep.getAfX(),
                        actualTreatmentStep.getAfX(), 0.0001);
            }
        }
        assertEquals(customFlags,
                loadedSuite.getCustomFlagNames().exportAsString());
        assertEquals(customNotes,
                loadedSuite.getCustomNoteNames().exportAsString());
        assertEquals(
                syntheticSuite1.getSiteByName("0.00").getLocation().toStrings(),
                loadedSuite.getSiteByName("0.00").getLocation().toStrings());
    }
    
    @Test(expected = PuffinUserException.class)
    public void testSaveAsWithIOException() throws PuffinUserException {
        syntheticSuite1.saveAs(temporaryFolder.getRoot().toPath().
                resolve("nonexistent").resolve("somefile.csv").toFile());
    }
    
    private void loadFileDataIntoSuite(File file, Suite suiteFromFile)
            throws IOException {
        final File[] files = {file};
            suiteFromFile.readFiles(Arrays.asList(files));
    }
    
    private File saveSuiteToTempFile(Suite suite)
            throws IOException, PuffinUserException {
        final File savedFile =
                File.createTempFile("puffinplot-test-", ".ppl",
                        temporaryFolder.getRoot());
        savedFile.delete();
        suite.saveAs(savedFile);
        return savedFile;
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testReadFilesEmptyList() throws IOException {
        final Suite suite = new Suite("test");
        suite.readFiles(Collections.emptyList());
    }
    
    @Test(expected = NullPointerException.class)
    public void testReadFilesNullList() throws IOException {
        new Suite("test").readFiles(null);
    }
    
    @Test
    public void testNonExistentFile() {
        final File file = temporaryFolder.getRoot().toPath().
                resolve("nonexistent").toFile();
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
    
    /**
     * This rather bloated test simultaneously tests calculateSuiteMeans,
     * saveCalcsSuite, and calculateMultiSuiteMeans.
     * 
     * @throws PuffinUserException
     * @throws IOException 
     */
    @Test
    public void testCalculateAndSaveSuiteMeansAndMultiSuiteMeans()
            throws PuffinUserException, IOException {
        /*
         * The test data is arranged so as to distribute sample and site
         * means and VGPs fairly evenly across hemispheres, giving sufficient
         * data for a meaningful calculation in most of the categories.
         */
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

        // A single suite for testing calculateSuiteMeans
        final Suite bigSuite = new Suite("test");
        
        /*
         * Two smaller suites, which will have the same contents as bigSuite,
         * for testing calculateMultiSuiteMeans. The first suite will contain
         * the two even-indexed sites, and the second the odd-indexed sites.
         */
        final List<Suite> smallSuites = Arrays.asList(
                new Suite("test"), new Suite("test"));

        /*
         * Create and initialize sites.
         */
        final List<Site> bigSuiteSites = new ArrayList<>();
        final List<List<Site>> smallSuiteSites = Arrays.asList(
                new ArrayList<>(), new ArrayList<>());
        final double[][] siteLocations = {
            {45, 45},
            {-45, 45},
            {30, 50},
            {-30, -50}
        };
        for (int siteIndex = 0; siteIndex < siteLocations.length; siteIndex++) {
            final String siteId = Integer.toString(siteIndex);
            final Site bigSuiteSite = bigSuite.getOrCreateSite(siteId);
            final Location location =
                    Location.fromDegrees(siteLocations[siteIndex][0],
                            siteLocations[siteIndex][1]);
            bigSuiteSite.setLocation(location);
            bigSuiteSites.add(bigSuiteSite);
            
            // Pick suite according to the site index.
            final int smallSuiteIndex = siteIndex % 2;
            final Suite smallSuite = smallSuites.get(smallSuiteIndex);
            final Site smallSuiteSite = smallSuite.getOrCreateSite(siteId);
            smallSuiteSite.setLocation(location);
            smallSuiteSites.get(smallSuiteIndex).add(smallSuiteSite);
        }
        
        /*
         * Create samples and add them to the sites and suites.
         */
        final List<Sample> samples = new ArrayList<>();
        for (int i=0; i<testData.size(); i++) {
            final Vec3 direction = testData.get(i);
            final int siteIndex = i % siteLocations.length;
            final int smallSuiteIndex = siteIndex % 2;
            final String sampleName = String.format("%d", i);

            final Sample bigSuiteSample = new Sample(sampleName, bigSuite);
            bigSuiteSample.setImportedDirection(direction);
            final Site site = bigSuiteSites.get(siteIndex);
            bigSuiteSample.setSite(site);
            site.addSample(bigSuiteSample);
            samples.add(bigSuiteSample);
            bigSuite.addSample(bigSuiteSample, Integer.toString(i));

            final Suite smallSuite = smallSuites.get(smallSuiteIndex);
            final Sample smallSuiteSample = new Sample(sampleName, smallSuite);
            smallSuiteSample.setImportedDirection(direction);
            final Site site2 = smallSuiteSites.get(smallSuiteIndex).get(siteIndex / 2);
            smallSuiteSample.setSite(site2);
            site2.addSample(smallSuiteSample);
            smallSuite.addSample(smallSuiteSample, Integer.toString(i));
        }
        
        /*
         * Generate the actual data from the hand-crafted suites. The small
         * suites combined contain the same data as the large suite, so we
         * expect the calculation results to match.
         */
        bigSuite.doAllCalculations(Correction.NONE, "true");
        bigSuiteSites.forEach(s -> s.calculateFisherStats(Correction.NONE));
        bigSuite.calculateSuiteMeans(bigSuite.getSamples(),
                bigSuite.getSites());
        
        for (Suite smallSuite: smallSuites) {
            smallSuite.doAllCalculations(Correction.NONE, "true");
            smallSuite.getSites().forEach(s -> 
                s.calculateFisherStats(Correction.NONE));
        }
        final SuiteCalcs multiSuiteMeans =
                Suite.calculateMultiSuiteMeans(smallSuites);
        
        /*
         * Generate the expected data by calculating directly using SuiteCalcs.
         * We're not checking the maths here; that should happen in the
         * SuiteMeans unit tests (or maybe even lower down).
         */
        final SuiteCalcs.Means dirsBySample =
                SuiteCalcs.Means.calculate(testData);
        final SuiteCalcs.Means vgpsBySample =
                SuiteCalcs.Means.calculate(samples.stream().map(
                        (s) -> VGP.calculate(s.getDirection(), 0,
                                s.getSite().getLocation()).getLocation().
                                toVec3()).collect(Collectors.toList()));
        final SuiteCalcs.Means dirsBySite =
                SuiteCalcs.Means.calculate(bigSuiteSites.stream().map((s) ->
                s.getMeanDirection()).collect(Collectors.toList()));
        final SuiteCalcs.Means vgpsBySite =
                SuiteCalcs.Means.calculate(bigSuiteSites.stream().map((s) ->
                VGP.calculate(s.getMeanDirection(), 0, s.getLocation()).
                        getLocation().toVec3()).collect(Collectors.toList()));
        final SuiteCalcs expectedCalcs = new SuiteCalcs(dirsBySite,
                dirsBySample, vgpsBySite, vgpsBySample);
        
        /*
         * SuiteCalcs doesn't implement an equals method, but the toStrings
         * method is a convenient way to test for equality without
         * complications from exact floating-point comparisons.
         */
        assertEquals(expectedCalcs.toStrings(),
                bigSuite.getSuiteMeans().toStrings());
        
        assertEquals(expectedCalcs.toStrings(),
                multiSuiteMeans.toStrings());
        
        StringBuilder expectedFileContents = new StringBuilder();
        expectedFileContents.append(SuiteCalcs.getHeaders().stream().
                collect(Collectors.joining(",")) + "\n");
        for (List<String> line: expectedCalcs.toStrings()) {
            expectedFileContents.append(line.stream().
                    collect(Collectors.joining(",")) + "\n");
        }
        
        final File savedCalcs = temporaryFolder.getRoot().toPath().
                resolve("suitecalcs.csv").toFile();
        bigSuite.saveCalcsSuite(savedCalcs);
        
        final String actualFileContents =
                new String(Files.readAllBytes(savedCalcs.toPath()));
        assertEquals(expectedFileContents.toString(), actualFileContents);
    }
    
    @Test
    public void testReadDirectionalData() {
        try {
            testReadDirectionalData(MeasurementType.DISCRETE,
                    "discrete.txt",
                    "SAMPLE1 30 40\nSAMPLE2\t50\t60",
                    new String[] {"SAMPLE1", "SAMPLE2"},
                    new double[][] {{30, 40}, {50, 60}});
            testReadDirectionalData(MeasurementType.CONTINUOUS,
                    "continuous.txt",
                    "0.1 , 35 , 45\n0.2,55,65",
                    new String[] {"0.1", "0.2"},
                    new double[][] {{35, 45}, {55, 65}});
        } catch (IOException ex) {
            Logger.getLogger(SuiteTest.class.getName()).
                    log(Level.SEVERE, null, ex);
            fail();
        }
    }

    private void testReadDirectionalData(final MeasurementType measurementType,
            final String filename,
            final String fileContents, final String[] sampleNames,
            final double[][] directions) throws IOException {
        final Suite suite = new Suite("test");
        suite.readDirectionalData(Collections.singletonList(
                TestUtils.writeStringToTemporaryFile(filename, fileContents,
                temporaryFolder)));
        assertEquals(sampleNames.length, suite.getSamples().size());
        assertEquals(measurementType, suite.getMeasurementType());
        for (int i=0; i<sampleNames.length; i++) {
            final Sample sample = suite.getSampleByIndex(i);
            assertTrue(Vec3.fromPolarDegrees(
                    1, directions[i][1], directions[i][0]).
                    equals(sample.getDirection()));
            assertEquals(sampleNames[i], sample.getNameOrDepth());
        }
    }
    
    @Test(expected = IOException.class)
    public void testReadDirectionalDataFromNonexistentFile()
            throws IOException {
        syntheticSuite1.readDirectionalData(Collections.singletonList(
                temporaryFolder.getRoot().toPath().resolve("nonexistent").
                        toFile()));
    }
    
    @Test
    public void testSetAndGetCurrentSampleIndex() {
        for (int i=0; i<2; i++) {
            syntheticSuite1.setCurrentSampleIndex(i);
            assertEquals(i, syntheticSuite1.getCurrentSampleIndex());
        }
    }

    @Test
    public void testGetCurrentSample() {
        final List<Sample> samples = syntheticSuite1.getSamples();
        for (int i=0; i<2; i++) {
            syntheticSuite1.setCurrentSampleIndex(i);
            assertEquals(samples.get(i),
                    syntheticSuite1.getCurrentSample());
        }
    }
    
    @Test
    public void testGetSampleByIndexOfMinusOne() {
        assertNull(syntheticSuite1.getSampleByIndex(-1));
    }
    
    @Test
    public void testGetSampleByIndexFromEmptySuite() {
        assertNull(new Suite("test").getSampleByIndex(0));
    }
    
    @Test(expected = NullPointerException.class)
    public void testAddDatumNull() {
        syntheticSuite1.addDatum(null);
    }

    @Test(expected = NullPointerException.class)
    public void testAddDatumNullMeasType() {
        final TreatmentStep d = new TreatmentStep();
        d.setMeasurementType(null);
        syntheticSuite1.addDatum(d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddDatumUnsetMeasType() {
        final TreatmentStep d = new TreatmentStep();
        d.setMeasurementType(MeasurementType.UNSET);
        syntheticSuite1.addDatum(d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddDatumIncompatibleMeasType() {
        final TreatmentStep d = new TreatmentStep();
        d.setMeasurementType(MeasurementType.DISCRETE);
        syntheticSuite1.addDatum(d);
    }

    @Test
    public void testAddDatumUnknownTreatType() {
        final TreatmentStep d = new TreatmentStep();
        d.setMeasurementType(MeasurementType.CONTINUOUS);
        d.setDepth("0");
        d.setTreatmentType(TreatmentType.UNKNOWN);
        syntheticSuite1.addDatum(d);
    }

    @Test
    public void testToStringReturnsSuiteName() {
        assertEquals(syntheticSuite1.getName(), syntheticSuite1.toString());
    }
    
    @Test
    public void testGetIndexBySample() {
        final int sampleIndex = 0;
        assertEquals(sampleIndex,
                syntheticSuite1.getIndexBySample(
                        syntheticSuite1.getSampleByIndex(sampleIndex)));
    }
    
    @Test
    public void testGetIndexBySampleWithSampleNotInSuite() {
        final Sample notInSuite = new Sample("test", null);
        assertEquals(-1, syntheticSuite1.getIndexBySample(notInSuite));
    }
    
    @Test
    public void testIsSaved() throws IOException {
        final Suite suite = new Suite("test");
        loadFileDataIntoSuite(puffinFile1, suite);
        assertTrue(suite.isSaved());
        suite.getSampleByIndex(0).clearPca();
        assertFalse(suite.isSaved());        
    }
    
    @Test
    public void testSavedListener() {
        /*
         * To get around the "final variables only" restriction on Java
         * closures, we wrap the boolean value in an array.
         */
        final boolean[] saved = new boolean[1];

        final Suite.SavedListener savedListener =
                (boolean newState) -> { saved[0] = newState; };
        syntheticSuite1.addSavedListener(savedListener);
        syntheticSuite1.setSaved(false);
        assertFalse(saved[0]);
        syntheticSuite1.setSaved(true);
        assertTrue(saved[0]);
        syntheticSuite1.removeSavedListener(savedListener);
        syntheticSuite1.setSaved(false);
        assertTrue(saved[0]);
    }
    
    @Test
    public void testGetCreator() {
        assertEquals("SuiteTest", syntheticSuite1.getCreator());
    }
    
    @Test
    public void testIsFilenameSet() throws IOException {
        assertFalse(syntheticSuite1.isFilenameSet());
        final Suite suite = new Suite("test");
        loadFileDataIntoSuite(puffinFile1, suite);
        assertTrue(suite.isFilenameSet());
    }
    
    @Test
    public void testSaveWithFilenameSet()
            throws PuffinUserException, IOException {
        final Suite suite1 = new Suite("SuiteTest");
        loadFileDataIntoSuite(puffinFile1, suite1);
        puffinFile1.delete();
        final String sampleName = "New sample name";
        suite1.getSampleByIndex(0).setNameOrDepth(sampleName);
        suite1.save();
        final Suite suite2 = new Suite("SuiteTest");
        loadFileDataIntoSuite(puffinFile1, suite2);
        assertEquals(sampleName, suite2.getSampleByIndex(0).getNameOrDepth());
    }
    
    @Test
    public void testSaveWithNoFilenameSet() throws PuffinUserException {
        /* Defined behaviour here is simply to do nothing, since no
         * save path has been set.
         */
        syntheticSuite1.save();
    }
    
    @Test
    public void testGetCustomFlagNames() {
        testCustomNames(syntheticSuite1.getCustomFlagNames());
    }

    @Test
    public void testGetCustomNoteNames() {
        testCustomNames(syntheticSuite1.getCustomNoteNames());
    }
    
    private void testCustomNames(final CustomFields<String> customNames) {
        assertEquals(0, customNames.size());
        customNames.add(0, "test1");
        customNames.add(0, "test0");
        assertEquals("test0\ttest1", customNames.exportAsString());
        customNames.swapAdjacent(0);
        assertEquals("test1\ttest0", customNames.exportAsString());
        customNames.remove(0);
        assertEquals("test0", customNames.get(0));
        assertNull(customNames.get(1)); // out of range: should be null
        customNames.setSize(3, "test3");
        assertEquals("test3\ttest3\ttest3", customNames.exportAsString());
        customNames.set(0, "test0");
        assertEquals("test0\ttest3\ttest3", customNames.exportAsString());
        customNames.set(5, "test5"); // out of range: should do nothing
        assertEquals("test0\ttest3\ttest3", customNames.exportAsString());
    }

    @Test
    public void testGetPuffinFile() throws IOException {
        final Suite suite = new Suite("test");
        loadFileDataIntoSuite(puffinFile1, suite);
        assertEquals(puffinFile1, suite.getPuffinFile());
    }
    
    @Test
    public void testReadFilesFromDirectory() throws IOException {
        final Suite suite = new Suite("test");
        loadFileDataIntoSuite(puffinFile1.getParentFile(), suite);
        assertEquals(2, suite.getNumSamples());
    }
    
    @Test
    public void testGetMinDepth() {
        assertEquals(0, syntheticSuite1.getMinDepth(), 1e-10);
    }
    
    @Test
    public void testGetMinDepthOnDiscreteSuite() {
        assertTrue(Double.isNaN(syntheticSuite2.getMinDepth()));
    }

    @Test
    public void testGetMaxDepth() {
        assertEquals(9, syntheticSuite1.getMaxDepth(), 1e-10);
    }
    
    @Test
    public void testGetMaxDepthOnDiscreteSuite() {
        assertTrue(Double.isNaN(syntheticSuite2.getMaxDepth()));
    }
    
    @Test
    public void testGetMaxDepthWithOutOfOrderDepths() {
        syntheticSuite1.getSampleByIndex(0).setDepth("7");
        assertEquals(9, syntheticSuite1.getMaxDepth(), 1e-10);
    }
    
    @Test
    public void testSetSiteNamesByDepth() {
        syntheticSuite1.setSiteNamesByDepth(syntheticSuite1.getSamples(), 5);
        for (Sample sample: syntheticSuite1.getSamples()) {
            assertEquals(
                    sample.getDepth() < 5 ? "0.00" : "5.00",
                    sample.getSite().getName()
            );
        }
    }
    
    @Test
    public void testSetSiteNamesBySubstring() {
        final int siteCutoff = 5;
        for (int i=0; i<syntheticSuite2.getNumSamples(); i++) {
            final Sample sample = syntheticSuite2.getSampleByIndex(i);
            sample.setNameOrDepth((i < siteCutoff ? "SITE1_" : "SITE2_") +
                    sample.getNameOrDepth());
        }
        syntheticSuite2.setSiteNamesBySubstring(syntheticSuite2.getSamples(),
                BitSet.valueOf(new byte[] {(byte) 0b00011111}));
        for (int i=0; i<syntheticSuite2.getNumSamples(); i++) {
            final Sample sample = syntheticSuite2.getSampleByIndex(i);
            assertEquals(i < siteCutoff ? "SITE1" : "SITE2",
                    sample.getSite().getName());
        }
    }
    
    @Test
    public void testSetNamedSiteForSamplesRepeatedly() {
        syntheticSuite1.setNamedSiteForSamples(syntheticSuite1.getSamples(),
                "site1");
        assertEquals("site1", syntheticSuite1.getSites().stream().
                map((site) -> site.getName()).collect(Collectors.joining()));
        syntheticSuite1.setNamedSiteForSamples(syntheticSuite1.getSamples(),
                "site2");
        assertEquals("site2", syntheticSuite1.getSites().stream().
                map((site) -> site.getName()).collect(Collectors.joining()));
    }
    
    @Test
    public void testSetNamedSiteForSamplesAndClearSites() {
        final Suite suite = syntheticSuite1;
        final String siteName1 = "site1";
        suite.setNamedSiteForSamples(suite.getSamples(), siteName1);
        assertEquals(1, suite.getSites().size());
        for (Sample sample: suite.getSamples()) {
            assertEquals(siteName1, sample.getSite().getName());
        }
        final String siteName0 = "site0";
        suite.setNamedSiteForSamples(suite.getSamples().subList(0, 1), siteName0);
        assertEquals(siteName0,
                suite.getSampleByIndex(0).getSite().getName());
        assertEquals(new HashSet(Arrays.asList(siteName0, siteName1)),
                suite.getSites().stream().map(Site::getName).collect(Collectors.toSet())
        );
        suite.clearSites(suite.getSamples().subList(0, 1));
        assertEquals(Arrays.asList(siteName1),
                suite.getSites().stream().map(Site::getName).collect(Collectors.toList()));
        suite.clearSites(suite.getSamples());
        assertTrue(suite.getSites().isEmpty());
    }

    @Test
    public void testRescaleMagSus() {
        syntheticSuite1.setSaved(true);
        syntheticSuite1.rescaleMagSus(2);
        assertFalse(syntheticSuite1.isSaved());
        for (Sample sample: syntheticSuite1.getSamples()) {
            for (TreatmentStep d: sample.getTreatmentSteps()) {
                assertEquals(Double.parseDouble(d.getDepth())*2,
                        d.getMagSus(), 1e-10);
            }
        }
    }
    
    /**
     * Here we just test that doSiteCalculations produces the same effect
     * as calculating directly from the Site objects. The correctness of
     * the calculations is checked in the tests for the appropriate 
     * calculation classes.
     */
    @Test
    public void testDoAndSaveSiteCalculations()
            throws PuffinUserException, IOException {
        setUpSiteCalculations(syntheticSuite1);
        
        /*
         * This part is purely a characterization test, with expected output
         * generated from a previous run. The correctness of the calculations
         * is checked elsewhere.
         */
        
        final File siteCalcsFile = temporaryFolder.getRoot().toPath().
                resolve("sitecalcs.csv").toFile();
        syntheticSuite1.saveCalcsSite(siteCalcsFile);
        final String siteCalcsString =
                new String(Files.readAllBytes(siteCalcsFile.toPath()));
        final String expectedCalcs =
                "Site,Samples,Fisher dec. (deg),Fisher inc. (deg),Fisher a95 (deg),Fisher k,Fisher nDirs,Fisher R,"
                + "GC valid,GC dec. (deg),GC inc. (deg),GC a95 (deg),GC k,GC N,GC M,GC R,GC min points,"
                + "GC D1min (degC or mT),GC D1max (degC or mT),GC D2min (degC or mT),GC D2max (degC or mT),"
                + "Lat (deg),Long (deg),VGP lat (deg),VGP long (deg),VGP dp (deg),VGP dm (deg)\n"
                + "0.00,5,22.8728,14.4217,16.7387,21.8470,5,4.8169,Y,183.0448,13.0636,6.8346,269.1429,5,0,4.9944,10,0.00000,0.00000,90.0000,90.0000,15.0,25.0,-68.1725,16.8419,3.5534,6.9694\n"
                + "5.00,5,33.9598,3.3541,1.0511,5300.1858,5,4.9992,Y,189.9534,4.3122,1.6842,4422.6002,5,0,4.9997,10,0.00000,0.00000,90.0000,90.0000,,,,,,\n";
        assertEquals(expectedCalcs, siteCalcsString);
        syntheticSuite1.getSites().forEach(s -> s.clearFisherStats());
        syntheticSuite1.getSites().forEach(s -> s.clearGcFit());
        syntheticSuite1.saveCalcsSite(siteCalcsFile);
        final String siteCalcsString2 =
                new String(Files.readAllBytes(siteCalcsFile.toPath()));
        final String expectedCalcs2 =
                "Site,Samples,Fisher dec. (deg),Fisher inc. (deg),Fisher a95 (deg),Fisher k,Fisher nDirs,Fisher R,"
                + "GC valid,GC dec. (deg),GC inc. (deg),GC a95 (deg),GC k,GC N,GC M,GC R,GC min points,"
                + "GC D1min (degC or mT),GC D1max (degC or mT),GC D2min (degC or mT),GC D2max (degC or mT),"
                + "Lat (deg),Long (deg),VGP lat (deg),VGP long (deg),VGP dp (deg),VGP dm (deg)\n"
                + "0.00,5,,,,,,,,,,,,,,,,,,,,15.0,25.0,-68.1725,16.8419,3.5534,6.9694\n"
                + "5.00,5,,,,,,,,,,,,,,,,,,,,,,,,,\n";
        assertEquals(expectedCalcs2, siteCalcsString2);
    }

    private static void setUpSiteCalculations(Suite suite) {
        for (Sample sample: suite.getSamples()) {
            for (TreatmentStep treatmentStep : sample.getTreatmentSteps()) {
                treatmentStep.setInPca(true);
                treatmentStep.setOnCircle(true);
            }
            sample.doPca(Correction.NONE);
            sample.fitGreatCircle(Correction.NONE);
        }
        suite.setSiteNamesByDepth(suite.getSamples(), 5);
        suite.getSiteByName("0.00").
                setLocation(Location.fromDegrees(15, 25));
        
        // Set a null site to check that site-less samples are
        // correctly handled.
        suite.getSampleByIndex(0).setSite(null);
        
        suite.doSiteCalculations(Correction.NONE, "true");
        for (Site site: suite.getSites()) {
            final FisherValues actualFisherValues = site.getFisherValues();
            final GreatCircles actualGreatCircles = site.getGreatCircles();
            site.clearFisherStats();
            site.clearGcFit();
            site.calculateFisherStats(Correction.NONE);
            site.calculateGreatCirclesDirection(Correction.NONE, "true");
            assertEquals(site.getFisherValues().toStrings(),
                    actualFisherValues.toStrings());
            assertEquals(site.getGreatCircles().toStrings(),
                    actualGreatCircles.toStrings());
        }
    }

    @Test(expected = PuffinUserException.class)
    public void testSaveCalcsSiteWithNoSites() throws PuffinUserException {
        syntheticSuite1.saveCalcsSite(temporaryFolder.getRoot().toPath().
                resolve("sitecalcs.csv").toFile());
    }
    
    @Test(expected = PuffinUserException.class)
    public void testSaveCalcsSiteWithIOException() throws PuffinUserException {
        setUpSiteCalculations(syntheticSuite1);
        syntheticSuite1.saveCalcsSite(temporaryFolder.getRoot().toPath().
                resolve("nonexistent").resolve("somefile.csv").toFile());
    }
    
    @Test
    public void testCalculateAndClearAmsStatisticsHext() throws IOException {
        testCalculateAmsStatistics(AmsCalculationType.HEXT, 1);
        testCalculateAmsStatistics(AmsCalculationType.BOOT, 1);
        testCalculateAmsStatistics(AmsCalculationType.PARA_BOOT, 2);
        assertNotNull(syntheticSuite2.getAmsHextParams());
        assertNotNull(syntheticSuite2.getAmsBootstrapParams());
        syntheticSuite2.clearAmsCalculations();
        assertNull(syntheticSuite2.getAmsHextParams());
        assertNull(syntheticSuite2.getAmsBootstrapParams());
    }
    
    private void testCalculateAmsStatistics(AmsCalculationType calcType,
            int expectedTauValue) throws IOException {
        Assume.assumeTrue("Linux".equals(System.getProperty("os.name")));
        for (int sampleIndex=0; sampleIndex<10; sampleIndex++) {
            syntheticSuite2.getSampleByIndex(sampleIndex).setAmsDirections(
                    90, 0, 0, sampleIndex, 0, 90+sampleIndex);
        }
        
        /*
         * This script outputs nonsense values, because we're not checking
         * the correctness of the calculation here -- just that the script
         * is getting called appropriately. It uses the tau value (first
         * field) to signal whether "-par" was passed as the third argument
         * (as it should be for parametric bootstrap calculation): tau==1 for
         * "no -par", and tau==2 for "-par present". Apart from this, the
         * output is not checked.
         */
        final File script = TestUtils.writeStringToTemporaryFile("script.sh",
                "#!/bin/sh\n\n" +
                        "if [ $# -gt 2 ] && [ $3 = \"-par\" ]\n" +
                        "then line=\"2 2 3 4 5 6 7 8 9 10\"\n" +
                        "else line=\"1 2 3 4 5 6 7 8 9 10\"\n" +
                        "fi\n" +
                        "for i in 1 2 3 4 5 6 7 8 9; do echo $line; done\n",
                temporaryFolder);
        script.setExecutable(true);
        syntheticSuite2.calculateAmsStatistics(syntheticSuite2.getSamples(),
                calcType, script.getCanonicalPath());
        final List<KentParams> kentParams = 
                calcType==AmsCalculationType.HEXT ?
                syntheticSuite2.getAmsHextParams() :
                syntheticSuite2.getAmsBootstrapParams();
        assertEquals(3, kentParams.size());
        assertTrue(kentParams.stream().noneMatch((kp) -> kp == null));
        assertEquals(expectedTauValue, (int) kentParams.get(0).getTau());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCalculateAmsStatisticsWithNoAmsData() throws IOException {
        final File script = TestUtils.writeStringToTemporaryFile("script.sh",
                "#!/bin/sh\n", temporaryFolder);
        syntheticSuite2.calculateAmsStatistics(syntheticSuite2.getSamples(),
                AmsCalculationType.HEXT, script.getCanonicalPath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateAmsStatisticsWithInsufficientAmsData()
            throws IOException {
        for (int sampleIndex=0; sampleIndex<2; sampleIndex++) {
            syntheticSuite2.getSampleByIndex(sampleIndex).setAmsDirections(
                    90, 0, 0, sampleIndex, 0, 90+sampleIndex);
        }
        final File script = TestUtils.writeStringToTemporaryFile("script.sh",
                "#!/bin/sh\n", temporaryFolder);
        syntheticSuite2.calculateAmsStatistics(syntheticSuite2.getSamples(),
                AmsCalculationType.HEXT, script.getCanonicalPath());
    }

    @Test
    public void testContainsSample() {
        for (int i=0; i<9; i++) {
            final String sampleName = String.format("SAMPLE_%d", i);
            assertTrue(syntheticSuite2.containsSample(sampleName));
            assertFalse(syntheticSuite1.containsSample(sampleName));
        }
        for (int i=0; i<9; i++) {
            final String sampleName = String.format("%d", i);
            assertTrue(syntheticSuite1.containsSample(sampleName));
            assertFalse(syntheticSuite2.containsSample(sampleName));
        }
        
        for (String sampleName: new String[] {"", "nonexistent", "SAMPLE_99",
            "???"}) {
            assertFalse(syntheticSuite1.containsSample(sampleName));
            assertFalse(syntheticSuite2.containsSample(sampleName));
        }
    }

    @Test
    public void testCalculateAndGetSiteFishers() {
        for (Sample sample: syntheticSuite1.getSamples()) {
            sample.selectAll();
            sample.useSelectionForPca();
            sample.doPca(Correction.NONE);
        }
        syntheticSuite1.setSiteNamesByDepth(syntheticSuite1.getSamples(), 5);
        syntheticSuite1.calculateSiteFishers(Correction.NONE);
                
        final List<FisherValues> actual = syntheticSuite1.getSiteFishers();
        assertEquals(2, actual.size());
        assertEquals(FisherValues.calculate(syntheticSuite1.getSamples().
                subList(0, 5).stream().map(sample -> sample.getDirection()).
                collect(Collectors.toList())).toStrings(),
                actual.get(0).toStrings());
        assertEquals(FisherValues.calculate(syntheticSuite1.getSamples().
                subList(5, 10).stream().map(sample -> sample.getDirection()).
                collect(Collectors.toList())).toStrings(),
                actual.get(1).toStrings());
    }
    
    @Test
    public void testRemoveSamplesOutsideDepthRange() {
        syntheticSuite1.removeSamplesOutsideDepthRange(2.5, 7.5);
        assertEquals("34567",
                syntheticSuite1.getSamples().stream().
                        map(sample -> sample.getNameOrDepth()).
                        collect(Collectors.joining()));
        createSuites();
        syntheticSuite1.removeSamplesOutsideDepthRange(-50, 50);
        assertEquals("0123456789",
                syntheticSuite1.getSamples().stream().
                        map(sample -> sample.getNameOrDepth()).
                        collect(Collectors.joining()));        
        createSuites();
        syntheticSuite1.removeSamplesOutsideDepthRange(0, 5);
        assertEquals("012345",
                syntheticSuite1.getSamples().stream().
                        map(sample -> sample.getNameOrDepth()).
                        collect(Collectors.joining()));        
    }
    
    @Test
    public void testRemoveSamplesByTreatmentType() {
        final Set initialSampleSet = new HashSet(syntheticSuite1.getSamples());
        final Set<Integer> indicesToRemove =
                Arrays.asList(3, 5, 6).stream().collect(Collectors.toSet());
        final Set<Sample> samplesToRemove = indicesToRemove.stream().
                        map(i -> syntheticSuite1.getSampleByIndex(i)).
                        collect(Collectors.toSet());
        samplesToRemove.stream().flatMap(s -> s.getTreatmentSteps().stream()).
                forEach(d -> d.setTreatmentType(TreatmentType.DEGAUSS_Z));
        syntheticSuite1.removeSamplesByTreatmentType(
                syntheticSuite1.getSamples(), TreatmentType.DEGAUSS_Z);
        final Set remainingSampleSet = new HashSet(syntheticSuite1.getSamples());
        assertTrue(initialSampleSet.containsAll(remainingSampleSet));
        assertTrue(initialSampleSet.containsAll(samplesToRemove));
        assertEquals(initialSampleSet.size(),
                samplesToRemove.size() + remainingSampleSet.size());
    }
    
    @Test
    public void testImportLocations() throws IOException {
        syntheticSuite1.setSiteNamesByDepth(syntheticSuite1.getSamples(), 5);
        final File locationFile =
                TestUtils.writeStringToTemporaryFile("locations.txt",
                        "0.00,30,45\n5.00,-20,350\nnonexistent,15,15\n",
                        temporaryFolder);
        syntheticSuite1.importLocations(locationFile);
        assertTrue(Location.fromDegrees(30, 45).toVec3().equals(
                syntheticSuite1.getSiteByName("0.00").getLocation().toVec3(),
                1e-10));
        assertTrue(Location.fromDegrees(-20, 350).toVec3().equals(
                syntheticSuite1.getSiteByName("5.00").getLocation().toVec3(),
                1e-10));
    }
    
    @Test
    public void testImportAmsFromAsc() throws IOException {
        Path filePath = extractAscFileFromResources();
        
        /*
         * Import to a sample that doesn't exist yet, with directions
         * relative to geographic north.
         */
        syntheticSuite2.importAmsFromAsc(
                Collections.singletonList(filePath.toFile()), false,
                false, false);
        assertEquals(11, syntheticSuite2.getNumSamples());
        final Sample sample =
                syntheticSuite2.getSampleByName("LPA0309.1");
        assertNotNull(sample);
        assertNotNull(sample.getAms());

        /*
         * Expected values generated from a previous run, but verified against
         * the pre-transformed geographic system directions in the input file.
         * Actual values recalculated from scratch using the specimen system
         * directions and sample orientation from the file. There is a minor
         * discrepancy: the file gives 0.9931 rather than 0.9330 for the first
         * parameter. But SAFYR presumably calculated that using full accuracy
         * for the specimen-system directions, while PuffinPlot has to work with
         * the 4 d.p. that the ASC file provides.
         */
        assertEquals("0.99303 1.01217 0.99480 0.00477 0.00079 0.00490",
                syntheticSuite2.getSampleByName("LPA0309.1").getAms().
                        toTensorComponentString());
        
        /*
         * To exercise some more code paths, we now import again to the same
         * (existing) sample, after adding a TreatmentStep with orientation parameters
         * to it. This time we specify magnetic rather than geographic north
         * (although in this case it's the same anyway).
         */
        final TreatmentStep treatmentStep = new TreatmentStep();
        treatmentStep.setSampAz(0);
        treatmentStep.setSampDip(90);
        treatmentStep.setFormAz(0);
        treatmentStep.setFormDip(0);
        
        /* This will set the sample's sample/formation corrections
         * from those of the treatmentStep.
         */
        sample.addTreatmentStep(treatmentStep);
        
        syntheticSuite2.importAmsFromAsc(
                Collections.singletonList(filePath.toFile()),
                true, false, false);
        
        /*
         * The expected values are taken from the same "principal directions"
         * fields in the file that AmsLoader reads. However, the test
         * is non-trivial because the Tensor constructor transforms the
         * tensor by the sample and formation orientations. In this case
         * we've set those orientations to 0 / 90 / 0 / 0, corresponding
         * to an identity transformation, so we end up with the same
         * values.
         */
        assertEquals("0.99480 1.00400 1.00120 0.00160 0.01060 -0.00470",
                syntheticSuite2.getSampleByName("LPA0309.1").getAms().
                        toTensorComponentString());
        
        /*
         * Now we import a third time to the same sample. This time we
         * set the corrections to arbitrary values beforehand, but
         * request that they be overwritten by values read from the ASC
         * file.
         */
        sample.setCorrections(17, 18, 19, 20, 0);
        syntheticSuite2.importAmsFromAsc(
                Collections.singletonList(filePath.toFile()),
                true, true, true);
        assertEquals("0.99303 1.01217 0.99480 0.00477 0.00079 0.00490",
                syntheticSuite2.getSampleByName("LPA0309.1").getAms().
                        toTensorComponentString());
        assertEquals(28, sample.getSampAz(), delta);
        assertEquals(0, sample.getSampDip(), delta);
        assertEquals(0, sample.getFormAz(), delta);
        assertEquals(0, sample.getFormDip(), delta);
    }

    private Path extractAscFileFromResources() throws IOException {
        final String filename = "LPA03091.ASC";
        final InputStream dataStream =
                TestFileLocator.class.getResourceAsStream(filename);
        final Path filePath =
                temporaryFolder.getRoot().toPath().resolve(filename);
        Files.copy(dataStream, filePath);
        return filePath;
    }
    
    @Test
    public void testReadFilesWithMultipleFiles() throws IOException {
        final Suite suite = new Suite("SuiteTest");
        suite.readFiles(Arrays.asList(puffinFile1, puffinFile1));
        assertEquals(temporaryFolder.getRoot().getName(), suite.getName());
    }
    
    @Test
    public void testReadFilesSuiteNameSetting() throws IOException {
        final Suite suite = new Suite("SuiteTest");
        
        /*
         * Reading a file into an empty suite should set the suite name
         * from the filename.
         */
        suite.readFiles(Collections.singletonList(puffinFile1));
        assertEquals(puffinFile1.getName(), suite.getName());

        /*
         * Reading a file into a non-empty suite should not change
         * the suite's name.
         */
        final Path otherFilePath =
                temporaryFolder.getRoot().toPath().resolve("other-file.ppl");        
        Files.copy(puffinFile1.toPath(), otherFilePath);
        suite.readFiles(Collections.singletonList(otherFilePath.toFile()));
        assertEquals(puffinFile1.getName(), suite.getName());
    }
    
    @Test
    public void testReadUnreadableFile() throws IOException {
        final Suite suite = new Suite("SuiteTest");
        puffinFile1.setReadable(false);
        suite.readFiles(Collections.singletonList(puffinFile1));
        assertEquals(1, suite.getLoadWarnings().size());
        assertEquals(0, suite.getNumSamples());
    }
    
    @Test
    public void testReadFileInUnknownFormat() throws IOException {
        final Suite suite = new Suite("SuiteTest");
        suite.readFiles(Collections.singletonList(puffinFile1),
                SensorLengths.fromPresetName("1:1:1"),
                TwoGeeLoader.Protocol.NORMAL, false,
                FileType.UNKNOWN, null,
                Collections.EMPTY_MAP);
        assertEquals(1, suite.getLoadWarnings().size());
        assertEquals(0, suite.getNumSamples());
    }
    
    @Test
    public void testReadDiscreteDataIntoContinuousSuite() throws IOException {
        final int initialNumberOfSamples = syntheticSuite1.getNumSamples();
        syntheticSuite1.readFiles(Collections.singletonList(puffinFile1));
        assertEquals(1, syntheticSuite1.getLoadWarnings().size());
        assertEquals(initialNumberOfSamples, syntheticSuite1.getNumSamples());
    }
    
    @Test
    public void testImportAmsFromDelimitedFileWithTensor() throws IOException {
        final File ascFile1 = TestUtils.writeStringToTemporaryFile("test1.asc",
                "sample1 0.9948 1.0040 1.0012 0.0016 0.0106 -0.0047 "+
                        "0 90 0 0 0\n",
                temporaryFolder);
        final Suite suite = new Suite("SuiteTest");
        suite.importAmsFromDelimitedFile(Collections.singletonList(ascFile1),
                false);
        assertEquals("0.99480 1.00400 1.00120 0.00160 0.01060 -0.00470",
                suite.getSampleByName("sample1").getAms().
                        toTensorComponentString());
        
        // Try overwriting with new data to check that existing samples
        // are handled correctly.
        final File ascFile2 = TestUtils.writeStringToTemporaryFile("test2.asc",
                "sample1 0.9111 1.0040 1.0012 0.0016 0.0106 -0.0047 "+
                        "0 90 0 0 0\n",
                temporaryFolder);
        suite.importAmsFromDelimitedFile(Collections.singletonList(ascFile2),
                false);
        assertEquals("0.91110 1.00400 1.00120 0.00160 0.01060 -0.00470",
                suite.getSampleByName("sample1").getAms().
                        toTensorComponentString());
    }
    
    @Test
    public void testImportAmsFromDelimitedFileWithDirections()
            throws IOException {
        final File ascFile1 = TestUtils.writeStringToTemporaryFile("test1.asc",
                "sample1 42 98 23 211 39 321\n",
                temporaryFolder);
        final Suite suite = new Suite("SuiteTest");
        final TreatmentStep d = new TreatmentStep(Vec3.NORTH);
        d.setSampAz(0);
        d.setSampDip(90);
        d.setFormAz(0);
        d.setFormDip(0);
        d.setMagDev(0);
        d.setMeasurementType(MeasurementType.DISCRETE);
        d.setDiscreteId("sample1");
        suite.addDatum(d);
        final Sample sample = suite.getSampleByIndex(0);
        sample.setCorrections(0, 90, 0, 0, 0);
        suite.updateReverseIndex();
        suite.importAmsFromDelimitedFile(Collections.singletonList(ascFile1),
                true);
        /*
         * Not checking the correctness of the calculation here, just that
         * it was added correctly to the right sample in the suite.
         */
        assertNotNull(suite.getSampleByName("sample1").getAms());
    }
    
    @Test
    public void testExportToFiles() throws IOException {
        syntheticSuite2.exportToFiles(temporaryFolder.getRoot(),
                Arrays.asList(TreatmentStepField.AF_X, TreatmentStepField.X_MOMENT,
                        TreatmentStepField.Y_MOMENT, TreatmentStepField.Z_MOMENT));
        for (Sample sample: syntheticSuite2.getSamples()) {
            final List<String> lines = Files.readAllLines(
                    Paths.get(temporaryFolder.getRoot().getCanonicalPath(),
                            sample.getNameOrDepth()));
            final Iterator<String> lineIterator = lines.iterator();
            final Iterator<TreatmentStep> datumIterator = sample.getTreatmentSteps().iterator();
            while (datumIterator.hasNext()) {
                final TreatmentStep treatmentStep = datumIterator.next();
                final String line = lineIterator.next();
                final Scanner scanner = new Scanner(line);
                scanner.useLocale(Locale.ENGLISH);
                final double actualAfx = scanner.nextDouble();
                final Vec3 actualMoment =
                        new Vec3(scanner.nextDouble(), scanner.nextDouble(),
                        scanner.nextDouble());
                assertTrue(treatmentStep.getMoment().equals(actualMoment, 1e-10));
                assertEquals(treatmentStep.getAfX(), actualAfx, 1e-10);
            }
        }
    }
    
    @Test(expected = PuffinUserException.class)
    public void testSaveCalcsSuiteWithNoCalculations()
            throws PuffinUserException {
        syntheticSuite1.saveCalcsSuite(temporaryFolder.getRoot().toPath().
                resolve("calculations.csv").toFile());
    }
    
    @Test(expected = PuffinUserException.class)
    public void testSavedCalcsSuiteWithIOException()
            throws PuffinUserException {
        syntheticSuite1.getSamples().forEach(s -> {
            s.selectAll();
            s.useSelectionForPca();
            s.doPca(Correction.NONE);
        });
        syntheticSuite1.calculateSuiteMeans(syntheticSuite1.getSamples(),
                syntheticSuite1.getSites());
        syntheticSuite1.saveCalcsSuite(temporaryFolder.getRoot().toPath().
                resolve("nonexistent").resolve("somefile.csv").toFile());
    }
    
    @Test
    public void testDoReversalTest() {
        final List<Vec3> suite1directions =
                TestUtils.makeVectorList(new double[][] {
                    { 1,  0,  1},
                    {-1,  0,  1},
                    { 0,  1, -1},
                    { 0, -1, -1}
                }, true);
        final List<Vec3> suite2directions =
                TestUtils.makeVectorList(new double[][] {
                    { 1,  0, -1},
                    {-1,  0, -1},
                    { 0,  1,  1},
                    { 0, -1,  1}
                }, true);
        
        final Suite suite1 = makeSuiteFromDirections(suite1directions);
        final Suite suite2 = makeSuiteFromDirections(suite2directions);
        
        List<FisherValues> fishers =
                Suite.doReversalTest(Arrays.asList(suite1, suite2));
        
        assertTrue(Vec3.DOWN.equals(
                fishers.get(0).getMeanDirection(), 1e-10));
        assertTrue(Vec3.DOWN.invert().equals(
                fishers.get(1).getMeanDirection(), 1e-10));
        for (FisherValues fv: fishers) {
            assertEquals(4, fv.getN());
            assertEquals(2.8284271247461903, fv.getR(), 1e-10);
            assertEquals(73.15012775459878, fv.getA95(), 1e-10);
            assertEquals(2.560660171779822, fv.getK(), 1e-10);
        }
    }
    
    private static Suite makeSuiteFromDirections(List<Vec3> directions) {
        final Suite suite = new Suite("SuiteTest");
        for (int i=0; i<directions.size(); i++) {
            final Sample sample = new Sample(String.format("%d", i), suite);
            sample.setImportedDirection(directions.get(i));
            suite.addSample(sample, Integer.toString(i));
        }
        return suite;
    }
    
    @Test
    public void testGetSamplesByDiscreteId() {
        for (int i=0; i<syntheticSuite1.getNumSamples(); i++) {
            syntheticSuite1.getSampleByIndex(i).
                    setDiscreteId(i<5 ? "ID1" : "ID2");
        }
        final List<Sample> samplesWithId1 =
                syntheticSuite1.getSamplesByDiscreteId("ID1");
        assertEquals(syntheticSuite1.getSamples().subList(0, 5),
                samplesWithId1);
    }
    
    @Test
    public void testRotateSamplesByDiscreteId() {
        for (int i=0; i<syntheticSuite1.getNumSamples(); i++) {
            syntheticSuite1.getSampleByIndex(i).
                    setDiscreteId(i<5 ? "ID1" : "ID2");
        }
        
        final Function<Suite, List<Vec3>> extractDirections =
                suite -> suite.getSamples().stream().
                        flatMap(sample -> sample.getTreatmentSteps().stream()).
                        map(TreatmentStep::getMoment).collect(Collectors.toList());
        
        final List<Vec3>originalDirections =
                extractDirections.apply(syntheticSuite1);
        final List<Vec3> expectedRotatedDirections = new ArrayList<>();
        for (int i=0; i<originalDirections.size(); i++) {
            expectedRotatedDirections.add(originalDirections.get(i).
                    rotZ(Math.toRadians(i < 50 ? 30 : -30)));
        }
        final Map<String, Double> rotations = Stream.of(
            new SimpleEntry<>("ID1", 30.),  new SimpleEntry<>("ID2", -30.)).
                collect(Collectors.toMap(
                        SimpleEntry::getKey, SimpleEntry::getValue));
        syntheticSuite1.rotateSamplesByDiscreteId(rotations);
        final List<Vec3> actualRotatedDirections =
                extractDirections.apply(syntheticSuite1);
        for (int i=0; i<expectedRotatedDirections.size(); i++) {
            assertTrue(expectedRotatedDirections.get(i).equals(
            actualRotatedDirections.get(i), delta));
        }
    }

    @Test(expected = PuffinUserException.class)
    public void testSaveCalcsSampleNoSamples()
            throws IOException, PuffinUserException {
        final Suite suite = new Suite("test");
        suite.saveCalcsSample(temporaryFolder.newFile());
    }
    
    @Test(expected = PuffinUserException.class)
    public void testSavedCalcsSampleWithIOException()
            throws PuffinUserException {
        syntheticSuite1.getSamples().forEach(s -> {
            s.selectAll();
            s.useSelectionForPca();
            s.doPca(Correction.NONE);
        });
        syntheticSuite1.saveCalcsSample(temporaryFolder.getRoot().toPath().
                resolve("nonexistent").resolve("somefile.csv").toFile());
    }
    
    @Test
    public void testExportToFilesWithFileBlockingDirectory()
            throws IOException {
        final ListHandler handler = ListHandler.createAndAdd();
        final File file = temporaryFolder.newFile("blocks_directory");
        syntheticSuite1.exportToFiles(file,
                Arrays.asList(TreatmentStepField.AREA));
        assertTrue(handler.wasOneMessageLogged(Level.WARNING));
    }
    
    @Test
    public void testExportToFilesInUncreatableDirectory()
            throws IOException {
        final ListHandler handler = ListHandler.createAndAdd();
        final File file = temporaryFolder.newFile("blocks_directory");
        final File specifiedDirectory =
                file.toPath().resolve("desired_subdirectory").toFile();
        syntheticSuite1.exportToFiles(specifiedDirectory,
                Arrays.asList(TreatmentStepField.AREA));
        assertEquals(Level.WARNING, handler.records.get(0).getLevel());
    }
    
    @Test
    public void testFromStringWithMalformedCreationDate() {
        final ListHandler handler = TestUtils.ListHandler.createAndAdd();
        syntheticSuite1.fromString("CREATION_DATE\twibble");
        assertTrue(handler.wasOneMessageLogged(Level.SEVERE));
    }
    
    @Test
    public void testFromStringWithMalformedModificationDate() {
        final ListHandler handler = TestUtils.ListHandler.createAndAdd();
        syntheticSuite1.fromString("MODIFICATION_DATE\twibble");
        assertTrue(handler.wasOneMessageLogged(Level.SEVERE));
    }
    
    @Test
    public void testMergeDuplicateTreatmentStepsWithEmptyList() {
        final Suite suite = new Suite("test");
        suite.mergeDuplicateTreatmentSteps(Collections.emptyList());
    }
    
    @Test
    public void testMergeDuplicateTreatmentStepsWithNoDuplicates() {
        final Suite suite = new Suite("test");
        final Sample sample = new Sample("sample0", suite);
        suite.addSample(sample, "sample0");
        final List<TreatmentStep> expected = new ArrayList<>();
        for (int step=0; step<3; step++) {
            final TreatmentStep d = new TreatmentStep();
            d.setTreatmentType(TreatmentType.THERMAL);
            d.setTemp(step*10);
            sample.addTreatmentStep(d);
            expected.add(d);
        }
        suite.mergeDuplicateTreatmentSteps(Collections.singletonList(sample));
        assertEquals(expected, sample.getTreatmentSteps());
    }

    @Test
    public void testMergeDuplicateTreatmentSteps() {
        final Suite suite = new Suite("test");
        final Sample sample = Mockito.mock(Sample.class);
        suite.addSample(sample, "sample0");
        suite.mergeDuplicateTreatmentSteps(Collections.singletonList(sample));
        Mockito.verify(sample).mergeDuplicateTreatmentSteps();
    }

    @Test
    public void testAlignSectionDeclinations() {
        
        final Suite suite = new Suite("test");
        for (int section = 0; section < 3; section++) {
            final double declination = (section+1)*10;
            for (int sampleIndex = 0; sampleIndex < 10; sampleIndex++) {
                final double depth = section * 10 + sampleIndex;
                final Sample sample = TestUtils.makeOneComponentSample(depth,
                        String.format("SECTION_%d", section),
                        Vec3.fromPolarDegrees(1, 0, declination));
                suite.addSample(sample, String.format("%f", depth));
            }
        }
        
        final double topDeclination = 45;
        
        suite.alignSectionDeclinations(topDeclination, 1);
        
        assertTrue(suite.getSamples().stream().allMatch(
                sample -> sample.getTreatmentSteps().stream().allMatch(
                        datum -> Math.abs(datum.getMoment().getDecDeg() -
                                topDeclination) < delta )));
    }
    
    @Test
    public void testAreSectionEndDirectionsDefined() {
        final List<Sample> samples =
                TestUtils.makeUniformSampleList(Vec3.fromPolarDegrees(1, 40, 20),
                        new double[] {0, 1, 2, 3}, "part0");
        samples.addAll(TestUtils.makeUniformSampleList(Vec3.fromPolarDegrees(1, 50, 30),
                        new double[] {4, 5, 6, 7}, "part1"));
        final Suite suite = new Suite("test");
        samples.forEach(s -> suite.addSample(s, s.getDiscreteId()));
        assertTrue(suite.areSectionEndDirectionsDefined(2));
        assertTrue(suite.areSectionEndDirectionsDefined(3));
        samples.get(1).clearPca();
        assertTrue(suite.areSectionEndDirectionsDefined(1));
        assertFalse(suite.areSectionEndDirectionsDefined(2));
    }
    
    @Test
    public void testMergeDuplicateSamplesWithInverseSuites() {
        final Suite suite0 = TestUtils.createContinuousSuite();
        final Suite suite1 = TestUtils.createContinuousSuite();
        suite1.getSamples().forEach(s -> s.getTreatmentSteps().forEach(
                d -> d.invertMoment()));
        suite1.getSamples().forEach(
                s -> suite0.addSample(s, s.getNameOrDepth()));
        suite0.updateReverseIndex();
        suite0.mergeDuplicateSamples(suite0.getSamples());
        assertTrue(suite0.getSamples().stream().
                flatMap(s -> s.getTreatmentSteps().stream()).
                map(d -> d.getIntensity()).allMatch(x -> x < delta));
    }
    
    @Test
    public void testMergeDuplicateSamplesWithDuplicateSuites() {
        final Suite suite0 = TestUtils.createContinuousSuite();
        final Suite suite1 = TestUtils.createContinuousSuite();
        final List<Vec3> originalDirections = suite0.getSamples().stream().
                flatMap(s -> s.getTreatmentSteps().stream()).
                map(d -> d.getMoment()).
                collect(Collectors.toList());
        suite1.getSamples().forEach(
                s -> suite0.addSample(s, s.getNameOrDepth()));
        suite0.updateReverseIndex();
        suite0.mergeDuplicateSamples(suite0.getSamples());
        final List<Vec3> mergedDirections = suite0.getSamples().stream().
                flatMap(s -> s.getTreatmentSteps().stream()).
                map(d -> d.getMoment()).
                collect(Collectors.toList());
        assertEquals(originalDirections.size(), mergedDirections.size());
        for (int i=0; i<originalDirections.size(); i++) {
            assertTrue(originalDirections.get(i).
                    equals(mergedDirections.get(i), delta));
        }
    }
    
    @Test
    public void testMergeDuplicateSamplesWithTrebledMoments() {
        final Suite suite0 = TestUtils.createContinuousSuite();
        final Suite suite1 = TestUtils.createContinuousSuite();
        final List<Vec3> originalDirections = suite0.getSamples().stream().
                flatMap(s -> s.getTreatmentSteps().stream()).
                map(d -> d.getMoment()).
                collect(Collectors.toList());
        suite1.getSamples().forEach(s -> s.getTreatmentSteps().forEach(
                d -> d.setMoment(d.getMoment().times(3))));
        suite1.getSamples().forEach(
                s -> suite0.addSample(s, s.getNameOrDepth()));
        suite0.updateReverseIndex();
        suite0.mergeDuplicateSamples(suite0.getSamples());
        final List<Vec3> mergedDirections = suite0.getSamples().stream().
                flatMap(s -> s.getTreatmentSteps().stream()).
                map(d -> d.getMoment()).
                collect(Collectors.toList());
        assertEquals(originalDirections.size(), mergedDirections.size());
        for (int i=0; i<originalDirections.size(); i++) {
            assertTrue(originalDirections.get(i).times(2).
                    equals(mergedDirections.get(i), delta));
        }
    }
    
    @Test
    public void testMergeDuplicateSamplesWithDifferentTreatmentSteps() {
        final Suite suite0 = TestUtils.createContinuousSuite();
        final Suite suite1 = TestUtils.createContinuousSuite();
        suite1.getSamples().stream().flatMap(s -> s.getTreatmentSteps().stream()).
                forEach(d -> {
                    d.setAfX(d.getAfX() + 5);
                    d.setAfY(d.getAfY() + 5);
                    d.setAfZ(d.getAfZ() + 5);
                });
        suite1.getSamples().forEach(
                s -> suite0.addSample(s, s.getNameOrDepth()));
        suite0.updateReverseIndex();
        suite0.mergeDuplicateSamples(suite0.getSamples());
        for (Sample s: suite0.getSamples()) {
            assertArrayEquals(new double[] {0, 5, 10, 15, 20, 25, 30,
                35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95},
                    s.getTreatmentLevels(),
                    delta
                );
        }
    }
}
 