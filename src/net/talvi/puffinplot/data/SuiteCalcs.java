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
                strings.addAll((i==0 ? all : i==1 ? upper : lower).toStrings());
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
    
    private final Means bySite;
    private final Means bySample;
    
    /**
     * Creates a new SuiteCalcs object.
     * 
     * @param bySite statistics calculated by site
     * @param bySample statistics calculated by sample
     */
    public SuiteCalcs(Means bySite, Means bySample) {
        this.bySite = bySite;
        this.bySample = bySample;
    }
    
    /**
     * @return suite statistics calculated by site
     */
    public Means getBySite() {
        return bySite;
    }

    /**
     * @return suite statistics calculated by sample
     */
    public Means getBySample() {
        return bySample;
    }
    
    /**
     * @return a string-matrix representation of these suite statistics.
     */
    public List<List<String>> toStrings() {
        List<List<String>> result = new ArrayList<>(6);
        for (int i=0; i<2; i++) {
            final Means means = i==0 ? bySite : bySample;
            for (int j=0; j<3; j++) {
                final List<String> strings = new ArrayList<>(8);
                strings.add(i==0 ? "Site" : "Sample");
                strings.addAll(means.toStrings().get(j));
                result.add(strings);
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
