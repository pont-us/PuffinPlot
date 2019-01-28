/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2018 Pontus Lurcock.
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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.prefs.Preferences;

import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.Util;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.plots.Plot;
import net.talvi.puffinplot.plots.ZPlot;

/**
 * A graph display which can show multiple plots. This is the display
 * used in PuffinPlot's main window.
 * 
 * @author pont
 */
public class MainGraphDisplay extends GraphDisplay implements Printable {
    private static final long serialVersionUID = 1L;

    // samplesForPrinting is only non-null during printing.
    private List<Sample> samplesForPrinting = null;
    private int printPageIndex = -1;
    private final static String[] plotNames = {"SampleEqAreaPlot", "ZPlot",
        "DemagPlot", "DemagTable", "SampleParamsLegend",
        "PlotTitle", "SiteParamsLegend",
        "AmsPlot", "TernaryPlot", "SiteEqAreaPlot", "SuiteEqAreaPlot",
        "SampleParamsTable", "SiteParamsTable", "NrmHistogram",
        "VgpTable", "SuiteParamsTable",
        "DepthPlot"
    };
    private final PlotParams params;
    private final PuffinApp app;

    MainGraphDisplay(final PuffinApp app) {
        super();
        this.app = app;
        params = app.getPlotParams();
        createPlots();
    }

    /**
     * Deletes all plots and recreates them. Some plots may have settings which
     * are only updated when the plot is created; this method allows them to
     * take notice of changes in these settings without a restart of the whole
     * program.
     */
    public void recreatePlots() {
        plots.clear();
        createPlots();
    }

    private void createPlots() {
        final Preferences pref = app.getPrefs().getPrefs();
        try {
            for (String plotName : plotNames) {
                plots.put(plotName,
                        (Plot) Class.forName("net.talvi.puffinplot.plots." + plotName).
                        getConstructor(GraphDisplay.class, PlotParams.class, Preferences.class).
                        newInstance(this, params, pref));
            }
        } catch (ClassNotFoundException | NoSuchMethodException |
                SecurityException | InstantiationException |
                IllegalAccessException | IllegalArgumentException |
                InvocationTargetException ex) {
            throw new Error(ex);
        }
        // The legend will always be drawn after the Zplot (as required),
        // since LinkedHashMap guarantees iteration in order of insertion.
        plots.put("ZplotLegend", ((ZPlot) plots.get("ZPlot")).getLegend());
    }

    /** Prints this graph display.
     * 
     * @param graphics the graphics object to which to draw the display
     * @param pf the page format
     * @param pageIndex the page number
     * @return {@link #PAGE_EXISTS} if the page number is valid,
     * otherwise {@link #NO_SUCH_PAGE}
     * @throws PrinterException if a printing error occurred
     */
    @Override
    public int print(Graphics graphics, PageFormat pf, int pageIndex)
            throws PrinterException {
        pf.setOrientation(PageFormat.LANDSCAPE);
        if (samplesForPrinting == null) {
            samplesForPrinting = app.getSelectedSamples();
        }
        if (pageIndex >= samplesForPrinting.size()) {
            samplesForPrinting = null; // we've finished printing
            return NO_SUCH_PAGE;
        }
        printPageIndex = pageIndex;
        final Graphics2D g2 = (Graphics2D) graphics;

        if (Util.runningOnOsX()) {
            /* Superscripts don't print properly on OS X (at least not
             * on Java 7 or Java 8u25), so we set a rendering hint that
             * tells the plotting code to fall back to E notation.
             * See bug 698def95-724d-44fb-9088-d3d7192e98ef .
             */
            final RenderingHints rh = g2.getRenderingHints();
            rh.put(PuffinRenderingHints.KEY_E_NOTATION, Boolean.TRUE);
            g2.setRenderingHints(rh);
        }

        printPlots(pf, graphics);
        return PAGE_EXISTS;
    }
    
    /**
     * Writes a requested page of the current suite to a given
     * graphics context. Intended for use when exporting multi-page
     * PDF. Uses a similar model to Java printing, sans {@code PageFormat}.
     * 
     * @param g2 graphics context
     * @param pageIndex the page to output
     * @return {@code true} if this is the highest-numbered page in the printable range;
     *  {@code false} otherwise
     */
    public boolean printPdfPage(Graphics2D g2, int pageIndex) {
        if (samplesForPrinting == null) {
            samplesForPrinting = app.getSelectedSamples();
        }
        printPageIndex = pageIndex;
        setDoubleBuffered(false);
        g2.setPaint(Color.BLACK);
        g2.setPaintMode();
        for (Plot plot: getVisiblePlots()) {
            plot.draw(g2);
        }
        printChildren(g2);
        setDoubleBuffered(true);
        if (pageIndex == samplesForPrinting.size() - 1) {
            samplesForPrinting = null; // we've finished printing
            return true;
        } else {
            return false;
        }
    }
    
    public boolean isPrintingInProgress() {
        return samplesForPrinting != null;
    }
    
    public Sample getCurrentlyPrintingSample() {
        return samplesForPrinting.get(printPageIndex);
    }
}
