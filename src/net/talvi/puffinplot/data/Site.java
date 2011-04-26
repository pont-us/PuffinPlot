package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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
}
