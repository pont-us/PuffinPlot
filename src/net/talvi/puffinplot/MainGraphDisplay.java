package net.talvi.puffinplot;

import java.util.prefs.Preferences;
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

        Preferences pref = PuffinApp.getInstance().getPrefs().prefs;

        for (int i=0; i<7; i++) {
            plots.add(
                    i==0 ? new SampleEqAreaPlot(this, params, pref)
                    : i == 1 ? new ZPlot(this, params, pref)
                    : i == 2 ? new DemagPlot(this, params, pref)
                    : i == 3 ? new DataTable(this, params, pref)
                    : i == 4 ? new PcaTable(this, params, pref)
                    : i == 5 ? new SampleTable(this, params, pref)
                    : /*i==6*/ new FisherTable(this, params, pref)
                    );
        }

    }

}
