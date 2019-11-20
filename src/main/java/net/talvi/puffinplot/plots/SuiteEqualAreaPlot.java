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
import java.util.List;

import net.talvi.puffinplot.data.FisherParams;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Site;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.SuiteCalcs;
import net.talvi.puffinplot.data.Vec3;

/**
 * An equal-area plot data for an entire suite. This plot displays site means
 * calculated using Fisher statistics or great-circle intersections, and overall
 * Fisher means of the site means themselves. If there are site means in both
 * hemispheres, a separate mean is shown for each hemisphere. If no sites are
 * defined, sample PCA directions are used instead.
 */
public class SuiteEqualAreaPlot extends EqualAreaPlot {

    /**
     * Creates a suite equal area plot with the supplied parameters.
     *
     * @param params the parameters of the plot
     */
    public SuiteEqualAreaPlot(PlotParams params) {
        super(params);
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

    @Override
    public void draw(Graphics2D graphics) {
        updatePlotDimensions(graphics);
        clearPoints();
        drawAxes();

        final Sample selectedSample = params.getSample();
        if (selectedSample==null) {
            return;
        }

        final Suite suite = selectedSample.getSuite();
        if (suite==null) {
            return;
        }

        final List<Site> sites = suite.getSites();
        final SuiteCalcs suiteCalcs = suite.getSuiteMeans();
        final Color highlightColour = (params.getSettingBoolean(
                "plots.highlightCurrentSample", false)) ?
                Color.RED : Color.BLACK;
        
        if (sites == null || sites.isEmpty()) {
            // If there are no sites, we plot sample directions.
            for (Sample sample: suite.getSamples()) {
                final Vec3 direction = sample.getDirection();
                if (direction != null) {
                    final PlotPoint point = ShapePoint.build(this,
                            project(direction)).
                            filled(direction.z > 0).build();
                    graphics.setColor(sample == selectedSample ?
                            highlightColour : Color.BLACK);
                    point.draw(graphics);
                    graphics.setColor(Color.BLACK);
                    writePointLabel(sample.getNameOrDepth(), point);
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
                    if (params.getSettingBoolean(
                            "plots.showSiteA95sOnSuitePlot", false)) {
                        drawSiteA95(siteMean);
                    }
                    final Vec3 meanDirection = siteMean.getMeanDirection();
                    final PlotPoint point = ShapePoint.build(this,
                            project(meanDirection)).
                            filled(meanDirection.z > 0).build();
                    graphics.setColor(site == selectedSample.getSite() ?
                            highlightColour : Color.BLACK);
                    point.draw(graphics);
                    graphics.setColor(Color.BLACK);
                    writePointLabel(site.getName(), point);
                }
            }
            if (suiteCalcs != null) {
                drawMeans(suiteCalcs.getDirsBySite());
            }
        }
        drawPoints(graphics);
    }

    private void drawMeans(SuiteCalcs.Means means) {
        if (means.getUpper() != null && means.getLower() != null) {
            drawFisher(means.getUpper());
            drawFisher(means.getLower());
        } else {
            drawFisher(means.getAll());
        }
    }

    private void drawFisher(FisherParams fisher) {
        if (fisher==null) {
            return;
        }
        final Vec3 mean = fisher.getMeanDirection();
        if (fisher.isA95Valid() && fisher.getA95() > 0) {
            drawLineSegments(mean.makeSmallCircle(fisher.getA95()));
        }
        final PlotPoint meanPoint = 
                ShapePoint.build(this, project(mean)).
                circle().build();
        meanPoint.draw(cachedGraphics);
    }
    
    private void drawSiteA95(FisherParams fisher) {
        if (fisher==null) {
            return;
        }
        if (fisher.isA95Valid() && fisher.getA95() > 0) {
            cachedGraphics.setColor(Color.BLUE);
            drawLineSegments(
                    fisher.getMeanDirection().makeSmallCircle(fisher.getA95()));
        }
    }

    private void writePointLabel(String text, PlotPoint point) {
        if (params.getSettingBoolean("plots.labelPointsInSuitePlots", false)) {
            final Point2D centre = point.getCentre();
            putText(cachedGraphics, text, centre.getX(), centre.getY(),
                    Direction.RIGHT,
                    0, 6);
        }
    }
}
