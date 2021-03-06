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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import net.talvi.puffinplot.data.MeasurementAxis;

import static net.talvi.puffinplot.plots.Direction.DOWN;
import static net.talvi.puffinplot.plots.Direction.LEFT;
import static net.talvi.puffinplot.plots.Direction.RIGHT;
import static net.talvi.puffinplot.plots.Direction.UP;

class ZplotAxes {

    private final static Direction[] directions = {RIGHT, DOWN, LEFT, UP};
    private final double scale;
    private final double xOffset, yOffset;
    private final PlotAxis[] axes;
    
    public ZplotAxes(Rectangle2D dataArea, Rectangle2D plotArea,
            MeasurementAxis vVs, MeasurementAxis[] hprojAxes, Plot plot) {
        super();

        final Rectangle2D.Double extDataArea = new Rectangle2D.Double();
        
        /*
         * We copy the data area into an extended data area, which will be grown
         * to include the origin and the full extents of the axes.
         */
        extDataArea.setRect(dataArea);
        extDataArea.add(new Point2D.Double(0,0));
        
        // Get the axis lengths.
        final double[] lengths = new double[] {
            extDataArea.getMaxX(),
            -extDataArea.getMinY(),
            -extDataArea.getMinX(),
            extDataArea.getMaxY()
        };

        
        // Generate the correct labels for the axes.
        final String[] labels = new String[4];
        for (int i=0; i<4; i++) {
            final Direction direction = directions[i];
            
            // Look up the horizontal label.
            final String hprojLabel =
                    hprojAxes[i].getDirection().getCompassDir();
            String vprojLabel = null;
            
            // Determine the vertical label.
            if (direction.isHorizontal()) {
                // The horizontal axis depends on the current projection.
                switch (vVs) {
                    case X:
                        vprojLabel = direction.rotAcw90().getCompassDir();
                        break;
                    case Y:
                        vprojLabel = direction.getCompassDir();
                        break;
                    case H:
                        vprojLabel = "H";
                        break;
                    default:
                        throw new Error("No such axis " + vVs.name());
                }
            } else {
                // Vertical axis is invariant: set up or down as appropriate.
                vprojLabel = direction.getLetter();
            }
            
            if (hprojLabel.equals(vprojLabel)) {
                labels[i] = hprojLabel;
            } else {
                labels[i] = hprojLabel + "," + vprojLabel;
            }
        }
        
        final PlotAxis.AxisParameters[] aps = new PlotAxis.AxisParameters[4];

        for (int i=0; i<4; i++) {
            aps[i] = new PlotAxis.AxisParameters(lengths[i], directions[i]).
                    withEndLabel(labels[i]);
        }

        axes = PlotAxis.makeMatchingAxes(aps, plot);

        extDataArea.setRect(-axes[2].getLength(), -axes[1].getLength(),
                axes[0].getLength()+axes[2].getLength(),
                axes[1].getLength()+axes[3].getLength());
        
        double xScale = plotArea.getWidth() / extDataArea.getWidth();
        double yScale = plotArea.getHeight() / extDataArea.getHeight();
        /*
         * We want to keep the plot square, so we use the smaller scaling factor
         * for both directions.
         */
        scale = Math.min(xScale, yScale);
        
        xOffset = plotArea.getMinX() - extDataArea.getMinX() * scale;
        yOffset = plotArea.getMaxY() + extDataArea.getMinY() * scale;
    }

    public double getScale() {
        return scale;
    }

    public int getMagnitude() {
        /*
         * Magnitudes should all be equal, so it doesn't matter which axis we
         * use here.
         */
        return axes[0].getMagnitude();
    }
    
    public Rectangle getBounds() {
        // x, y, width, height
        return new Rectangle(
                (int) (xOffset - axes[2].getLength() * scale),
                (int) (yOffset - axes[3].getLength() * scale),
                (int) ((axes[0].getLength() + axes[2].getLength()) * scale),
                (int) ((axes[1].getLength() + axes[3].getLength()) * scale));
    }
    
    public void draw(Graphics2D graphics) {
        for (PlotAxis axis: axes) {
            axis.draw(graphics, scale,
                    (int) getXOffset(), (int) (getYOffset()));
        }
    }

    public double getXOffset() {
        return xOffset;
    }

    public double getYOffset() {
        return yOffset;
    }
}
