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
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.talvi.puffinplot.TestUtils.ListHandler;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

public class SiteTest {

    private List<Sample> standardSamples;

    @Before
    public void setUp() {
        standardSamples = new ArrayList<>();
        for (int i=0; i<10; i++) {
            final Sample sample = new Sample("sample"+i, null);
            sample.setImportedDirection(Vec3.fromPolarDegrees(1, 40+i, 40+i));
            standardSamples.add(sample);
        }        
    }
    
    private static List<Sample> makeGcSamples() {
        final List<Sample> samples = new ArrayList<>();
        for (int i=0; i<5; i++) {
            final Sample sample = new Sample("sample"+i, null);
            sample.setImportedDirection(Vec3.fromPolarDegrees(1, 40+i, 40+i));
            samples.add(sample);
            for (int j=0; j<5; j++) {
                final Datum d = new Datum(Vec3.fromPolarDegrees(1, 30+10*j, 20*i));
                d.setTreatType(TreatType.DEGAUSS_Z);
                d.setAfZ(j*10);
                d.setOnCircle(true);
                sample.addDatum(d);
            }
            sample.fitGreatCircle(Correction.NONE);
        }
        final Sample pcaSample = new Sample("pca sample", null);
        for (int j=0; j<5; j++) {
            final Datum d = new Datum(Vec3.fromPolarDegrees(j, 90, 0));
            d.setTreatType(TreatType.DEGAUSS_Z);
            d.setAfZ(j*10);
            d.setInPca(true);
            pcaSample.addDatum(d);
        }
        pcaSample.doPca(Correction.NONE);
        
        samples.add(pcaSample);
        return samples;
    }
    
    @Test
    public void testCalculateAndGetFisherStats() {
        final Site site = new Site("site0", standardSamples);
        final List<String> expectedStats = FisherValues.calculate(
                standardSamples.stream().map(s -> s.getImportedDirection()).
                collect(Collectors.toList())).toStrings();
        site.addSample(new Sample("without direction", null));
        site.calculateFisherStats(Correction.NONE);        
        assertEquals(expectedStats, site.getFisherValues().toStrings());
        assertSame(site.getFisherValues(), site.getFisherParams());
        site.clearFisherStats();
        assertNull(site.getFisherValues());
    }
    
    @Test
    public void testCalculateFisherStatsWithNoDirection() {
        final Site site = new Site("site0");
        site.calculateFisherStats(Correction.NONE);
        assertNull(site.getFisherParams());
    }

    @Test
    public void testClearFisherStats() {
        final Site site = new Site("site0", standardSamples);
        site.calculateFisherStats(Correction.NONE);
        assertNotNull(site.getFisherValues());
        assertNotNull(site.getFisherParams());
        site.clearFisherStats();
        assertNull(site.getFisherValues());
        assertNull(site.getFisherParams());
    }

    @Test
    public void testCalculateGreatCirclesDirection() {
        final List<Sample> samples = makeGcSamples();
        final Site site = new Site("site0", samples);
        final GreatCircles expected = GreatCircles.instance(
                samples.subList(samples.size()-1, samples.size()).stream().
                        map(s -> s.getPcaValues().getDirection()).
                        collect(Collectors.toList()),
                samples.subList(0, samples.size()-1).
                        stream().map(s -> s.getGreatCircle()).
                        collect(Collectors.toList()), "true");
        site.calculateGreatCirclesDirection(Correction.NONE, "true");
        final GreatCircles actual = site.getGreatCircles();
        assertEquals(expected.toStrings(), actual.toStrings());
    }

    @Test
    public void testToStringsAndFromString() {
        final Site site0 = new Site("site0");
        final Location location = Location.fromDegrees(20, 30);
        site0.setLocation(location);
        final List<String> strings = site0.toStrings();
        final Site site1 = new Site("site1");
        strings.forEach(string -> site1.fromString(string));
        assertTrue(location.toVec3().equals(site1.getLocation().toVec3(), 1e-10));
    }

    @Test
    public void testFromStringAndToStringsWithHeight() {
        final Site site = new Site("site0", standardSamples);
        site.fromString("HEIGHT\t7");
        final List<String> strings = site.toStrings();
        assertTrue(strings.contains("HEIGHT\t7.0"));
    }
    
