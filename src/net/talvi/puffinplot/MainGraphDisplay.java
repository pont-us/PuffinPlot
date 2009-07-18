package net.talvi.puffinplot;

import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.plots.Plot;
import net.talvi.puffinplot.plots.ZPlot;

public class MainGraphDisplay extends GraphDisplay implements Printable {

    // samplesForPrinting is only non-null during printing.
    private Sample[] samplesForPrinting = null;
    private int printPageIndex = -1;

    MainGraphDisplay() {

        super();
        final PuffinApp app = PuffinApp.getInstance();
        PlotParams params = new PlotParams() {

            public Sample getSample() {
                return samplesForPrinting == null
                        ? app.getSample()
                        : samplesForPrinting[printPageIndex];
            }

            public Correction getCorrection() {
                return app.getCorrection();
            }

            public MeasurementAxis getAxis() {
                return app.getMainWindow().controlPanel.getAxis();
            }

            public MeasType getMeasType() {
                return app.getSuite().getMeasType();
            }

            public boolean isEmptyCorrectionActive() {
                return app.isEmptyCorrectionActive();
            }
        };

        Preferences pref = PuffinPrefs.prefs;

        String[] plotNames = {"SampleEqAreaPlot", "ZPlot", "DemagPlot",
            "DataTable", "PcaTable", "SampleTable", "FisherTable"};

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
        plots.put("ZplotLegend", ((ZPlot) plots.get("ZPlot")).getLegend());
    }

    public int print(Graphics graphics, PageFormat pf, int pageIndex)
            throws PrinterException {
        pf.setOrientation(PageFormat.LANDSCAPE);

        if (samplesForPrinting == null)
            samplesForPrinting = PuffinApp.getInstance().getSelectedSamples();
        if (pageIndex >= samplesForPrinting.length) {
            samplesForPrinting = null; // we've finished printing
            return NO_SUCH_PAGE;
        }
        printPageIndex = pageIndex;
        printPlots(pf, graphics);
        return PAGE_EXISTS;
    }
}
