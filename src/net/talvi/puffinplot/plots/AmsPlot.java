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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

import net.talvi.puffinplot.data.KentParams;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

/**
 * A plot which shows the principal axes of anisotropy of magnetic
 * susceptibility (AMS) tensors, and statistical means and confidence
 * regions for groups of tensors.
 */
public class AmsPlot extends EqAreaPlot {

    private List<KentParams> cachedBootstrapParams;
    private List<KentParams> cachedHextParams;
    private List<List<Vec3>> cachedBootstrapRegions = new ArrayList<>();
    private List<List<Vec3>> cachedHextRegions = new ArrayList<>();    
    
    /**
     * Creates an AMS plot with the supplied parameters.
     *
     * @param parent the graph display containing the plot
     * @param params the parameters of the plot
     * @param prefs the preferences containing the plot configuration
     */
    public AmsPlot(PlotParams params) {
        super(params);
    }

    @Override
    public String getName() {
        return "ams";
    }

    @Override
    public String getNiceName() {
        return "AMS";
    }

    @Override
    public String getShortName() {
        return "AMS";
    }
    
    private List<List<Vec3>> paramsToSegments(List<KentParams> kps) {
        List<List<Vec3>> result = new ArrayList<>();
        if (kps != null) {
            for (KentParams kp: kps) {
                List<List<Vec3>> segments =
                        Vec3.interpolateEquatorPoints(Vec3.makeEllipse(kp));
                result.addAll(segments);
            }
        }
        return result;
    }
    
    private void cacheConfidenceRegionsIfRequired(Suite suite) {
        final List<KentParams> bootstrapParams = suite.getAmsBootstrapParams();     
        if (bootstrapParams != cachedBootstrapParams) {
            cachedBootstrapParams = bootstrapParams;
            cachedBootstrapRegions = paramsToSegments(bootstrapParams);
        }
        final List<KentParams> hextParams = suite.getAmsHextParams();
        if (hextParams != cachedHextParams) {
            cachedHextParams = hextParams;
            cachedHextRegions = paramsToSegments(hextParams);
        }
    }

    private PlotPoint getPointForAxis(Point2D pos, double size, int axis) {
        assert(axis>=0 && axis < 3);
        return new ShapePoint.Builder(this, pos).size(size).filled(true).
                pointShape(ShapePoint.PointShape.fromAmsAxis(axis)).
                build();
    }

    private void drawLhLineSegments(Graphics2D g, List<Vec3> vs) {
        /*
         * Determine whether we're in upper hemisphere, ignoring z co-ordinates
         * very close to zero. Assumes all segments in same hemisphere.
         */
        boolean upperHemisphere = true;
        for (Vec3 v: vs) {
            if (v.z > 1e-10) {
                upperHemisphere = false;
                break;
            }
        }
        List<Vec3> vs2 = vs;
        if (upperHemisphere) {
            vs2 = new ArrayList<>(vs.size());
            for (Vec3 v: vs) {
                vs2.add(v.invert());
            }
        }
        g.draw(vectorsToPath(vs2));
    }

    /**
     * Draws this plot.
     *
     * @param g the graphics context to which to draw the plot
     */
    @Override
    public void draw(Graphics2D g) {
        updatePlotDimensions(g);
        clearPoints();
        drawAxes();
        final Sample sample = params.getSample();
        if (sample == null) return;

        g.setStroke(getStroke());

        List<Sample> samples = params.getAllSamplesInSelectedSites();
        if (samples.isEmpty()) samples = Collections.singletonList(sample);
        for (Sample s: samples) {
            if (s.getAms() != null) {
                for (int i=0; i<3; i++) {
                    Vec3 v = s.getAms().getAxis(i).normalize();
                    if (v.z < 0) v = v.invert(); // ensure lower hemisphere
                    final Point2D pos = project(v);
                    g.setColor(Color.GRAY);
                    getPointForAxis(pos, PLOT_POINT_SIZE, i).draw(g);
                }
            }
        }

        cacheConfidenceRegionsIfRequired(sample.getSuite());
        // Mean directions should be same for Hext and bootstrap
        final List<KentParams> meanDirections = 
                cachedBootstrapParams != null ?
                cachedBootstrapParams :
                cachedHextParams;
        if (meanDirections != null) {
            for (int i=0; i<3; i+=1) {
                final KentParams kp = meanDirections.get(i);
                final Point2D pos = project(kp.getMean());
                getPointForAxis(pos, PLOT_POINT_SIZE*3, i).draw(g);
            }
        }
        
        g.setStroke(getStroke());
        g.setColor(Color.BLACK);
        for (List<Vec3> segment: cachedBootstrapRegions) {
            drawLhLineSegments(g, segment);
        }
        g.setStroke(getDashedStroke());
        for (List<Vec3> segment: cachedHextRegions) {
            drawLhLineSegments(g, segment);
        }
    }
}