    @Test
    public void testGetSamples() {
        final Site site = new Site("site0", standardSamples);
        assertEquals(standardSamples, site.getSamples());
    }

    @Test
    public void testGetGreatCircleLimitHeader() {
        assertEquals("GC D1min (degC or mT),GC D1max (degC or mT),"
                + "GC D2min (degC or mT),GC D2max (degC or mT)",
                Site.getGreatCircleLimitHeader().stream().
                        collect(Collectors.joining(",")));
    }

    @Test
    public void testGetGreatCircleLimitStrings() {
        final List<Sample> samples = makeGcSamples();
        final Site site = new Site("site0", samples);
        assertEquals(Arrays.asList("0.00000", "0.00000", "40.0000", "40.0000"),
                site.getGreatCircleLimitStrings());
    }

    @Test
    public void testClearGcFit() {
        final Site site = new Site("site0", makeGcSamples());
        site.calculateGreatCirclesDirection(Correction.NONE, "true");
        assertNotNull(site.getGreatCircles());
        site.clearGcFit();
        assertNull(site.getGreatCircles());

    }

    @Test
    public void testGetName() {
        final String name = "site0";
        final Site site = new Site(name, standardSamples);
        assertEquals(name, site.getName());
    }

    @Test
    public void testIsEmpty() {
        final Site emptySite = new Site("Empty site");
        assertTrue(emptySite.isEmpty());
        final Site nonEmptySite = new Site("Non-empty site", standardSamples);
        assertFalse(nonEmptySite.isEmpty());
    }

    @Test
    public void testSetAndGetLocation() {
        final Site site = new Site("site0");
        final Location location = Location.fromDegrees(20, 30);
        site.setLocation(location);
        assertEquals(location, site.getLocation());
    }

    @Test
    public void testGetMeanDirection() {
        final Site site = new Site("site0", standardSamples);
        site.calculateFisherStats(Correction.NONE);
        assertEquals(site.getFisherParams().getMeanDirection(),
                site.getMeanDirection());
    }

    @Test
    public void testGetMeanDirectionWithNoData() {
        final Site site = new Site("site0");
        assertNull(site.getMeanDirection());
    }
    
    @Test
    public void testCalculateAndGetVgp() {
        final Site site = new Site("site0", standardSamples);
        final Location location = Location.fromDegrees(20, 30);
        site.setLocation(location);
        site.calculateFisherStats(Correction.NONE);
        site.calculateVgp();
        final FisherValues fv = FisherValues.calculate(
                standardSamples.stream().map(s -> s.getImportedDirection()).
                collect(Collectors.toList()));
        final VGP vgp = VGP.calculate(fv, location);
        assertEquals(vgp.toStrings(), site.getVgp().toStrings());
    }
    
    @Test
    public void testCalculateVgpWithNoData() {
        final Site site = new Site("site0", standardSamples);
        site.calculateVgp();
        assertNull(site.getVgp());
    }
    
    @Test
    public void testAddSample() {
        final Site site = new Site("site0");
        final Sample sample = new Sample("test", null);
        site.addSample(sample);
        assertEquals(Collections.singletonList(sample), site.getSamples());
        site.addSample(sample);
        assertEquals(Collections.singletonList(sample), site.getSamples());        
    }
    
    @Test
    public void testAddSampleNull() {
        final ListHandler handler = ListHandler.createAndAdd();
        final Site site = new Site("site0");
        site.addSample(null);
        assertTrue(handler.wasOneMessageLogged(Level.WARNING));
    }

    @Test
    public void testRemoveSample() {
        final List<Sample> samplesBackup = new ArrayList<>(standardSamples);
        final Site site = new Site("site0", standardSamples);
        Sample removeMe = standardSamples.get(3);
        site.removeSample(removeMe);
        assertEquals(samplesBackup.stream().filter(s -> s != removeMe).
                collect(Collectors.toList()),
                site.getSamples());
    }
    
    @Test
    public void testRemoveSampleNull() {
        final ListHandler handler = ListHandler.createAndAdd();
        final Site site = new Site("site0");
        site.removeSample(null);
        assertTrue(handler.wasOneMessageLogged(Level.WARNING));        
    }
}
