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
package net.talvi.puffinplot.data.file;

import java.util.List;
import net.talvi.puffinplot.data.TreatmentStep;

/**
 * A dataset loaded from a file. This interface provides a "raw" list of
 * treatment steps with no grouping into samples or sites. It is designed to be
 * used by file loaders to provide data to the Suite class, which will use it to
 * build a hierarchically structured suite.
 */
public interface LoadedData {
    
    /**
     * Returns the treatment steps read from the file. May be empty, but not
     * null.
     * 
     * @return a list of treatment steps
     */
    List<TreatmentStep> getTreatmentSteps();
    
    /**
     * Returns a list of messages produced during file reading, intended for
     * presentation to the user. Generally these will be errors or warnings
     * about problems encountered while loading the file. The list may be
     * empty, but not null.
     *
     * @return a list of messages produced during file reading
     */
    List<String> getMessages();

    /**
     * Returns a list of non-processed raw lines from the file. These are lines
     * which have not been interpreted by the loader, but which may be
     * interpreted directly by {@link net.talvi.puffinplot.data.Suite}.
     * Currently it is only used to allow {@link PplLoader} to pass sample-,
     * site-, and suite-level data on to the suite, and is ignored by
     * {@code Suite} for all other loader classes.
       * 
     * @return a list of raw extra lines from the file
     */
    List<String> getExtraLines();
    
}
