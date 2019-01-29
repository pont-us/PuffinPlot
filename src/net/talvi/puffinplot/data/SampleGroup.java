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

import java.util.List;

/**
 * An interface for any grouping of samples (at time of writing:
 * a {@code Site}, {@code Suite}, or {@code CoreSection}). Initially
 * it just contains a single method, {@code getSamples}. Eventually
 * some utility methods (e.g. to perform calculations) may be
 * defined here as default methods for the convenience of
 * interface implementer classes.
 * 
 */
public interface SampleGroup {
    
    List<Sample> getSamples();
    
}
