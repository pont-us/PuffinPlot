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
package net.talvi.puffinplot.window;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;

import net.talvi.puffinplot.plots.Plot;
import net.talvi.puffinplot.plots.SiteEqAreaPlot;

/**
 * A graph display containing a single equal-area plot which shows
 * data for a site.
 * 
 * @see SiteEqAreaPlot
 * @see SiteMeanWindow
 * 
 * @author pont
 */
public class SiteMeanDisplay extends GraphDisplay implements Printable {

    /**
     * Creates a new site mean graph display.
     */
    public SiteMeanDisplay(PlotParams params, Preferences prefs) {
        super();
        setOpaque(true); // content panes must be opaque
        setPreferredSize(new Dimension(600, 600));
        zoomTransform = AffineTransform.getScaleInstance(1.0, 1.0);
        final Plot plot = new SiteEqAreaPlot(params,
                new Rectangle2D.Double(20, 20, 560, 560));
        plot.setVisible(true);
        plots.put(plot.getClass(), plot);
    }

    /**
     * Prints this graph display.
     *
     * @param graphics the graphics object to which to draw the display
     * @param pageFormat the page format
     * @param pageIndex the page number
     * @return {@link #PAGE_EXISTS} if the page number is valid, otherwise
     * {@link #NO_SUCH_PAGE}
     * @throws PrinterException if a printing error occurred
     */
    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
            throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        } else {
            printPlots(pageFormat, graphics);
            return PAGE_EXISTS;
        }
    }
}
