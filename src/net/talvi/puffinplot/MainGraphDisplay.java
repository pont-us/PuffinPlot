package net.talvi.puffinplot;

import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.plots.Plot;

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

            public boolean isEmptyCorrectionActive() {
                return app.isEmptyCorrectionActive();
            }
        };

        Preferences pref = PuffinPrefs.prefs;

        String[] plotNames = {"SampleEqAreaPlot", "ZPlot", "DemagPlot",
            "DataTable", "PcaTable", "SampleTable", "FisherTable",
            "ZplotLegend"};

        try {
            for (String plotName : plotNames) {
                plots.add((Plot) Class.forName("net.talvi.puffinplot.plots." + plotName).
                        getConstructor(GraphDisplay.class, PlotParams.class, Preferences.class).
                        newInstance(this, params, pref));
            }
        } catch (Exception ex) {
            throw new Error(ex);
        }

    }

}
