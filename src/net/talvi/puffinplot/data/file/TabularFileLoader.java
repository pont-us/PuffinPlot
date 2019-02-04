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
import java.util.List;

/**
 * A loader for a tabular text file with a custom format.
 * 
 * The format is specified by a {@link FileFormat} object.
 * 
 * @author pont
 */
public class TabularFileLoader extends AbstractFileLoader {
    private final File file;
    private final FileFormat format;
    
    /**
     * Creates a new TabularFileLoader.
     * 
     * @param file the file from which to read treatmentSteps
     * @param format the format in which the treatmentSteps is stored in the file
     */
    public TabularFileLoader(File file, FileFormat format) {
        this.file = file;
        this.format = format;
        BufferedReader reader = null;
        if (!format.specifiesFullVector()) {
            if (!format.specifiesDirection()) {
                addMessage("The specified fields are not sufficient to specify\n"
                        + "a direction; all magnetization vectors will be set to zero!");
            } else {
                addMessage("The specified fields specify a magnetization direction\n"
                        + "but no magnitude. All magnetization intensities "
                        + "will be set to 1.");
            }
        }
        try {
            reader = new BufferedReader(new FileReader(file));
            final List<String> lines = new ArrayList<>(50);
            while (true) {
                final String line = reader.readLine();
                if (line==null) break;
                lines.add(line);
            }
            treatmentSteps = format.readLines(lines);
        } catch (IOException ex) {
            addMessage(ex.getLocalizedMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex2) {
                    // do nothing
                }
            }
        }
    }
}
