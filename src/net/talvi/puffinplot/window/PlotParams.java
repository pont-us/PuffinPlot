package net.talvi.puffinplot.window;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;

public interface PlotParams {

    public Sample getSample();
    public Correction getCorrection();
    public MeasurementAxis getAxis();
    
}
