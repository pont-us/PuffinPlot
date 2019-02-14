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
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class to maintain a mapping between identifier strings and files.
 * <p>
 * This is essentially a thin wrapper around a supplied getter and putter
 * method; in the PuffinPlot Swing application, these are supplied by a
 * java.util.prefs.Preferences object, and the IdToFileMap is used to
 * keep track of the last-used directory for various Open File dialogs.
 *
 * @author pont
 */
public class IdToFileMap {
    
    private final UnaryOperator<String> getter;
    private final BiConsumer<String, String> putter;
    // prefix for keys
    private final static String PREFS_PREFIX = "lastUsedFileDir.";
    private final static Logger logger =
            Logger.getLogger(IdToFileMap.class.getName());

    /**
     * Creates a new IdToFileMap, which wwill store and retrieve mappings
     * using the supplied getter and putter.
     * 
     * @param getter a getter method which returns a value for a provided
     * key. For a non-existent key, it should return an empty string.
     * @param putter a fuction which takes a key and a value, and stores
     * the value under the given key.
     */
    
    public IdToFileMap(UnaryOperator<String> getter,
            BiConsumer<String, String> putter) {
        this.getter = getter;
        this.putter = putter;
    }
    
    /**
     * Return the directory associated with an identifier.
     * 
     * @param identifier an identifier
     * @return the associated directory, or null if there is none
     */
    public File get(String identifier) {
        final String filename = getter.apply(PREFS_PREFIX + identifier);
        if (filename == null || filename.isEmpty()) {
            return null;
        } else {
            return new File(filename);
        }
    }
    
    /**
     * Return the directory associated with an identifier, as a string.
     * 
     * @param identifier an identifier
     * @return a string giving the canonical path of the associated directory,
     *         or null if there is none
     */
    public String getString(String identifier) {
        final String filename = getter.apply(PREFS_PREFIX + identifier);
        if (filename == null || filename.isEmpty()) {
            return null;
        } else {
            return filename;
        }
    }
    
    /**
     * Set the directory associated with an identifier.
     * 
     * @param identifier an identifier (non-null)
     * @param directory the associated directory (non-null)
     */
    public void put(String identifier, File directory) {
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(directory);
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(
                    "Supplied file object must be a directory.");
        }
        try {
            putter.accept(PREFS_PREFIX + identifier,
                    directory.getCanonicalPath());
        } catch (IOException exception) {
            logger.log(Level.WARNING,
                    "Could not get directory path.", exception);
        }
    }
}
