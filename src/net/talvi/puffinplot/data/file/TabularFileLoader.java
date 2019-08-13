/* This file is part of PuffinPlot, a program for palaeomagnetic
 * treatmentSteps plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.talvi.puffinplot.data.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A loader for a tabular text file with a custom format.
 * 
 * The format is specified by a {@link FileFormat} object.
 * 
 * @author pont
 */
public class TabularFileLoader implements FileLoader {
    
    private final OptionDefinition formatOption =
            new SimpleOptionDefinition(
                    "format", "a description of the file format",
                    FileFormat.class, null, true);
    private final List<OptionDefinition> optionDefinitions =
            Collections.singletonList(formatOption);
    
    /**
     * Reads a file. The loader supports one option, {@code format},
     * which is required. The value of this option must be a
     * {@link FileFormat} object describing the format of the file to be
     * loaded, and may not be {@code null}.
     * 
     * @param file the file from which to read data
     * @param options file loading options (see description)
     */
    @Override
    public LoadedData readFile(File file, Map<String, Object> options) {
        final FileFormat format = (FileFormat) formatOption.getValue(options);
        if (format == null) {
            /*
             * An exception is more appropriate than an empty LoadedData object
             * with a message here: absence of a FileFormat is a program error,
             * not a user action or file reading problem.
             */
            throw new IllegalArgumentException("No file format supplied");
        }        final SimpleLoadedData loadedData = new SimpleLoadedData();
        if (!format.specifiesFullVector()) {
            if (!format.specifiesDirection()) {
                loadedData.addMessage(
                        "The specified fields are not sufficient to specify "
                        + "a direction; missing vector components will be set "
                        + "to zero!");
            } else {
                loadedData.addMessage(
                        "The specified fields specify a magnetization "
                        + "direction but no magnitude. All magnetization "
                        + "intensities will be set to 1.");
            }
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            
            final List<String> lines = new ArrayList<>(50);
            while (true) {
                final String line = reader.readLine();
                if (line == null) {
                    break;
                }
                lines.add(line);
            }
            loadedData.setTreatmentSteps(format.readLines(lines));
        } catch (IOException ex) {
            loadedData.addMessage(ex.getLocalizedMessage());
        } finally {
            return loadedData;
        }
    }
    
    @Override
    public List<OptionDefinition> getOptionDefinitions() {
        return optionDefinitions;
    }
}
