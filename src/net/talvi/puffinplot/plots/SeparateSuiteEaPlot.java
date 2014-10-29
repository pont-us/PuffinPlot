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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.PcaValues;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

/**
 * An equal-area plot showing suite-level Fisher statistics.
 * This plot is only used in the special Fisher plot window; 
 * {@link SuiteEqAreaPlot} is used in the main window.
 * 
 * @author pont
 */
public class SeparateSuiteEaPlot extends EqAreaPlot {

    private final Stroke dashedStroke =
            new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            10.0f * getUnitSize(), new float[] {1,0,1}, 0);
    
    private final Stroke thinStroke =
            new BasicStroke(getUnitSize() / 2.0f);
    
    private boolean groupedBySite = true;
    
    /** Creates a suite equal-area plot with the supplied parameters.
     * 
     * @param parent the graph display containing the plot
     * @param params the parameters of the plot
     * @param dimensions the dimensions of this plot
     * @param prefs the preferences containing the plot configuration
     */
    public SeparateSuiteEaPlot(GraphDisplay parent, PlotParams params,
            Rectangle2D dimensions, Preferences prefs) {
        super(parent, params, prefs);
        this.dimensions = dimensions;
    }

    /** Returns this plot's internal name.
     * @return this plot's internal name */
    @Override
    public String getName() {
        return "fisherplot";
    }

    @Override
    public String getShortName() {
        return "Suite";
    }
    
    /** Draws this plot. 
     * @param g the graphics object to which to draw the plot
     */
    @Override
    public void draw(Graphics2D g) {
        updatePlotDimensions(g);
        clearPoints();
        drawAxes();
        Suite suite = PuffinApp.getInstance().getSuite();
        if (suite==null) return;
        List<FisherValues> fishers = groupedBySite 
                ? suite.getSiteFishers()
                : Collections.singletonList(suite.getSuiteMeans().getBySample().getAll());
        if (fishers==null) return;

        final AlphaComposite translucent =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .05f);
        final AlphaComposite opaque =
                AlphaComposite.getInstance(AlphaComposite.SRC);

        boolean firstPoint = true;
        for (FisherValues fisher: fishers) {
            final Vec3 v = fisher.getMeanDirection();
            final Point2D meanPoint = project(v);
            addPoint(null, meanPoint, v.z>0, firstPoint, !firstPoint);

            if (fisher.isA95Valid()) {
                final GeneralPath ellipse =
                        new GeneralPath(GeneralPath.WIND_EVEN_ODD, 72);
                boolean firstEllipsePoint = true;
                for (double dec = 0; dec < 360; dec += 5) {
                    Vec3 circlePoint =
                            (Vec3.fromPolarDegrees(1, 90-fisher.getA95(), dec));
                    Vec3 w = circlePoint.rotY(Math.PI / 2 - v.getIncRad());
                    w = w.rotZ(v.getDecRad());
                    Point2D.Double p = project(w);
                    if (firstEllipsePoint) {
                        ellipse.moveTo((float) p.x, (float) p.y);
                    } else {
                        ellipse.lineTo((float) p.x, (float) p.y);
                    }
                    firstEllipsePoint = false;
                }
                ellipse.closePath();
                
                g.setComposite(translucent);
                g.fill(ellipse);
                g.setComposite(opaque);
                final Stroke oldStroke = g.getStroke();
                g.setStroke(thinStroke);
                g.draw(ellipse);
                g.setStroke(oldStroke);
            }
            
            final Stroke oldStroke = g.getStroke();
            if (groupedBySite) {
                g.setStroke(dashedStroke);
                for (Vec3 w : fisher.getDirections()) {
                    g.draw(new Line2D.Double(meanPoint, project(w)));
                }
                g.setStroke(oldStroke);
            } else {
                g.setStroke(thinStroke);
                for (Sample s: PuffinApp.getInstance().getSelectedSamples()) {
                    if (s == null) continue;
                    final Vec3 direction = s.getDirection();
                    if (direction == null) continue;
                    final Point2D.Double p = project(direction);
                    g.draw(new Line2D.Double(p.x - 10, p.y, p.x + 10, p.y));
                    g.draw(new Line2D.Double(p.x, p.y - 10, p.x, p.y + 10));
                }
            }
            firstPoint = false;
        }
        drawPoints(g);
    }

    /**
     * Reports whether the Fisher means are grouped by site.
     * 
     * @return {@code true} if the graph shows Fisher means are grouped by site; 
     * {@code false} if it shows a single Fisher mean calculated from all samples
     */
    public boolean isGroupedBySite() {
        return groupedBySite;
    }

    /**
     * Sets whether the Fisher means are to be grouped by site.
     * 
     * @param groupedBySite {@code true} to show Fisher means are grouped by site; 
     * {@code false} to show a single Fisher mean calculated from all samples
     */
    public void setGroupedBySite(boolean groupedBySite) {
        this.groupedBySite = groupedBySite;
    }

}
