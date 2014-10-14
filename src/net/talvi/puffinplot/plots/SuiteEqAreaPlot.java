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
package net.talvi.puffinplot.plots;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.*;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

/**
 * An equal-area plot data for an entire suite.
 * This plot displays site means calculated using Fisher statistics
 * or great-circle intersections, and overall Fisher means of the
 * site means themselves. If there are site means in both hemispheres,
 * a separate mean is shown for each hemisphere. If no sites are defined,
 * sample PCA directions are used instead.
 */
public class SuiteEqAreaPlot extends EqAreaPlot {

    /** Creates a suite equal area plot with the supplied parameters.
     * 
     * @param parent the graph display containing the plot
     * @param params the parameters of the plot
     * @param prefs the preferences containing the plot configuration
     */
    public SuiteEqAreaPlot(GraphDisplay parent, PlotParams params,
            Preferences prefs) {
        super(parent, params, prefs);
    }

    @Override
    public String getName() {
        return "equarea_suite";
    }
    
    @Override
    public String getNiceName() {
        return "Equal-area (suite)";
    }

    @Override
    public String getShortName() {
        return "Suite";
    }

    private void drawFisher(FisherValues fv) {
        if (fv==null) return;
        final Vec3 mean = fv.getMeanDirection();
        if (fv.getA95() > 0) {
            drawLineSegments(mean.makeSmallCircle(fv.getA95()));
        }
        final PlotPoint meanPoint = 
                ShapePoint.build(this, project(mean)).
                circle().build();
        meanPoint.draw(g);
    }

    private void drawMeans(SuiteCalcs.Means means) {
        if (means.getUpper() != null && means.getLower() != null) {
            drawFisher(means.getUpper());
            drawFisher(means.getLower());
        } else {
            drawFisher(means.getAll());
        }
    }

    private void writePointLabel(String s, PlotPoint point) {
        if (prefs != null &&
                prefs.getBoolean("plots.labelPointsInSuitePlots", false)) {
            final Point2D centre = point.getCentre();
            putText(g, s, centre.getX(), centre.getY(),
                    Direction.RIGHT,
                    0, 6);
        }
    }

    @Override
    public void draw(Graphics2D g) {
        updatePlotDimensions(g);
        clearPoints();
        drawAxes();
        final Sample selectedSample = params.getSample();
        if (selectedSample==null) return;
        final Suite suite = selectedSample.getSuite();
        if (suite==null) return;
        List<Site> sites = suite.getSites();
        final SuiteCalcs suiteCalcs = suite.getSuiteMeans();
        if (sites == null || sites.isEmpty()) {
            for (Sample sample: suite.getSamples()) {
                final Vec3 dir = sample.getDirection();
                if (dir != null) {
                    final PlotPoint p = ShapePoint.build(this, project(dir)).
                            filled(dir.z>0).build();
                    writePointLabel(sample.getNameOrDepth(), p);
                    p.draw(g);
                }
            }
            if (suiteCalcs != null) {
                drawMeans(suiteCalcs.getBySample());
            }
        } else {
            for (Site site: sites) {
                final FisherParams siteMean = site.getFisherParams();
                if (siteMean != null) {
                    final Vec3 dir = siteMean.getMeanDirection();
                    PlotPoint p = ShapePoint.build(this, project(dir)).filled(dir.z>0).build();
                    writePointLabel(site.getName(), p);
                    p.draw(g);
                }
            }
            if (suiteCalcs != null) {
                drawMeans(suiteCalcs.getBySite());
            }
        }
        drawPoints(g);
    }
}
