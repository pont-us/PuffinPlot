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

import net.talvi.puffinplot.data.KentParams;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.window.PlotParams;

/**
 * A plot which shows the principal axes of anisotropy of magnetic
 * susceptibility (AMS) tensors, and statistical means and confidence
 * regions for groups of tensors.
 */
public class AmsPlot extends EqualAreaPlot {

    private List<KentParams> cachedBootstrapParams;
    private List<KentParams> cachedHextParams;
    private List<List<Vec3>> cachedBootstrapRegions = new ArrayList<>();
    private List<List<Vec3>> cachedHextRegions = new ArrayList<>();    
    
    /**
     * Creates an AMS plot with the supplied parameters.
     *
     * @param params the parameters of the plot
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
    
    /**
     * Draws this plot.
     *
     * @param graphics the graphics context to which to draw the plot
     */
    @Override
    public void draw(Graphics2D graphics) {
        updatePlotDimensions(graphics);
        clearPoints();
        drawAxes();
        final Sample selectedSample = params.getSample();
        if (selectedSample == null) {
            return;
        }

        graphics.setStroke(getStroke());

        List<Sample> samples = params.getAllSamplesInSelectedSites();
        if (samples.isEmpty()) {
            samples = Collections.singletonList(selectedSample);
        }
        for (Sample sampleToPlot: samples) {
            if (sampleToPlot.getAms() != null) {
                for (int i=0; i<3; i++) {
                    Vec3 v = sampleToPlot.getAms().getAxis(i).normalize();
                    if (v.z < 0) {
                         // ensure lower hemisphere
                        v = v.invert();
                    }
                    final Point2D point = project(v);
                    graphics.setColor(Color.GRAY);
                    getPointForAxis(point, PLOT_POINT_SIZE, i).draw(graphics);
                }
            }
        }

        cacheConfidenceRegionsIfRequired(selectedSample.getSuite());
        // Mean directions should be same for Hext and bootstrap.
        final List<KentParams> meanDirections = 
                cachedBootstrapParams != null ?
                cachedBootstrapParams :
                cachedHextParams;
        if (meanDirections != null) {
            for (int axis=0; axis<3; axis+=1) {
                final KentParams kp = meanDirections.get(axis);
                final Point2D pos = project(kp.getMean());
                getPointForAxis(pos, PLOT_POINT_SIZE*3, axis).draw(graphics);
            }
        }
        
        graphics.setStroke(getStroke());
        graphics.setColor(Color.BLACK);
        for (List<Vec3> segment: cachedBootstrapRegions) {
            drawLowerHemisphereLineSegments(graphics, segment);
        }
        graphics.setStroke(getDashedStroke());
        for (List<Vec3> segment: cachedHextRegions) {
            drawLowerHemisphereLineSegments(graphics, segment);
        }
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

    private PlotPoint getPointForAxis(Point2D point, double size, int axis) {
        assert(axis>=0 && axis < 3);
        return new ShapePoint.Builder(this, point).size(size).filled(true).
                pointShape(ShapePoint.PointShape.fromAmsAxis(axis)).
                build();
    }

    /**
     * Draw line segments between a sequence of vectors in the same
     * (upper/lower) hemisphere. If the points are in the lower hemisphere,
     * they are used as supplied. If they are in the upper hemisphere, their
     * inverses in the lower hemisphere are plotted. If supplied vectors
     * are not all in the same hemisphere, the effects of this method are
     * undefined.
     * 
     * @param graphics the graphics context into which to draw
     * @param vectors the directions between which to draw line segments
     */
    private void drawLowerHemisphereLineSegments(Graphics2D graphics,
            List<Vec3> vectors) {
        /*
         * Determine whether we're in upper hemisphere, ignoring z co-ordinates
         * very close to zero. Assumes all segments in same hemisphere, so
         * if *any* vectors are in the lower hemisphere we assume that we're
         * there.
         */
        boolean upperHemisphere = true;
        for (Vec3 v: vectors) {
            if (v.z > 1e-10) {
                upperHemisphere = false;
                break;
            }
        }
        List<Vec3> vectorsToDraw = vectors;
        if (upperHemisphere) {
            /*
             * If we're in the upper hemisphere, invert all vectors before
             * plotting.
             */
            vectorsToDraw = new ArrayList<>(vectors.size());
            for (Vec3 v: vectors) {
                vectorsToDraw.add(v.invert());
            }
        }
        graphics.draw(vectorsToPath(vectorsToDraw));
    }
}
