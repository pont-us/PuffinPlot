/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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
package net.talvi.puffinplot.plots;

import java.util.List;
import java.util.prefs.Preferences;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;

/**
 * The current parameters for a plot. This interface provides a way for a plot
 * to retrieve ‘dynamic’ parameters affecting its appearance – that is,
 * parameters which can change after the plot has been created. An object
 * implementing this interface is passed to a plot's constructor; the plot can
 * then call back through this interface to read the current state of the
 * parameters. In the case of the main graph display in PuffinPlot's GUI, the
 * parameters are read from the {@link net.talvi.puffinplot.PuffinApp} object,
 * which in turn gets them from user selections in the control panel.
 * <p>
 * It is not guaranteed that there will always be a valid value available for
 * each parameter. Methods returning lists may return empty lists, and
 * methods returning single objects may return {@code null}.
 * 
 */
public interface PlotParams {

    /**
     * Returns the sample to plot.
     *
     * @return the sample to plot
     */
    public Sample getSample();
    
    /**
     * @return a list of the currently selected samples. May be empty,
     * but not null
     */
    public List<Sample> getSelectedSamples();

    /**
     * Returns the correction to be applied to magnetic moment data.
     *
     * @return the correction to be applied to magnetic moment data
     */
    public Correction getCorrection();

    /**
     * @return the X axis of the Zijderveld plot's vertical projection
     */
    public MeasurementAxis getVprojXaxis();

    /**
     * @return the X axis of the Zijderveld plot's horizontal projection
     */
    public MeasurementAxis getHprojXaxis();

    /**
     * @return the Y axis of the Zijderveld plot's horizontal projection
     */
    public MeasurementAxis getHprojYaxis();
    
    /**
     * @return a list of all samples in all selected sites (may be empty,
     * but not null)
     */
    public List<Sample> getAllSamplesInSelectedSites();
    
    public Preferences getPreferences();
    
    public float getUnitSize();

}
