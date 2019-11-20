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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.Vec3;

/**
 * An equal-area plot showing suite-level Fisher statistics.
 * This plot is only used in the special Fisher plot window; 
 * {@link SuiteEqualAreaPlot} is used in the main window.
 * 
 * @author pont
 */
public class SeparateSuiteEqualAreaPlot extends EqualAreaPlot {

    private final Stroke dashedStroke =
            new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            10.0f * getUnitSize(), new float[] {1,0,1}, 0);
    
    private final Stroke thinStroke =
            new BasicStroke(getUnitSize() / 2.0f);
    
    private boolean groupedBySite = true;
    
    /**
     * Creates a suite equal-area plot with the supplied parameters.
     *
     * @param params the parameters of the plot
     * @param dimensions the dimensions of this plot
     */
    public SeparateSuiteEqualAreaPlot(PlotParams params, Rectangle2D dimensions) {
        super(params);
        this.dimensions = dimensions;
    }

    /**
     * Returns this plot's internal name.
     *
     * @return this plot's internal name
     */
    @Override
    public String getName() {
        return "fisherplot";
    }

    @Override
    public String getShortName() {
        return "Suite";
    }
    
    /**
     * Draws this plot.
     *
     * @param graphics the graphics object to which to draw the plot
     */
    @Override
    public void draw(Graphics2D graphics) {
        updatePlotDimensions(graphics);
        clearPoints();
        drawAxes();
        final Sample sample = params.getSample();
        if (sample==null) {
            return;
        }
        final Suite suite = sample.getSuite();
        if (suite==null) {
            return;
        }
        final List<FisherValues> fishers = groupedBySite 
                ? suite.getSiteFishers()
                : Collections.singletonList(suite.getSuiteMeans().
                        getDirsBySample().getAll());
        if (fishers==null) {
            return;
        }

        final AlphaComposite translucent =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .05f);
        final AlphaComposite opaque =
                AlphaComposite.getInstance(AlphaComposite.SRC);

        boolean firstPoint = true;
        for (FisherValues fisher: fishers) {
            if (fisher==null) {
                continue;
            }
            final Vec3 vector = fisher.getMeanDirection();
            final Point2D meanPoint = project(vector);
            addPoint(null, meanPoint, vector.z>0, firstPoint, !firstPoint);

            if (fisher.isA95Valid()) {
                final GeneralPath pathToFill =
                        new GeneralPath(GeneralPath.WIND_EVEN_ODD, 72);
                boolean firstEllipsePoint = true;
                final List<Vec3> errorCircle = fisher.getMeanDirection().
                        makeSmallCircle(fisher.getA95());
                final List<Vec3> errorCircleInterpolated = new ArrayList<>();
                for (List<Vec3> vs:
                        Vec3.interpolateEquatorPoints(errorCircle)) {
                    errorCircleInterpolated.addAll(vs);
                }

                for (Vec3 cirecleVector: errorCircleInterpolated) {
                    final Point2D.Double point = project(cirecleVector);
                    if (firstEllipsePoint) {
                        pathToFill.moveTo((float) point.x, (float) point.y);
                    } else {
                        pathToFill.lineTo((float) point.x, (float) point.y);
                    }
                    firstEllipsePoint = false;
                }
                pathToFill.closePath();
                
                graphics.setComposite(translucent);
                graphics.fill(pathToFill);
                graphics.setComposite(opaque);
                drawLineSegments(errorCircle);
            }
            
            final Stroke oldStroke = graphics.getStroke();
            if (groupedBySite) {
                graphics.setStroke(dashedStroke);
                for (Vec3 siteDirection: fisher.getDirections()) {
                    graphics.draw(new Line2D.Double(
                            meanPoint, project(siteDirection)));
                }
                graphics.setStroke(oldStroke);
            } else {
                graphics.setStroke(thinStroke);
                for (Sample selectedSample: params.getSelectedSamples()) {
                    if (selectedSample == null) {
                        continue;
                    }
                    final Vec3 direction = selectedSample.getDirection();
                    if (direction == null) {
                        continue;
                    }
                    final Point2D.Double p = project(direction);
                    graphics.draw(
                            new Line2D.Double(p.x - 10, p.y, p.x + 10, p.y));
                    graphics.draw(
                            new Line2D.Double(p.x, p.y - 10, p.x, p.y + 10));
                }
            }
            firstPoint = false;
        }
        drawPoints(graphics);
    }

    /**
     * Reports whether the Fisher means are grouped by site.
     *
     * @return {@code true} if the graph shows Fisher means are grouped by site;
     * {@code false} if it shows a single Fisher mean calculated from all
     * samples
     */
    public boolean isGroupedBySite() {
        return groupedBySite;
    }

    /**
     * Sets whether the Fisher means are to be grouped by site.
     *
     * @param groupedBySite {@code true} to show Fisher means are grouped by
     * site; {@code false} to show a single Fisher mean calculated from all
     * samples
     */
    public void setGroupedBySite(boolean groupedBySite) {
        this.groupedBySite = groupedBySite;
    }

}
