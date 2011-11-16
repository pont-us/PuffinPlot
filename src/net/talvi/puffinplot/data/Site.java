package net.talvi.puffinplot.data;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import static java.util.Collections.min;
import static java.util.Collections.max;

public class Site {

    private final String name;
    private final List<Sample> samples;
    private FisherValues fisher;
    private GreatCircles greatCircles;
    private double height = Double.NaN;

    public Site(String name, List<Sample> samples) {
        this.name = name;
        this.samples = samples;
    }

    public Site(String name) {
        this.name = name;
        this.samples = new ArrayList<Sample>();
    }

    public void doFisher(Correction correction) {
        Collection<Vec3> directions =
                new ArrayList<Vec3>(getSamples().size());
        for (Sample s: getSamples()) {
            s.doPca(correction);
            if (s.getPca() != null) directions.add(s.getPcaValues().getDirection());
        }
        if (!directions.isEmpty()) {
            fisher = FisherValues.calculate(directions);
        }
    }

    public void clearFisher() {
        fisher = null;
    }

    public void doGreatCircle(Correction correction) {
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
            } else if (sample.getPca() != null) {
                endpoints.add(sample.getPcaValues().getDirection());
            }
        }
        if (!circles.isEmpty()) {
            greatCircles = new GreatCircles(endpoints, circles);
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public List<Sample> getSamples() {
        return samples;
    }

    public FisherValues getFisher() {
        return fisher;
    }

    public GreatCircles getGreatCircles() {
        return greatCircles;
    }

    private String fmt(double x) {
        return String.format("%g", x);
    }

    public static List<String> getGreatCircleLimitHeader() {
        return Arrays.asList(new String[] {"GC D1min (°C or mT)",
            "GC D1max (°C or mT)","GC D2min (°C or mT)","GC D2max (°C or mT)"});
    }

    /**
     * Return, as a list of strings:
     * minFirstGc, maxFirstGc, minLastGc, MaxLastGc
     * minimum (among samples in this site) first treatment step
     * value for great-circle fit, etc.
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

    public void clearGcFit() {
        greatCircles = null;
    }

    void addSample(Sample sample) {
        if (!samples.contains(sample)) {
            samples.add(sample);
            sample.setSite(this);
        }
    }

    Object getName() {
        return name;
    }

    public List<String> toStrings() {
        List<String> result = new ArrayList<String>();
        if (!Double.isNaN(height)) {
            result.add("HEIGHT\t" + Double.toString(height));
        }
        return result;
    }

    public void fromString(String string) {
        String[] parts = string.split("\t", -1); // don't discard trailing empty strings
        if ("HEIGHT".equals(parts[0])) {
            height = Double.parseDouble(parts[1]);
        }
    }
}
