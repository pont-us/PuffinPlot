package net.talvi.puffinplot;

import net.talvi.puffinplot.plots.DataTable;
import net.talvi.puffinplot.plots.DemagPlot;
import net.talvi.puffinplot.plots.FisherEqAreaPlot;
import net.talvi.puffinplot.plots.SampleEqAreaPlot;
import net.talvi.puffinplot.plots.FisherTable;
import net.talvi.puffinplot.plots.PcaTable;
import net.talvi.puffinplot.plots.SampleTable;
import net.talvi.puffinplot.plots.ZPlot;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;

/**
 *
 * @author pont
 */
public class MainGraphDisplay extends GraphDisplay {

    MainGraphDisplay() {

        super();
        final PuffinApp app = PuffinApp.getInstance();
        PlotParams params = new PlotParams() {

            public Sample getSample() {
                // TODO: looks a bit hacky, fix or document
                return samples == null ? app.getSample() : samples[printPageIndex];
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
        };

        PuffinPrefs pref = PuffinApp.getInstance().getPrefs();
        plots.put("equarea", new SampleEqAreaPlot(this, params, pref.getPlotSize("equarea")));
        plots.put("zplot", new ZPlot(this, params, pref.getPlotSize("zplot")));
        plots.put("demag", new DemagPlot(this, params, pref.getPlotSize("demag")));
        plots.put("datatable", new DataTable(this, params, pref.getPlotSize("datatable")));
        plots.put("pcatable", new PcaTable(this, params, pref.getPlotSize("pcatable")));
        plots.put("sampletable", new SampleTable(this, params, pref.getPlotSize("sampletable")));
        plots.put("fishertable", new FisherTable(this, params, pref.getPlotSize("fishertable")));
    }

}
