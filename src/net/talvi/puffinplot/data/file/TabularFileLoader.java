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
