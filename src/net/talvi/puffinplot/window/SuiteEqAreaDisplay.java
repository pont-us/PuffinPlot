package net.talvi.puffinplot.window;

import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.plots.Plot;
import net.talvi.puffinplot.plots.SeparateSuiteEaPlot;

/**
 * A graph display which contains a single equal-area plot of Fisher means
 * at suite level.
 * 
 * @see net.talvi.puffinplot.plots.SeparateSuiteEaPlot
 * @see FisherWindow
 * @author pont
 */
public class SuiteEqAreaDisplay extends GraphDisplay implements Printable {
    private static final long serialVersionUID = 1L;

    /** Creates a new suite equal-area graph display */
    public SuiteEqAreaDisplay() {
        super();
        zoomTransform = AffineTransform.getScaleInstance(1.0, 1.0);
        Plot plot = new SeparateSuiteEaPlot(
                null, null, new Rectangle2D.Double(50, 50, 600, 600),
                PuffinApp.getInstance().getPrefs().getPrefs());
        plot.setVisible(true);
        plots.put(plot.getName(), plot);
    }

    /** Prints this graph display.
     * 
     * @param graphics the graphics object to which to draw the display
     * @param pageFormat the page format
     * @param pageIndex the page number
     * @return {@link #PAGE_EXISTS} if the page number is valid,
     * otherwise {@link #NO_SUCH_PAGE}
     * @throws PrinterException if a printing error occurred
     */
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
            throws PrinterException {
        if (pageIndex > 0) return NO_SUCH_PAGE;
        else {
            printPlots(pageFormat, graphics);
            return PAGE_EXISTS;
        }
    }
}
