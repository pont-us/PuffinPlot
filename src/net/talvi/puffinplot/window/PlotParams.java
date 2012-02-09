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
