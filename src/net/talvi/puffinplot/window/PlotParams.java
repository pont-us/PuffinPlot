package net.talvi.puffinplot.window;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;

/**
 * <p>The current parameters for a plot. This interface provides a way
 * for a plot to retrieve <q>dynamic</q> parameters affecting its
 * appearance &endash; that is, parameters which can change after
 * the plot has been created. An object implementing this interface
 * is passed to a plot's constructor; the plot can then call back
 * through this interface to read the current state of the parameters.
 * In the case of the main graph display in PuffinPlot's GUI,
 * the parameters are read from the {@link net.talvi.puffinplot.PuffinApp}
 * object, which in turn gets them from user selections in the control
 * panel.</p>
 * 
 * @author pont
 */
public interface PlotParams {

    /** Returns the sample to plot.
     * @return the sample to plot */
    public Sample getSample();

    /** Returns the correction to be applied to magnetic moment data.
     * @return the correction to be applied to magnetic moment data */
    public Correction getCorrection();

    /** Returns the vertical projection axis for a Zijderveld plot.
     * @return the vertical projection axis for a Zijderveld plot */
    public MeasurementAxis getAxis();

}
