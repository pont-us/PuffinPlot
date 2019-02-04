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

import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.plots.PlotAxis.AxisParameters;
import net.talvi.puffinplot.window.PlotParams;

/**
 * A simple x/y plot for the entire suite with depth on the x axis and a sample
 * parameter on the y axis. Currently a very basic implementation with
 * inclination hard-coded as the parameter.
 *
 * @author pont
 */
public class DepthPlot extends Plot {

    /**
     * Creates a depth plot with the supplied parameters
     *
     * @param params the parameters of the plot
     */
    public DepthPlot(PlotParams params) {
        super(params);
    }
    
    @Override
    public String getName() {
        return "depth";
    }
    
    @Override
    public String getNiceName() {
        return "Inclination/Depth";
    }

    @Override
    public void draw(Graphics2D graphics) {
        clearPoints();
        final Sample currentSample = params.getSample();
        if (currentSample == null) {
            return;
        }
        final Suite suite = currentSample.getSuite();
        
        final AxisParameters xAxisParams = 
                new AxisParameters(suite.getMaxDepth(), Direction.RIGHT).
                withLabel("Depth").withNumberEachTick();
        final PlotAxis xAxis = new PlotAxis(xAxisParams, this);
        
        double minInc = 0, maxInc = 0, totalInc = 0;
        int numberOfIncs = 0;
        boolean anyData = false;
        for (Sample sample: suite.getSamples()) {
            if (!sample.hasTreatmentSteps()) {
                continue;
            }
            if (sample.getDirection() == null) {
                continue;
            }
            if (Double.isNaN(sample.getDepth())) {
                continue;
            }
            anyData = true;
            final double inc = sample.getDirection().getIncDeg();
            if (inc < minInc) {
                minInc = inc;
            }
            if (inc > maxInc) {
                maxInc = inc;
            }
            if (inc > 30) {
                totalInc += inc;
                numberOfIncs++;
            }
        }
        
        if (!anyData) {
            return;
        }
        
        final AxisParameters upAxisParams = 
                new AxisParameters(maxInc, Direction.UP).
                withLabel("Inc").withNumberEachTick();
        final AxisParameters downAxisParams = 
                new AxisParameters(-minInc, Direction.DOWN).
                withLabel("Inc").withNumberEachTick();
        final PlotAxis upAxis = new PlotAxis(upAxisParams, this);
        final PlotAxis downAxis = new PlotAxis(downAxisParams, this);
        
        final Rectangle2D dim =
                cropRectangle(getDimensions(), 320, 100, 50, 290);
        final double xScale = dim.getWidth() / xAxis.getLength();
        final double yScale =
                dim.getHeight() / (upAxis.getLength() + downAxis.getLength());
        
        for (Sample sample: suite.getSamples()) {
            if (!sample.hasTreatmentSteps()) {
                continue;
            }
            if (sample.getDirection() == null) {
                continue;
            }
            final double depth = sample.getDepth();
            final double inc = sample.getDirection().getIncDeg();
            final double xPos = dim.getMinX() + depth * xScale;
            final double yPos =
                    dim.getMaxY() - downAxis.getLength() - inc * yScale;
            final Point2D point = new Point2D.Double(xPos, yPos);
            final boolean highlight = (sample == currentSample);
            addPoint(null, point, highlight, highlight, true);
        }
        
        graphics.setColor(Color.BLACK);
        upAxis.draw(graphics, yScale, (int)dim.getMinX(),
                (int)(dim.getMaxY() - downAxis.getLength()));
        downAxis.draw(graphics, yScale, (int)dim.getMinX(),
                (int)(dim.getMaxY() - downAxis.getLength()));
        xAxis.draw(graphics, xScale, (int)dim.getMinX(),
                (int)(dim.getMaxY() - downAxis.getLength()));
        drawPoints(graphics);
        
        writeString(graphics, String.format("%f", totalInc / numberOfIncs),
                (float) getDimensions().getMinX(),
                (float) getDimensions().getMinY());
    }
}
