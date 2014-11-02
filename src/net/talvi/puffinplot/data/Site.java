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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import static java.util.Collections.max;
import static java.util.Collections.min;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

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
    private Location location = null;
    private VGP vgp = null;

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
        this.samples = new ArrayList<>();
    }

    /** Calculate Fisherian statistics on the PCA directions of samples
     * within this site. The PCA directions will be automatically calculated
     * (or recalculated) before the Fisher statistics are calculated,
     * if sufficient points have been selected for the operation.
     * If no PCA direction is available, the sample Fisher mean (if any)
     * and the imported sample direction (if any) are used in turn as
     * fallbacks. The results are stored within the site. If the 
     * site has a location set, the VGP will also be calculated and stored.
     * 
     * @param correction the correction to apply to the magnetic moment
     * data when performing the PCA calculations
     */
    public void calculateFisherStats(Correction correction) {
        Collection<Vec3> directions =
                new ArrayList<>(getSamples().size());
        for (Sample sample: getSamples()) {
            sample.doPca(correction);
            if (sample.getDirection() != null) {
                directions.add(sample.getDirection());
            }
        }
        if (!directions.isEmpty()) {
            fisher = FisherValues.calculate(directions);
        }
        if (getLocation() != null) {
            calculateVgp();
        }
    }

    /** Clears the stored Fisher statistics, if any. */
    public void clearFisherStats() {
        fisher = null;
    }

    /** Calculate a mean direction for the site using best-fit great circles.
     * For each sample at the site, the great circle is incorporated
     * into a calculation of the mean direction. If no great circle 
     * has been fitted for a site, the PCA direction (if any) is used.
     * Mean direction estimate is by the method of McFadden and McElhinny
     * (1988). If the site has a location set, the VGP will also be 
     * calculated and stored.
     * 
     * @param correction the correction to apply to the magnetic moment
     * data when fitting the great circles.
     * @see GreatCircles
     */
    public void calculateGreatCirclesDirection(Correction correction) {
        List<Vec3> endpoints = new LinkedList<>();
        LinkedList<GreatCircle> circles = new LinkedList<>();
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
        if ((endpoints.size() > 0 && circles.size() > 0)
                || circles.size() > 1) {
            greatCircles = new GreatCircles(endpoints, circles);
        }
        if (getLocation() != null) {
            calculateVgp();
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
    public FisherValues getFisherValues() {
        return fisher;
    }

    /** Returns the great-circle parameters (if any) calculated for this site. 
     * @return the great-circle parameters (if any) calculated for this site */
    public GreatCircles getGreatCircles() {
        return greatCircles;
    }
    
    /**
     * Returns the Fisherian parameters of the site mean direction,
     * as calculated by Fisher statistics or great-circle analysis.
     * If a great-circle mean has been calculated, its parameters will be 
     * returned. If there is no great-circle mean but a Fisher mean has been 
     * calculated, the Fisher mean will be returned. If neither type of mean 
     * has been calculated, a null value will be returned.
     * 
     * @return the Fisherian parameters of the site mean direction
     */
    public FisherParams getFisherParams() {
        if (greatCircles != null && greatCircles.isValid()) {
            return greatCircles;
        } else if (fisher != null) {
            return fisher;
        } else {
            return null;
        }
    }

    private String fmt(double x) {
        return String.format(Locale.ENGLISH, "%g", x);
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
        final List<Double> firsts = new ArrayList<>(samples.size());
        final List<Double> lasts = new ArrayList<>(samples.size());
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

    /**
     * @return the name of this site
     */
    public String getName() {
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
        List<String> result = new ArrayList<>();
        if (!Double.isNaN(height)) {
            result.add("HEIGHT\t" + Double.toString(height));
        }
        if (location != null) {
            result.add(String.format("LOCATION\t%s\t%s",
                    Double.toString(location.getLatDeg()),
                    Double.toString(location.getLongDeg())));
        }
        return result;
    }

    /** Sets site data from information in a string.
     * The format is the same as that exported from {@link #toStrings()}.
     * @param string a string containing site data
     */
    public void fromString(String string) {
        String[] parts = string.split("\t", -1); // don't discard trailing empty strings
        switch (parts[0]) {
            case "HEIGHT":
            height = Double.parseDouble(parts[1]);
                break;
            case "LOCATION":
                location = Location.fromDegrees(Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]));
                break;
        }
    }

    /**
     * @return the location
     */
    public Location getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(Location location) {
        this.location = location;
    }
    
    /**
     * Returns the site mean direction.
     * 
     * See {@link #getFisherParams()} for details of how the mean
     * direction is chosen.
     * 
     * @return the site mean direction, or null if none exists
     */
    public Vec3 getMeanDirection() {
        if (getFisherParams() != null) {
            return getFisherParams().getMeanDirection();
        }
        return null;
    }
    
    /**
     * Calculates a virtual geomagnetic pole for the site, if possible.
     * 
     * A site mean and location are necessary to calculate a VGP.
     */
    public void calculateVgp() {
        if (getLocation() != null && getFisherParams() != null) {
            vgp = VGP.calculate(getFisherParams(), getLocation());
        }
    }

    /**
     * @return the site VGP (virtual geomagnetic pole)
     */
    public VGP getVgp() {
        return vgp;
    }
}
