package net.talvi.puffinplot.window;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;

/**
 *
 * @author pont
 */
public interface PlotParams {

    public MeasType getMeasType();
    public Sample getSample();
    public Correction getCorrection();
    public MeasurementAxis getAxis();
    public boolean isEmptyCorrectionActive();
    
}
