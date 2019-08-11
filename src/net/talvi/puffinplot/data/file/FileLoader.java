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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An interface implemented by PuffinPlot file loaders. It provides
 * methods for producing a {@link LoadedData} object from a file.
 *
 */
public interface FileLoader {
 
    /**
     * Read a file using a specified set of options.
     * 
     * @param file the file to read
     * @param options file reading options
     * @return a representation of the data within the specified file
     */
    LoadedData readFile(File file, Map<Object, Object> options);
    
    default LoadedData readFile(File file) {
        return readFile(file, Collections.emptyMap());
    }

    default List<OptionDefinition> getOptionDefinitions() {
        return Collections.emptyList();
    }
}
