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
import java.awt.geom.Rectangle2D;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.plots.PlotAxis.AxisParameters;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

/**
 *
 * @author pont
 */
public class DepthPlot extends Plot {

    private Preferences prefs;
    
    /** Creates a depth plot with the supplied parameters
     * 
     * @param parent the graph display containing the plot
     * @param params the parameters of the plot
     * @param prefs the preferences containing the plot configuration
     */
    public DepthPlot(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
        this.prefs = prefs;
    }
    
    @Override
    public String getName() {
        return "depth";
    }

    @Override
    public void draw(Graphics2D g) {
        clearPoints();
        final Sample currentSample = params.getSample();
        if (currentSample == null) return;
        final Suite suite = currentSample.getSuite();
        
        final AxisParameters xAxisParams = 
                new AxisParameters(suite.getMaxDepth(), Direction.RIGHT).
                withLabel("Depth").withNumberEachTick();
        final PlotAxis xAxis = new PlotAxis(xAxisParams, this);
        
        double minInc = 0, maxInc = 0;
        boolean anyData = false;
        for (Sample s: suite.getSamples()) {
            if (!s.hasData()) continue;
            if (s.getDirection() == null) continue;
            if (Double.isNaN(s.getDepth())) continue;
            anyData = true;
            final double inc = s.getDirection().getIncDeg();
            if (inc < minInc) minInc = inc;
            if (inc > maxInc) maxInc = inc;
        }
        if (!anyData) return;
        
        final AxisParameters upAxisParams = 
                new AxisParameters(maxInc, Direction.UP).
                withLabel("Inc").withNumberEachTick();
        final AxisParameters downAxisParams = 
                new AxisParameters(-minInc, Direction.DOWN).
                withLabel("Inc").withNumberEachTick();
        final PlotAxis upAxis = new PlotAxis(upAxisParams, this);
        final PlotAxis downAxis = new PlotAxis(downAxisParams, this);
        
        final Rectangle2D dim = cropRectangle(getDimensions(), 320, 100, 50, 290);
        double xScale = dim.getWidth() / xAxis.getLength();
        final double yScale = dim.getHeight() / (upAxis.getLength() + downAxis.getLength());
        
        for (Sample s: suite.getSamples()) {
            if (!s.hasData()) continue;
            if (s.getDirection() == null) continue;
            final double depth = s.getDepth();
            final double inc = s.getDirection().getIncDeg();
            final double xPos = dim.getMinX() + depth * xScale;
            final double yPos = dim.getMaxY() - downAxis.getLength() - inc * yScale;
            final Point2D p = new Point2D.Double(xPos, yPos);
            final boolean highlight = (s == currentSample);
            addPoint(null, p, highlight, highlight, true);
        }
        
        upAxis.draw(g, yScale, (int)dim.getMinX(), (int)(dim.getMaxY() - downAxis.getLength()));
        downAxis.draw(g, yScale, (int)dim.getMinX(), (int)(dim.getMaxY() - downAxis.getLength()));
        xAxis.draw(g, xScale, (int)dim.getMinX(), (int)(dim.getMaxY() - downAxis.getLength()));
        drawPoints(g);
    }
}
