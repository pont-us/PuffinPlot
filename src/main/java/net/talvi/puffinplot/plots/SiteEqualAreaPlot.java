/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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
package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.GreatCircle;
import net.talvi.puffinplot.data.GreatCircles;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Site;
import net.talvi.puffinplot.data.Vec3;

/**
 * An equal-area plot showing data for a site. For each sample at the site, this
 * plot shows the best-fitting great circle (if it has been calculated) and PCA
 * direction (if it has been calculated). The plot also shows the site mean
 * direction as calculated by Fisher statistics or by great-circle analysis.
 *
 * @author pont
 */
public class SiteEqualAreaPlot extends EqualAreaPlot {

    private static final int GC_CACHE_SIZE = 200;
    private FisherValues fisherCache = null;
    private LineCache fisherLineCache = null;
    private GreatCircles gcsCache = null;
    private LineCache gcsLineCache = null;
    private Map<GreatCircle, LineCache> gcCache = new GcCache<>();
    
    private class GcCache<K, V> extends LinkedHashMap<K, V> {
        public GcCache() {
            super(GC_CACHE_SIZE, 0.75f, true);
        }
        @Override
        protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
            return size() > GC_CACHE_SIZE;
        }
    }
    
    /** Creates a site equal area plot with the supplied parameters.
     * 
     * @param params the parameters of the plot
     * @param dimensions the initial dimensions of the plot
     */
    public SiteEqualAreaPlot(PlotParams params, Rectangle2D dimensions) {
        super(params);
        this.dimensions = dimensions;
    }

    /** Creates a site equal area plot with the supplied parameters.
     * 
     * @param params the parameters of the plot
     */
    public SiteEqualAreaPlot(PlotParams params) {
        super(params);
    }
    
    @Override
    public String getName() {
        return "equarea_site";
    }
    
    @Override
    public String getNiceName() {
        return "Equal-area (site)";
    }
    
    @Override
    public String getShortName() {
        return "Site";
    }

    private Color getHighlightColour() {
        return (params.getSettingBoolean(
                "plots.highlightCurrentSample", false)) ?
                Color.RED : Color.BLACK;
    }
    
    private void drawGreatCircles(Site site) {
        final GreatCircles circles = site.getGreatCircles();
        if (circles == null) {
            return;
        }
        final Vec3 meanDir = circles.getMeanDirection();
        assert(meanDir.isFinite());
        double maxRadius = 0;
        final GreatCircle currentGc =
                params.getSample().getGreatCircle();
        for (GreatCircle circle: circles.getCircles()) {
            final Vec3 pole = circle.getPole();
            assert(pole.isFinite());
            cachedGraphics.setColor(
                    circle == currentGc ? getHighlightColour() : Color.BLACK);
            final Vec3 segmentStart =
                    pole.nearestOnCircle(circle.getPoints().get(0));
            final Vec3 segmentEnd = pole.nearestOnCircle(meanDir);
            drawGreatCircleSegment(segmentStart,
                    segmentEnd,circle.lastPoint());
            for (Vec3 p: circle.getPoints()) {
                ShapePoint.build(this, project(p)).
                        scale(0.8).filled(p.z>=0).build().draw(cachedGraphics);
                final LineCache lineCache =
                        projectGreatCircleSegment(p, pole.nearestOnCircle(p));
                lineCache.draw(cachedGraphics);
            }
            final Vec3 nearestPoint = pole.nearestOnCircle(meanDir);
            final double thisRadius = Math.abs(meanDir.angleTo(nearestPoint));
            if (thisRadius > maxRadius) {
                maxRadius = thisRadius;
            }
            if (true || !gcCache.containsKey(circle)) {
                final LineCache lineCache =
                        projectGreatCircleSegment(meanDir, nearestPoint);
                gcCache.put(circle, lineCache);
            }
            gcCache.get(circle).draw(cachedGraphics);
            ShapePoint.build(this, project(pole)).filled(pole.z>0).
                    triangle().build().draw(cachedGraphics);
            cachedGraphics.setColor(Color.BLACK);
        }
        
        final PlotPoint meanPoint = ShapePoint.build(this, project(meanDir))
                .circle().scale(1.5).filled(meanDir.z>0).build();
        meanPoint.draw(cachedGraphics);
        if (!(Double.isNaN(circles.getA95()) ||
                Double.isInfinite(circles.getA95()))) {
            if (circles != gcsCache) {
                final List<Vec3> smallCircle =
                        meanDir.makeSmallCircle(circles.getA95());
                final List<List<Vec3>> segments =
                        Vec3.interpolateEquatorPoints(smallCircle);
                drawLineSegments(smallCircle);
                gcsLineCache = new LineCache(getStroke(), getDashedStroke());
                for (List<Vec3> part : segments) {
                    projectLineSegments(part, gcsLineCache);
                }
                gcsCache = circles;
            }
            gcsLineCache.draw(cachedGraphics);

        }
        final List<String> strings = circles.toStrings();
        writeString(cachedGraphics, strings.get(3) + "/" + strings.get(4),
                xo - radius, yo - radius);
    }
    
    private void writeSampleLabel(Sample s, PlotPoint point) {
        if (params.getSettingBoolean("plots.labelSamplesInSitePlots", false)) {
            final Point2D centre = point.getCentre();
            putText(cachedGraphics, s.getNameOrDepth(),
                    centre.getX(), centre.getY(), Direction.RIGHT,
                    0, 6);
        }
    }
    
    private void drawPcas(Site site) {
        final List<Sample> samples = site.getSamples();
        if (samples == null || samples.isEmpty()) {
            return;
        }
        
        final List<Vec3> sampleDirs = new ArrayList<>(samples.size());
        for (Sample sample : samples) {
            if (sample.getDirection() != null) {
                final Vec3 v = sample.getDirection();
                sampleDirs.add(v);
                final PlotPoint point =
                        ShapePoint.build(this, project(v))
                        .diamond().filled(v.z > 0).build();
                cachedGraphics.setColor(sample == params.getSample() ?
                        getHighlightColour() : Color.BLACK);
                point.draw(cachedGraphics);
                cachedGraphics.setColor(Color.BLACK);
                writeSampleLabel(sample, point);
            }
        }
        if (sampleDirs.isEmpty()) {
            return;
        }
        if (site.getGreatCircles() != null) {
            /*
             * If there's a GC calculation, it takes precedence over a mean of
             * PCAs, sample Fisher means, or imported directions.
             */
            return;
        }
        final FisherValues fisherMean = site.getFisherValues();
        if (fisherMean == null) {
            return;
        }
        final Vec3 meanDir = fisherMean.getMeanDirection();
        final PlotPoint meanPoint =
                ShapePoint.build(this, project(meanDir)).
                circle().scale(1.5).filled(meanDir.z>0).build();
        cachedGraphics.setColor(Color.BLACK);
        meanPoint.draw(cachedGraphics);
        if (fisherMean.getN() > 1) {
            if (fisherMean != fisherCache) {
                fisherLineCache = new LineCache(getStroke(), getDashedStroke());
                if (fisherMean.isA95Valid()) {
                    final List<Vec3> smallCircle =
                            meanDir.makeSmallCircle(fisherMean.getA95());
                    final List<List<Vec3>> segments =
                            Vec3.interpolateEquatorPoints(smallCircle);
                    for (List<Vec3> part : segments) {
                        projectLineSegments(part, fisherLineCache);
                    }
                }
                fisherCache = fisherMean;
            }
            fisherLineCache.draw(cachedGraphics);
        }
    }

    @Override
    public void draw(Graphics2D graphics) {
        updatePlotDimensions(graphics);
        drawAxes();
        clearPoints();
        final Sample sample = params.getSample();
        if (sample == null) {
            return;
        }
        final Site site = sample.getSite();
        if (site == null) {
            writeString(graphics, "No sites defined.", xo - 40, yo - 20);
            return;
        }
        drawGreatCircles(site);
        drawPcas(site);
        drawPoints(graphics);
    }
    
    @Override
    public void setDimensions(Rectangle2D dimensions) {
        fisherCache = null;
        gcsCache = null;
        if (!gcCache.isEmpty()) {
            gcCache = new GcCache<>();
        }
        super.setDimensions(dimensions);
    }
}
