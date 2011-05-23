package net.talvi.puffinplot.data;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import static java.util.Collections.min;
import static java.util.Collections.max;

public class Site {

    private final String name;
    private final List<Sample> samples;
    private FisherValues fisher;
    private GreatCircles greatCircles;

    public Site(String name, List<Sample> samples) {
        this.name = name;
        this.samples = samples;
    }

    public void doFisher() {
        Collection<Vec3> directions =
                new ArrayList<Vec3>(getSamples().size());
        for (Sample s: getSamples()) {
            s.doPca();
            if (s.getPca() != null) directions.add(s.getPcaValues().getDirection());
        }
        if (!directions.isEmpty()) {
            fisher = FisherValues.calculate(directions);
        }
    }

    public void doGreatCircle() {
        List<Vec3> endpoints = new LinkedList<Vec3>();
        LinkedList<List<Vec3>> circles = new LinkedList<List<Vec3>>();
        for (Sample sample: getSamples()) {
            if (sample.getPca() != null) {
                endpoints.add(sample.getPcaValues().getDirection());
            } else if (sample.greatCircle != null) {
                circles.add(sample.getCirclePoints());
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
        return Arrays.asList(new String[] {"T1min","T1max","T2min","T2max"});
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

}
