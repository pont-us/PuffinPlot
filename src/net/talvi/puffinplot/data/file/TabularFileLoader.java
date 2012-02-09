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
 *
 * @author pont
 */
public class TabularFileLoader extends AbstractFileLoader {
    private final File file;
    private final FileFormat format;
    
    public TabularFileLoader(File file, FileFormat format) {
        this.file = file;
        this.format = format;
        BufferedReader reader = null;
        try {
        reader = new BufferedReader(new FileReader(file));
        List<String> lines = new ArrayList<String>(50);
        while (true) {
            final String line = reader.readLine();
            if (line==null) break;
            lines.add(line);
        }
        data = format.readLines(lines);
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
