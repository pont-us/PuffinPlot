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
package net.talvi.puffinplot.window;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasurementAxis;
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
        "DepthPlot"};
    private final PlotParams params;

    MainGraphDisplay() {
        super();
        final PuffinApp app = PuffinApp.getInstance();
        params = new PlotParams() {
            @Override
            public Sample getSample() {
                return samplesForPrinting == null
                        ? app.getSample()
                        : samplesForPrinting.get(printPageIndex);
            }
            @Override
            public Correction getCorrection() {
                return app.getCorrection();
            }
            @Override
            public MeasurementAxis getAxis() {
                return app.getMainWindow().getControlPanel().getAxis();
            }
        };
        createPlots();
    }

    /** Deletes all plots and recreates them. Some plots may have 
     * settings which are only updated when the plot is created;
     * this method allows them to take notice of changes in these
     * settings without a restart of the whole program. */
    public void recreatePlots() {
        plots.clear();
        createPlots();
    }

    private void createPlots() {
        Preferences pref = PuffinApp.getInstance().getPrefs().getPrefs();
        try {
            for (String plotName : plotNames) {
                plots.put(plotName,
                        (Plot) Class.forName("net.talvi.puffinplot.plots." + plotName).
                        getConstructor(GraphDisplay.class, PlotParams.class, Preferences.class).
                        newInstance(this, params, pref));
            }
        } catch (Exception ex) {
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
        if (samplesForPrinting == null)
            samplesForPrinting = PuffinApp.getInstance().getSelectedSamples();
        if (pageIndex >= samplesForPrinting.size()) {
            samplesForPrinting = null; // we've finished printing
            return NO_SUCH_PAGE;
        }
        printPageIndex = pageIndex;
        printPlots(pf, graphics);
        return PAGE_EXISTS;
    }
    
    /**
     * Writes a requested page of the current suite to a given
     * graphics context. Intended for use when exporting multi-page
     * PDF. Uses the same model as Java printing, sans {@code PageFormat}.
     * 
     * @param g2 graphics context
     * @param pageIndex the page to output
     * @return {@code true} if this page is invalid (page number too high);
     *  {@code false} otherwise
     */
    public boolean printPdfPage(Graphics2D g2, int pageIndex) {
        if (samplesForPrinting == null)
            samplesForPrinting = PuffinApp.getInstance().getSelectedSamples();
        if (pageIndex >= samplesForPrinting.size()) {
            samplesForPrinting = null; // we've finished printing
            return true;
        }
        printPageIndex = pageIndex;
        setDoubleBuffered(false);
        g2.setPaint(Color.BLACK);
        g2.setPaintMode();
        for (Plot plot: getVisiblePlots()) plot.draw(g2);
        printChildren(g2);
        setDoubleBuffered(true);
        return false;
    }
}
