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
package net.talvi.puffinplot.data.file;

import java.util.List;

import net.talvi.puffinplot.data.TreatmentStep;

/**
 * An interface for classes which read data from files on disk into PuffinPlot's
 * data structures.
 * 
 * @author pont
 */
public interface FileLoader {
    
    /**
     * Returns the treatment steps read from the file.
     *
     * @return the treatment steps read from the file
     */
    public List<TreatmentStep> getTreatmentSteps();
    
    /**
     * Returns any messages produced during the loading process.
     *
     * @return any messages produced during the loading process
     */
    public List<String> getMessages();
    
    /**
     * Returns any lines in the file which were not handled by the loader.
     *
     * @return any lines in the file which were not handled by the loader
     */
    public List<String> getExtraLines();
}
