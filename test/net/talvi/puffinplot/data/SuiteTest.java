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
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.talvi.puffinplot.PuffinUserException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import net.talvi.puffinplot.TestUtils;
import org.junit.Assume;

/**
 *
 * @author pont
 */
public class SuiteTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
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
        syntheticSuite1 = new Suite("SuiteTest");
        for (int depth=0; depth<10; depth++) {
            final String depthString = String.format("%d", depth);
            final Sample sample = new Sample(depthString, syntheticSuite1);
            for (int demag=0; demag<100; demag += 10) {
                final Datum d = new Datum((depth+1.)*(100.-demag),
                        depth*50, demag);
                d.setDepth(depthString);
                d.setSuite(syntheticSuite1);
                d.setMeasType(MeasType.CONTINUOUS);
                d.setAfX(demag);
                d.setAfY(demag);
                d.setAfZ(demag);
                d.setTreatType(TreatType.DEGAUSS_XYZ);
                d.setSample(sample);
                d.setMagSus(depth);
                sample.addDatum(d);
                syntheticSuite1.addDatum(d);
            }
        }
        syntheticSuite1.updateReverseIndex();
        
        syntheticSuite2 = new Suite("SuiteTest");
        for (int sampleIndex=0; sampleIndex<10; sampleIndex++) {
            final String sampleName = String.format("SAMPLE_%d", sampleIndex);
            final Sample sample = new Sample(sampleName, syntheticSuite2);
            for (int demag=0; demag<100; demag += 10) {
                final Datum d = new Datum((sampleIndex+1.)*(100.-demag),
                        sampleIndex*50, demag);
                d.setDiscreteId(sampleName);
                d.setSuite(syntheticSuite2);
                d.setMeasType(MeasType.DISCRETE);
                d.setAfX(demag);
                d.setAfY(demag);
                d.setAfZ(demag);
                d.setTreatType(TreatType.DEGAUSS_XYZ);
                d.setSample(sample);
                d.setMagSus(sampleIndex);
                d.setSampAz(0);
                d.setSampDip(0);
                d.setFormAz(0);
                d.setFormDip(0);
                sample.addDatum(d);
                syntheticSuite2.addDatum(d);
            }
        }
        syntheticSuite2.updateReverseIndex();
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
            final File savedFile = File.createTempFile("puffinplot-test-", ".ppl", temporaryFolder.getRoot());
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
    
    @Test
    public void testNonExistentFile() {
        Path path;
        final File file =
                temporaryFolder.getRoot().toPath().resolve("nonexistent").toFile();
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

        final Suite suite = new Suite("test");

        /*
         * Create and initialize sites.
         */
        final List<Site> sites = new ArrayList<>();
        final double[][] siteLocations = {
            {45, 45},
            {-45, 45},
            {30, 50},
            {-30, -50}
        };
        for (int siteIndex = 0; siteIndex < siteLocations.length; siteIndex++) {
            final Site site = suite.getOrCreateSite(Integer.toString(siteIndex));
            site.setLocation(Location.fromDegrees(siteLocations[siteIndex][0],
                    siteLocations[siteIndex][1]));
            sites.add(site);
        }
        
        /*
         * Create samples and add them to the sites and suite.
         */
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
        
        /*
         * Generate the actual data from the hand-crafted Suite.
         */
        suite.doAllCalculations(Correction.NONE, "true");
        for (Site site: sites) {
            site.calculateFisherStats(Correction.NONE);
        }
        suite.calculateSuiteMeans(suite.getSamples(), suite.getSites());
        
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
                SuiteCalcs.Means.calculate(sites.stream().map((s) ->
                s.getMeanDirection()).collect(Collectors.toList()));
        final SuiteCalcs.Means vgpsBySite =
                SuiteCalcs.Means.calculate(sites.stream().map((s) ->
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
                suite.getSuiteMeans().toStrings());
        }
    
    @Test
    public void testReadDirectionalData() {
        try {
            testReadDirectionalData(MeasType.DISCRETE,
                    "discrete.txt",
                    "SAMPLE1 30 40\nSAMPLE2\t50\t60",
                    new String[] {"SAMPLE1", "SAMPLE2"},
                    new double[][] {{30, 40}, {50, 60}});
            testReadDirectionalData(MeasType.CONTINUOUS,
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

    private void testReadDirectionalData(final MeasType measType,
            final String filename,
            final String fileContents, final String[] sampleNames,
            final double[][] directions) throws IOException {
        final Suite suite = new Suite("test");
        suite.readDirectionalData(Collections.singletonList(
                TestUtils.writeStringToTemporaryFile(filename, fileContents,
                temporaryFolder)));
        assertEquals(sampleNames.length, suite.getSamples().size());
        assertEquals(measType, suite.getMeasType());
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
        final Datum d = new Datum();
        d.setMeasType(null);
        syntheticSuite1.addDatum(d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddDatumUnsetMeasType() {
        final Datum d = new Datum();
        d.setMeasType(MeasType.UNSET);
        syntheticSuite1.addDatum(d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddDatumIncompatibleMeasType() {
        final Datum d = new Datum();
        d.setMeasType(MeasType.DISCRETE);
        syntheticSuite1.addDatum(d);
    }

    @Test
    public void testAddDatumUnknownTreatType() {
        final Datum d = new Datum();
        d.setMeasType(MeasType.CONTINUOUS);
        d.setDepth("0");
        d.setTreatType(TreatType.UNKNOWN);
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
    public void testIsSaved() {
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
    public void testIsFilenameSet() {
        assertFalse(syntheticSuite1.isFilenameSet());
        final Suite suite = new Suite("test");
        loadFileDataIntoSuite(puffinFile1, suite);
        assertTrue(suite.isFilenameSet());
    }
    
    @Test
    public void testSaveWithFilenameSet() throws PuffinUserException {
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
        assertEquals(0, syntheticSuite1.getCustomFlagNames().size());
    }

    @Test
    public void testGetCustomNoteNames() {
        assertEquals(0, syntheticSuite1.getCustomNoteNames().size());
    }
    
    @Test
    public void testGetPuffinFile() {
        final Suite suite = new Suite("test");
        loadFileDataIntoSuite(puffinFile1, suite);
        assertEquals(puffinFile1, suite.getPuffinFile());
    }
    
    @Test
    public void testReadFilesFromDirectory() {
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
        final String siteName = "site1";
        syntheticSuite1.setNamedSiteForSamples(syntheticSuite1.getSamples(),
                siteName);
        assertEquals(1, syntheticSuite1.getSites().size());
        for (Sample sample: syntheticSuite1.getSamples()) {
            assertEquals(siteName, sample.getSite().getName());
        }
        syntheticSuite1.clearSites();
        assertTrue(syntheticSuite1.getSites().isEmpty());
    }

    @Test
    public void testRescaleMagSus() {
        syntheticSuite1.setSaved(true);
        syntheticSuite1.rescaleMagSus(2);
        assertFalse(syntheticSuite1.isSaved());
        for (Sample sample: syntheticSuite1.getSamples()) {
            for (Datum d: sample.getData()) {
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
    public void testDoSiteCalculations() {
        for (Sample sample: syntheticSuite1.getSamples()) {
            for (Datum datum: sample.getData()) {
                datum.setInPca(true);
                datum.setOnCircle(true);
            }
            sample.doPca(Correction.NONE);
            sample.fitGreatCircle(Correction.NONE);
        }
        syntheticSuite1.setSiteNamesByDepth(syntheticSuite1.getSamples(), 5);
        
        // Set a null site to check that this site-less samples are
        // correctly handled.
        syntheticSuite1.getSampleByIndex(0).setSite(null);
        syntheticSuite1.doSiteCalculations(Correction.NONE, "true");
        for (Site site: syntheticSuite1.getSites()) {
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

    @Test
    public void testCalculateAndClearAmsStatisticsHext() throws IOException {
        testCalculateAmsStatistics(Suite.AmsCalcType.HEXT, 1);
        testCalculateAmsStatistics(Suite.AmsCalcType.BOOT, 1);
        testCalculateAmsStatistics(Suite.AmsCalcType.PARA_BOOT, 2);
        assertNotNull(syntheticSuite2.getAmsHextParams());
        assertNotNull(syntheticSuite2.getAmsBootstrapParams());
        syntheticSuite2.clearAmsCalculations();
        assertNull(syntheticSuite2.getAmsHextParams());
        assertNull(syntheticSuite2.getAmsBootstrapParams());
    }
    
    private void testCalculateAmsStatistics(Suite.AmsCalcType calcType,
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
                calcType==Suite.AmsCalcType.HEXT ?
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
                Suite.AmsCalcType.HEXT, script.getCanonicalPath());
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
                Suite.AmsCalcType.HEXT, script.getCanonicalPath());
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
}
