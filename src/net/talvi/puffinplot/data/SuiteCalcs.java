/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2015 Pontus Lurcock.
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a set of calculations for a suite of data.
 * 
 * Specifically, these are various kinds of formation mean direction.
 * 
 * @author pont
 */
public final class SuiteCalcs {

    private static final List<String> HEADERS;
    
    static {
        List<String> headersTemp = new ArrayList<>();
        headersTemp.add("Type");
        headersTemp.add("Group");
        headersTemp.addAll(FisherValues.getHeaders());
        HEADERS = Collections.unmodifiableList(headersTemp);
    }

    /**
     * Fisher statistics for the suite.
     */
    public final static class Means {
        private final FisherValues all;
        private final FisherValues upper;
        private final FisherValues lower;
        
        /**
         * Create a new set of Fisher statistics for a Suite.
         * 
         * @param all statistics for the whole suite
         * @param upper statistics for the upper-hemisphere directions
         * @param lower statistics for the lower-hemisphere directions
         */
        private Means(FisherValues all, FisherValues upper, FisherValues lower) {
            this.all = all;
            this.upper = upper;
            this.lower = lower;
        }

        /**
         * @return Fisher statistics on a whole suite.
         */
        public FisherValues getAll() {
            return all;
        }

        /**
         * @return Fisher statistics on the upper-hemisphere directions in a suite.
         */
        public FisherValues getUpper() {
            return upper;
        }

        /**
         * @return Fisher statistics on the lower-hemisphere directions in a suite.
         */
        public FisherValues getLower() {
            return lower;
        }
        
        /**
         * @return a string-matrix representation of these statistics
         */
        public List<List<String>> toStrings() {
            List<List<String>> result = new ArrayList<>(3);
            for (int i=0; i<3; i++) {
                final List<String> strings = new ArrayList<>(8);
                strings.add(i==0 ? "All" : i==1 ? "Upper" : "Lower");
                FisherValues values = i==0 ? all : i==1 ? upper : lower;
                strings.addAll(FisherValues.toStrings(values));
                result.add(strings);
            }
            return result;
        }
        
        /**
         * Calculate Fisher statistics on a set of directions.
         * 
         * Separate statistics are calculated for the whole set of directions,
         * for the upper-hemisphere directions only, and for the lower-hemisphere
         * directions only.
         * 
         * @param directions directions on which to calculate statistics
         * @return the calculated statistics
         */
        public static Means calculate(Collection<Vec3> directions) {
            final List<Vec3> upperDirs = new ArrayList<>(directions.size());
            final List<Vec3> lowerDirs = new ArrayList<>(directions.size());
            for (Vec3 direction: directions) {
                if (direction.z > 0) {
                    lowerDirs.add(direction);
                } else {
                    upperDirs.add(direction);
                }
            }
            return new Means(FisherValues.calculate(directions),
                    FisherValues.calculate(upperDirs),
                    FisherValues.calculate(lowerDirs));
        }
    }
    
    private final Means dirsBySite;
    private final Means dirsBySample;
    private final Means vgpsBySite;
    private final Means vgpsBySample;
    
    /**
     * Creates a new SuiteCalcs object.
     * 
     * @param dirsBySite direction statistics calculated by site
     * @param dirsBySample direction statistics calculated by sample
     * @param vgpsBySite VGP statistics calculated by site
     * @param vgpsBySample VGP statistics calculated by sample
     */
    public SuiteCalcs(Means dirsBySite, Means dirsBySample,
            Means vgpsBySite, Means vgpsBySample) {
        this.dirsBySite = dirsBySite;
        this.dirsBySample = dirsBySample;
        this.vgpsBySite = vgpsBySite;
        this.vgpsBySample = vgpsBySample;
    }
    
    /**
     * @return suite statistics calculated by site
     */
    public Means getDirsBySite() {
        return dirsBySite;
    }

    /**
     * @return suite statistics calculated by sample
     */
    public Means getDirsBySample() {
        return dirsBySample;
    }
    
    /**
     * @return the vgpsBySite
     */
    public Means getVgpsBySite() {
        return vgpsBySite;
    }

    /**
     * @return the vgpsBySample
     */
    public Means getVgpsBySample() {
        return vgpsBySample;
    }
    
    /**
     * @return a string-matrix representation of these suite statistics.
     */
    public List<List<String>> toStrings() {
        List<List<String>> result = new ArrayList<>(12);
        for (int type=0; type<2; type++) {
            for (int grouping=0; grouping<2; grouping++) {
                final Means means;
                if (type==0 /* direction */) {
                    means = grouping==0 ? dirsBySite : dirsBySample;
                } else /* VGP */ {
                    means = grouping==0 ? vgpsBySite : vgpsBySample;
                }
                for (int hemisphere=0; hemisphere<3; hemisphere++) {
                    final List<String> strings = new ArrayList<>(8);
                    strings.add((grouping==0 ? "Site " : "Sample ") +
                            (type==0 ? "dir" : "VGP"));
                    strings.addAll(means.toStrings().get(hemisphere));
                    result.add(strings);
                }
            }
        }
        return result;
    } 
    
    /**
     * @return headers corresponding to fields returned by {@link #toStrings()}.
     */
    public static List<String> getHeaders() {
        return HEADERS;
    }
    
}
