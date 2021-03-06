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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot. If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data;

/**
 *  An estimate of relative palaeointensity for a single sample.
 */
public interface SampleRpiEstimate {
    
    /**
     *
     * @return a string of comma-separated values representing an RPI
     * estimate (and any associated data) for a sample
     * 
     * @see #getCommaSeparatedHeader() 
     */
    String toCommaSeparatedString();

    /**
     * 
     * @return a string of comma-separated headers defining the interpretation
     * of the values returned by {@link #toCommaSeparatedString()}
     * 
     * @see #toCommaSeparatedString()
     */
    String getCommaSeparatedHeader();
    
    /**
     *
     * @return an estimate of the sample's RPI
     */
    double getEstimate();
    
    /**
     * @return the depth of the sample
     */
    Sample getNrmSample();
}
