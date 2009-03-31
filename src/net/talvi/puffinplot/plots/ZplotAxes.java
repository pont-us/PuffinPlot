package net.talvi.puffinplot.plots;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Collections;
import static net.talvi.puffinplot.plots.PlotAxis.Direction.*;

import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.plots.PlotAxis.Direction;

public class ZplotAxes {

    private final static Direction[] directions =
            new Direction[] { RIGHT, DOWN, LEFT, UP };
    private final double scale;
    private final double xOffset, yOffset;
    private final PlotAxis[] axes;
    
    public ZplotAxes(Rectangle2D dataArea, Rectangle2D plotArea,
            MeasurementAxis vVs, Plot plot) {
        super();

        Rectangle2D.Double extDataArea = new Rectangle2D.Double();
        
        // We copy the data area into an extended data area, which will be
        // grown to include the origin and the full extents of the axes.
        extDataArea.setRect(dataArea);
        extDataArea.add(new Point2D.Double(0,0));
        
        // Get the axis lengths
        double[] lengths = new double[] {
            extDataArea.getMaxX(),
            -extDataArea.getMinY(),
            -extDataArea.getMinX(),
            extDataArea.getMaxY()
        };
        
        // Generate the correct labels for the axes
        String[] labels = new String[4];
        for (int i=0; i<4; i++) {
            Direction dir = directions[i];
            labels[i] = dir.getCompassDir()+",";
            if (dir.isHorizontal()) {
                switch (vVs) {
                    case X: labels[i] += dir.rotAcw90().getCompassDir(); break;
                    case Y: labels[i] += dir.getCompassDir(); break;
                    case H: labels[i] += "H"; break;
                }
            }
            else labels[i] += dir.getLetter();
        }
        
        // calculate step sizes
        Double[] stepSizes = new Double[4];
        for (int i=0; i<4; i++) stepSizes[i] = PlotAxis.saneStepSize(lengths[i]);
        
        // We need a uniform step size or the plot will look pretty odd.
        double step = Collections.max(Arrays.asList(stepSizes));
        
        axes = new PlotAxis[4];
        for (int i=0; i<4; i++)
            axes[i] = new PlotAxis(lengths[i], directions[i], step, null, labels[i], plot);
        
        extDataArea.setRect(-axes[2].getLength(), -axes[1].getLength(),
                axes[0].getLength()+axes[2].getLength(),
                axes[1].getLength()+axes[3].getLength());
        
        double xScale = plotArea.getWidth() / extDataArea.getWidth();
        double yScale = plotArea.getHeight() / extDataArea.getHeight();
        // we want to keep the plot square, so we use the smaller
        // scaling factor for both directions
        scale = Math.min(xScale, yScale);
        
        xOffset = plotArea.getMinX() - extDataArea.getMinX() * scale;
        yOffset = plotArea.getMaxY() + extDataArea.getMinY() * scale;
    }

    public double getScale() {
        return scale;
    }
    
    public Rectangle getBounds() {
        // x, y, width, height
        return new Rectangle(
                (int) (xOffset - axes[2].getLength() * scale),
                (int) (yOffset - axes[3].getLength() * scale),
                (int) ((axes[0].getLength() + axes[2].getLength()) * scale),
                (int) ((axes[1].getLength() + axes[3].getLength()) * scale));
    }
    
    public void draw(Graphics2D g) {
        for (PlotAxis a: axes) a.draw(g, scale, (int)getXOffset(),
                (int) (getYOffset()) );
    }

    public double getXOffset() {
        return xOffset;
    }

    public double getYOffset() {
        return yOffset;
    }
}
