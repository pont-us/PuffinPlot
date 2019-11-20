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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 * @author pont
 */
public class IdToFileMapTest {
    
    private Map<String, String> backingStore = new HashMap<>();
    private IdToFileMap idToFileMap1;
    private IdToFileMap idToFileMap2;
    private List<File> dirs;
    private static final int nDirs = 5;
    private static final String[] identifiers ={
        "one 1", "t w o", "Three", "FOUR",
        "Five, a slightly longer ID string"};
    private static final String nonExistentKey = "This key does not exist.";
    
    @Before
    public void setUp() throws IOException {
        idToFileMap1 = new IdToFileMap(backingStore::get, backingStore::put);
        idToFileMap2 = new IdToFileMap(backingStore::get, backingStore::put);
        dirs = new ArrayList<>(nDirs);
        for (int i=0; i<nDirs; i++) {
            final Path path = Files.createTempDirectory("puffinplot-test");
            dirs.add(path.toFile());
        }
    }
    
    @After
    public void tearDown() throws BackingStoreException {
        for (File file: dirs) {
            file.delete();
        }
    }

    /**
     * Test of put and get methods of class IdToFileMap.
     */
    @Test
    public void testPutAndGet() {
        for (int i=0; i<nDirs; i++) {
            idToFileMap1.put(identifiers[i], dirs.get(i));
        }
        
        for (int i=0; i<nDirs; i++) {
            final File dir = idToFileMap2.get(identifiers[i]);
            assertEquals(dirs.get(i), dir);
        }
    }

    /**
     * Test of put and getString methods of class IdToFileMap.
     * @throws java.io.IOException
     */
    @Test
    public void testPutAndGetString() throws IOException {
        for (int i=0; i<nDirs; i++) {
            idToFileMap2.put(identifiers[i], dirs.get(i));
        }
        
        for (int i=0; i<nDirs; i++) {
            final String dir = idToFileMap1.getString(identifiers[i]);
            assertEquals(dirs.get(i).getCanonicalPath(), dir);
        }
    }

    /**
     * Test of IdToFileMap behaviour on non-existent keys.
     */
    @Test
    public void testNonexistent() {
        assertNull(idToFileMap1.get(nonExistentKey));
        assertNull(idToFileMap2.getString(nonExistentKey));
    }
    
}
