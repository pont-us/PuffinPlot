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
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class IdToFileMapTest {
    
    private Preferences prefs;
    private IdToFileMap idToFileMap;
    private List<File> dirs;
    private static final int nDirs = 5;
    private static final String[] identifiers ={
        "one 1", "t w o", "Three", "FOUR",
        "Five, a slightly longer ID string"};
    private static final String nonExistentKey = "This key does not exist.";
    
    public IdToFileMapTest() {
    }
    
    @Before
    public void setUp() throws IOException {
        final Preferences classPrefs =
                Preferences.userNodeForPackage(IdToFileMapTest.class);
        prefs = classPrefs.node("test");
        // There's no actual "test" package corresponding to this node,
        // but it's not going to clash with anything else, and using a
        // separate node allows us to clear it easily after the test.
        idToFileMap = new IdToFileMap(prefs);
        dirs = new ArrayList<>(nDirs);
        for (int i=0; i<nDirs; i++) {
            final Path path = Files.createTempDirectory("puffinplot-test");
            dirs.add(path.toFile());
        }
    }
    
    @After
    public void tearDown() throws BackingStoreException {
        idToFileMap = null;
        // Nobody else should be using this node so we can safely clear it.
        prefs.clear();
        prefs.flush();
        for (File file: dirs) {
            file.delete();
        }
    }

    /**
     * Test of put and get methods of class IdToFileMap.
     */
    @Test
    public void testPutAndGet() {
        System.out.println("put and get");
        
        for (int i=0; i<nDirs; i++) {
            idToFileMap.put(identifiers[i], dirs.get(i));
        }
        
        for (int i=0; i<nDirs; i++) {
            final File dir = idToFileMap.get(identifiers[i]);
            assertEquals(dirs.get(i), dir);
        }
    }

    /**
     * Test of put and getString methods of class IdToFileMap.
     * @throws java.io.IOException
     */
    @Test
    public void testPutAndGetString() throws IOException {
        System.out.println("put and getString");
        
        for (int i=0; i<nDirs; i++) {
            idToFileMap.put(identifiers[i], dirs.get(i));
        }
        
        for (int i=0; i<nDirs; i++) {
            final String dir = idToFileMap.getString(identifiers[i]);
            assertEquals(dirs.get(i).getCanonicalPath(), dir);
        }
    }

    /**
     * Test of IdToFileMap behaviour on non-existent keys.
     */
    @Test
    public void testNonexistent() {
        System.out.println("non-existent key");
        assertNull(idToFileMap.get(nonExistentKey));;
        assertNull(idToFileMap.getString(nonExistentKey));
    }
    
}
