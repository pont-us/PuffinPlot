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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot. If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.OptionalDouble;
import net.talvi.puffinplot.data.SampleRpiEstimate;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.SuiteRpiEstimate;

/**
 * A plot which shows the current suite RPI estimate.
 */
public class RpiPlot extends Plot {

    /**
     * Instantiates a new RPI plot.
     * 
     * @param params the plot parameters controlling this plot's content
     */
    public RpiPlot(PlotParams params) {
        super(params);
    }
    
    @Override
    public String getName() {
        return "rpiplot";
    }

    @Override
    public String getNiceName() {
        return "RPI plot";
    }

    @Override
    public void draw(Graphics2D graphics) {
        clearPoints();
        final SuiteRpiEstimate suiteRpi = params.getSuiteRpiEstimate();
        if (suiteRpi == null) {
            return;
        }
        final Suite suite = suiteRpi.getNrmSuite();
        if (suite == null) {
            return;
        }
         
        final PlotAxis.AxisParameters xAxisParams = new PlotAxis
                .AxisParameters(suite.getMaxDepth(), Direction.RIGHT)
                .withLabel("Depth")
                .withNumberEachTick();
        final PlotAxis xAxis = new PlotAxis(xAxisParams, this);
        
        final List<SampleRpiEstimate> sampleRpis = suiteRpi.getRpis();
        final OptionalDouble optionalMaxRpi =
                sampleRpis.stream().mapToDouble(rpi -> rpi.getEstimate()).max();
        if (!optionalMaxRpi.isPresent()) {
            return;
        }
        final double maxRpi = optionalMaxRpi.getAsDouble();
        
        final PlotAxis.AxisParameters upAxisParams =
                new PlotAxis.AxisParameters(maxRpi, Direction.UP).
                        withLabel("RPI").withNumberEachTick();
        final PlotAxis upAxis = new PlotAxis(upAxisParams, this);

        final Rectangle2D dim =
                cropRectangle(getDimensions(), 320, 100, 50, 290);
        final double xScale = dim.getWidth() / (xAxis.getLength());
        final double yScale =
                dim.getHeight() / upAxis.getLength();

        int i = 0;
        final Path2D path = new Path2D.Double(Path2D.WIND_EVEN_ODD,
                sampleRpis.size());
        for (SampleRpiEstimate sampleRpi : sampleRpis) {
            final double depth = sampleRpi.getNrmSample().getDepth();
            final double rpi = sampleRpi.getEstimate();
            final double xPos = dim.getMinX() + depth * xScale;
            final double yPos = dim.getMaxY() - rpi * yScale;
            if (i == 0) {
                path.moveTo(xPos, yPos);
            } else {
                path.lineTo(xPos, yPos);
            }
            i++;
        }

        graphics.setColor(Color.BLACK);
        upAxis.draw(graphics, yScale, (int) dim.getMinX(),
                (int) (dim.getMaxY()));
        xAxis.draw(graphics, xScale, (int) dim.getMinX(),
                (int) (dim.getMaxY()));
        graphics.draw(path);
    }
}
