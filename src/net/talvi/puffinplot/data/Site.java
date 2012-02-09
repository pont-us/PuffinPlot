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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data;

import java.util.logging.Logger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import static java.util.Collections.min;
import static java.util.Collections.max;

/**
 * <p>A site is a grouping of samples within a suite. In practice, it usually
 * corresponds to a group of samples from a small physical area.
 * In a discrete study, it usually corresponds to a physical field
 * site within a section. In a long core study, it usually corresponds
 * to a narrow <q>slice</q> of the core between two defined depths.</p> 
 * 
 * @author pont
 */

public class Site {

    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");
    private final String name;
    private final List<Sample> samples;
    private FisherValues fisher;
    private GreatCircles greatCircles;
    private double height = Double.NaN;

    /**
     * Creates a site containing the specified samples.
     * 
     * @param name the name of the site
     * @param samples the samples contained in the site
     */
    public Site(String name, List<Sample> samples) {
        this.name = name;
        this.samples = samples;
    }

    /** Creates a site containing no samples. 
     * @param name the name of the site */
    public Site(String name) {
        this.name = name;
        this.samples = new ArrayList<Sample>();
    }

    /** Calculate Fisherian statistics on the PCA directions of samples
     * within this site. The PCA directions will be automatically calculated
     * (or recalculated) before the Fisher statistics are calculated.
     * The results are stored within the site.
     * 
     * @param correction the correction to apply to the magnetic moment
     * data when performing the PCA calculations
     */
    public void doFisher(Correction correction) {
        Collection<Vec3> directions =
                new ArrayList<Vec3>(getSamples().size());
        for (Sample s: getSamples()) {
            s.doPca(correction);
            if (s.getPcaAnnotated() != null) directions.add(s.getPcaValues().getDirection());
        }
        if (!directions.isEmpty()) {
            fisher = FisherValues.calculate(directions);
        }
    }

    /** Clears the stored Fisher statistics, if any. */
    public void clearFisher() {
        fisher = null;
    }

    /** Calculate a mean direction for the site using best-fit great circles.
     * For each sample at the site, the great circle is incorporated
     * into a calculation of the mean direction. If no great circle 
     * has been fitted for a site, the PCA direction (if any) is used.
     * Mean direction estimate is by the method of McFadden and McElhinny
     * (1988).
     * 
     * @param correction the correction to apply to the magnetic moment
     * data when fitting the great circles.
     * @see GreatCircles
     */
    public void calculateGreatCirclesDirection(Correction correction) {
        List<Vec3> endpoints = new LinkedList<Vec3>();
        LinkedList<GreatCircle> circles = new LinkedList<GreatCircle>();
        for (Sample sample: getSamples()) {
            /* We assume that if there's a great circle then it should be
             * used (and that if a PCA fit is present for the same site, it's
             * not relevant to the component being examined here). If there
             * is no great circle but there *is* a PCA fit, then the PCA
             * direction is used as a stable endpoint.
             */
            if (sample.getGreatCircle() != null) {
                sample.fitGreatCircle(correction); // make sure it's up to date
                circles.add(sample.getGreatCircle());
            } else if (sample.getPcaAnnotated() != null) {
                endpoints.add(sample.getPcaValues().getDirection());
            }
        }
        if (!circles.isEmpty()) {
            greatCircles = new GreatCircles(endpoints, circles);
        }
    }

    /** Returns the name of this site. 
     * @return the name of this site */
    @Override
    public String toString() {
        return name;
    }

    /** Returns the samples in this site 
     * @return the samples in this site */
    public List<Sample> getSamples() {
        return Collections.unmodifiableList(samples);
    }

    /** Returns the Fisher statistics (if any) calculated for this site. 
     * @return the Fisher statistics (if any) calculated for this site */
    public FisherValues getFisher() {
        return fisher;
    }

    /** Returns the great-circle parameters (if any) calculated for this site. 
     * @return the great-circle parameters (if any) calculated for this site */
    public GreatCircles getGreatCircles() {
        return greatCircles;
    }
    
    /**
     * Returns the Fisherian parameters of the site mean direction.
     * If a great-circle mean has been calculated, its parameters will be 
     * returned. If there is no great-circle mean but a Fisher mean has been 
     * calculated, the Fisher mean will be returned. If neither type of mean 
     * has been calculated, a null value will be returned.
     * 
     * @return the Fisherian parameters of the site mean direction
     */
    public FisherParams getMeanDirection() {
        if (fisher != null) return fisher;
        else if (greatCircles != null && greatCircles.isValid())
            return greatCircles;
        else return null;
    }

    private String fmt(double x) {
        return String.format("%g", x);
    }

    /** Returns headers for information on the treatment steps used
     * for the great-circle analysis. See the PuffinPlot user manual
     * for details of their interpretation.
     * 
     * @return headers for information on the treatment steps used
     * for the great-circle analyses
     */
    public static List<String> getGreatCircleLimitHeader() {
        return Arrays.asList(new String[] {"GC D1min (°C or mT)",
            "GC D1max (°C or mT)","GC D2min (°C or mT)","GC D2max (°C or mT)"});
    }

    /** Returns information on the treatment steps used for the 
     * great-circle analysis. The list consists of:
     * minFirstGc, maxFirstGc, minLastGc, MaxLastGc.
     * Where minFirstGc is the minimum (among samples in this site) first 
     * treatment step value for any great-circle fit, and so forth.
     * 
     * @return information on the treatment steps used for the 
     * great-circle analysis
     */
    public List<String> getGreatCircleLimitStrings() {
        final List<Double> firsts = new ArrayList<Double>(samples.size());
        final List<Double> lasts = new ArrayList<Double>(samples.size());
        for (Sample s: samples) {
            final double first = s.getFirstGcStep();
            if (first != -1) firsts.add(first);
            final double last = s.getLastGcStep();
            if (last != -1) lasts.add(s.getLastGcStep());
        }
        return Arrays.asList(new String[] {fmt(min(firsts)),
                fmt(max(firsts)), fmt(min(lasts)), fmt(max(lasts))});
    }

    /** Clears the stored great-circle fit parameters, if any */
    public void clearGcFit() {
        greatCircles = null;
    }

    void addSample(Sample sample) {
        if (sample==null) {
            logger.warning("null sample passed to Suite.addSample.");
            return;
        }
        if (!samples.contains(sample)) {
            samples.add(sample);
            sample.setSite(this);
        }
    }
    
    void removeSample(Sample sample) {
        if (sample==null) {
            logger.warning("null sample passed to Suite.removeSample.");
            return;
        }
        samples.remove(sample);
    }

    Object getName() {
        return name;
    }
    
    /** Reports whether there are any samples in this site. 
     * @return {@code true} if there are no samples in this site */
    public boolean isEmpty() {
        return samples.isEmpty();
    }

    /** Returns a list of strings giving information about this site.
     * @return  a list of strings giving information about this site */
    public List<String> toStrings() {
        List<String> result = new ArrayList<String>();
        if (!Double.isNaN(height)) {
            result.add("HEIGHT\t" + Double.toString(height));
        }
        return result;
    }

    /** Sets site data from information in a string.
     * The format is the same as that exported from {@link #toStrings()}.
     * @param string a string containing site data
     */
    public void fromString(String string) {
        String[] parts = string.split("\t", -1); // don't discard trailing empty strings
        if ("HEIGHT".equals(parts[0])) {
            height = Double.parseDouble(parts[1]);
        }
    }
}
