/*
 * This file is part of PuffinPlot, a program for palaeomagnetic
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
package net.talvi.puffinplot.plots;

import net.talvi.puffinplot.data.Sample;

/**
 * An interface for classes which want to be notified of clicks on samples.
 * More precisely, clicks on plot elements representing samples.
 * A class implementing this interface should be registered with the
 * plot of interest by calling
 * {@link Plot#addSampleClickListener(net.talvi.puffinplot.plots.SampleClickListener)}.
 * Once this has been done, the {@link SampleClickListener#sampleClicked(net.talvi.puffinplot.data.Sample) }
 * will be called whenever the user clicks on a plot element representing
 * a sample.
 * 
 * @see Plot#addSampleClickListener(net.talvi.puffinplot.plots.SampleClickListener)
 * @see Plot#removeSampleClickListener(net.talvi.puffinplot.plots.SampleClickListener)
 */
public interface SampleClickListener {
    
    /**
     * Invoked when a sample is clicked in a plot.
     * 
     * @param sample the sample corresponding to the plot element which was clicked
     */
    public void sampleClicked(Sample sample);
}
