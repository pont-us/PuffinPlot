package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a set of calculations for a suite of data.
 * Specifically, these are various kinds of formation mean direction.
 * 
 * @author pont
 */
public final class SuiteCalcs {

    private static final List<String> HEADERS;
    
    static {
        List<String> headersTemp = new ArrayList<String>();
        headersTemp.add("Type");
        headersTemp.add("Group");
        headersTemp.addAll(FisherValues.getHeaders());
        HEADERS = Collections.unmodifiableList(headersTemp);
    }
    
    public final static class Means {
        private final FisherValues all;
        private final FisherValues upper;
        private final FisherValues lower;
        
        public Means(FisherValues all, FisherValues upper, FisherValues lower) {
            this.all = all;
            this.upper = upper;
            this.lower = lower;
        }

        public FisherValues getAll() {
            return all;
        }

        public FisherValues getUpper() {
            return upper;
        }

        public FisherValues getLower() {
            return lower;
        }
        
        public List<List<String>> toStrings() {
            List<List<String>> result = new ArrayList<List<String>>(3);
            for (int i=0; i<3; i++) {
                final List<String> strings = new ArrayList<String>(8);
                strings.add(i==0 ? "All" : i==1 ? "Upper" : "Lower");
                strings.addAll((i==0 ? all : i==1 ? upper : lower).toStrings());
                result.add(strings);
            }
            return result;
        }
        
        public static Means calculate(Collection<Vec3> directions) {
            final List<Vec3> upperDirs = new ArrayList<Vec3>(directions.size());
            final List<Vec3> lowerDirs = new ArrayList<Vec3>(directions.size());
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
    
    public SuiteCalcs(Means bySite, Means bySample) {
        this.bySite = bySite;
        this.bySample = bySample;
    }
    
    public Means getBySite() {
        return bySite;
    }

    public Means getBySample() {
        return bySample;
    }
    
    public List<List<String>> toStrings() {
        List<List<String>> result = new ArrayList<List<String>>(6);
        for (int i=0; i<2; i++) {
            final Means means = i==0 ? bySite : bySample;
            for (int j=0; j<3; j++) {
                final List<String> strings = new ArrayList<String>(8);
                strings.add(i==0 ? "Site" : "Sample");
                strings.addAll(means.toStrings().get(j));
                result.add(strings);
            }
        }
        return result;
    } 
    
    public static List<String> getHeaders() {
        return HEADERS;
    }
    
}
