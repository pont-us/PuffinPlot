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
package net.talvi.puffinplot;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * A class to maintain a mapping between identifier strings and directories.
 * 
 * The store is backed by a Java Preferences object supplied to the 
 * constructor. The intended application is keeping track of last-used
 * directories for Open File dialog boxes.
 *
 * @author pont
 */
public class IdToFileMap {
    
    private final Preferences prefs;
    // prefix for preferences keys
    private final static String PREFS_PREFIX = "lastUsedFileDir.";
    private final static Logger logger =
            Logger.getLogger(IdToFileMap.class.getName());

    /**
     * Creates a new IdToFileMap, which wwill store and retrieve mappings
     * using the supplied Preferences object.
     * 
     * @param preferences the Preferences object in which to store the mappings
     */
    public IdToFileMap(Preferences preferences) {
        this.prefs = preferences;
    }
    
    /**
     * Return the directory associated with an identifier.
     * 
     * @param identifier a dialog identifier
     * @return the associated directory, or null if there is none
     */
    public File get(String identifier) {
        final String filename = prefs.get(PREFS_PREFIX + identifier, "");
        if (filename.isEmpty()) {
            return null;
        } else {
            return new File(filename);
        }
    }
    
    /**
     * Return the directory associated with an identifier, as a string.
     * 
     * @param identifier a dialog identifier
     * @return a string giving the canonical path of the associated directory,
     *         or null if there is none
     */
    public String getString(String identifier) {
        final String filename = prefs.get(PREFS_PREFIX + identifier, "");
        if (filename.isEmpty()) {
            return null;
        } else {
            return filename;
        }
    }
    
    /**
     * Set the directory associated with an open dialog identifier.
     * 
     * @param identifier an dialog identifier
     * @param directory the associated directory
     */
    public void put(String identifier, File directory) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(
                    "Supplied file object must be a directory.");
        }
        try {
            prefs.put(PREFS_PREFIX + identifier, directory.getCanonicalPath());
        } catch (IOException exception) {
            logger.log(Level.WARNING,
                    "Could not get directory path.", exception);
        }
    }
}
