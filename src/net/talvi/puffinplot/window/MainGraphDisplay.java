package net.talvi.puffinplot.window;

import net.talvi.puffinplot.*;
import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.List;
import java.util.prefs.Preferences;
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
        "DemagPlot", "DataTable", "PcaTable", "PlotTitle", "SiteDataTable",
        "AmsPlot", "TernaryPlot", "SiteEqAreaPlot", "SuiteEqAreaPlot",
        "NrmHistogram"};
    private final PlotParams params;

    MainGraphDisplay() {
        super();
        final PuffinApp app = PuffinApp.getInstance();
        params = new PlotParams() {
            public Sample getSample() {
                return samplesForPrinting == null
                        ? app.getSample()
                        : samplesForPrinting.get(printPageIndex);
            }
            public Correction getCorrection() {
                return app.getCorrection();
            }
            public MeasurementAxis getAxis() {
                return app.getMainWindow().controlPanel.getAxis();
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
}
