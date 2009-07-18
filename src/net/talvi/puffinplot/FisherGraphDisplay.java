package net.talvi.puffinplot;

import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import net.talvi.puffinplot.plots.FisherEqAreaPlot;
import net.talvi.puffinplot.plots.Plot;

public class FisherGraphDisplay extends GraphDisplay implements Printable {

    public FisherGraphDisplay() {
        super();
        zoomTransform = AffineTransform.getScaleInstance(1.0, 1.0);
        Plot plot = new FisherEqAreaPlot(
                null, null, new Rectangle2D.Double(50, 50, 600, 600));
        plots.put(plot.getName(), plot);
    }

    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
            throws PrinterException {
        if (pageIndex > 0) return NO_SUCH_PAGE;
        else {
            printPlots(pageFormat, graphics);
            return PAGE_EXISTS;
        }
    }
}
