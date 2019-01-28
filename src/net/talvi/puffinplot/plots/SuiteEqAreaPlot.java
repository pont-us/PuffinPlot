/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2018 Pontus Lurcock.
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
import java.util.List;
import java.util.prefs.Preferences;

import net.talvi.puffinplot.data.FisherParams;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Site;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.SuiteCalcs;
import net.talvi.puffinplot.data.Vec3;
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

    private void drawFisher(FisherParams fv) {
        if (fv==null) return;
        final Vec3 mean = fv.getMeanDirection();
        if (fv.isA95Valid() && fv.getA95() > 0) {
            drawLineSegments(mean.makeSmallCircle(fv.getA95()));
        }
        final PlotPoint meanPoint = 
                ShapePoint.build(this, project(mean)).
                circle().build();
        meanPoint.draw(g);
    }
    
    private void drawSiteA95(FisherParams fv) {
        if (fv==null) return;
        if (fv.isA95Valid() && fv.getA95() > 0) {
            g.setColor(Color.BLUE);
            drawLineSegments(fv.getMeanDirection().makeSmallCircle(fv.getA95()));
        }
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
        final Color highlightColour = (prefs != null &&
                prefs.getBoolean("plots.highlightCurrentSample", false)) ?
                Color.RED : Color.BLACK;
        
        if (selectedSample==null) return;
        final Suite suite = selectedSample.getSuite();
        if (suite==null) return;
        List<Site> sites = suite.getSites();
        final SuiteCalcs suiteCalcs = suite.getSuiteMeans();
        if (sites == null || sites.isEmpty()) {
            // If there are no sites, we plot sample directions.
            for (Sample sample: suite.getSamples()) {
                final Vec3 dir = sample.getDirection();
                if (dir != null) {
                    final PlotPoint p = ShapePoint.build(this, project(dir)).
                            filled(dir.z>0).build();
                    g.setColor(sample == selectedSample ?
                            highlightColour : Color.BLACK);
                    p.draw(g);
                    g.setColor(Color.BLACK);
                    writePointLabel(sample.getNameOrDepth(), p);
                }
            }
            if (suiteCalcs != null) {
                drawMeans(suiteCalcs.getDirsBySample());
            }
        } else {
            // If there are sites, we plot them instead of the samples.
            for (Site site: sites) {
                final FisherParams siteMean = site.getFisherParams();
                if (siteMean != null) {
                    if (prefs.getBoolean("plots.showSiteA95sOnSuitePlot", false)) {
                        drawSiteA95(siteMean);
                    }
                    final Vec3 dir = siteMean.getMeanDirection();
                    final PlotPoint p = ShapePoint.build(this, project(dir)).
                            filled(dir.z>0).build();
                    g.setColor(site == selectedSample.getSite() ?
                            highlightColour : Color.BLACK);
                    p.draw(g);
                    g.setColor(Color.BLACK);
                    writePointLabel(site.getName(), p);
                }
            }
            if (suiteCalcs != null) {
                drawMeans(suiteCalcs.getDirsBySite());
            }
        }
        drawPoints(g);
    }
}
