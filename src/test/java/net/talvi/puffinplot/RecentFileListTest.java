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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static java.util.Collections.singletonList;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class RecentFileListTest {
    
    private final Map<String, String> store = new HashMap<>();
    private final RecentFileList rfl = makeSimpleFileList();
    private final static String[][] filePaths = {
        {"/path/one/file1"},
        {"/path/with/rather/a/lot/of/elements"+
                "to/test/truncation/of/long/names/filler/filler/filler/"+
                "file2"},
        {"/path/three/dir3/set3file1", "/path/three/dir3/set3file2"}
    };

    @Test
    public void testSaveAndConstruct() {
        rfl.save(store::remove, store::put);
        final RecentFileList rfl2 = new RecentFileList(store::get);
        assertArrayEquals(rfl.getFilesetNames(), rfl2.getFilesetNames());
        assertEquals(rfl.getFilesetLongNames(), rfl2.getFilesetLongNames());
    }

    @Test
    public void testGetFilesetLongNames() {
        final List<String> longNames = rfl.getFilesetLongNames();
        assertEquals(filePaths[0][0], longNames.get(2));
        assertEquals(
                "…" + filePaths[1][0].substring(filePaths[1][0].length() - 40,
                        filePaths[1][0].length()),
                longNames.get(1));
        assertEquals("/path/three/dir3/set3file1, …", longNames.get(0));
    }

    @Test
    public void testGetFilesAndReorder() {
        /*
         * file1 was added first, so should have sunk to third under the two
         * more recent additions.
         */
        assertEquals(singletonList("/path/one/file1"),
                filesToPaths(rfl.getFilesAndReorder(2)));
        /**
         * Fetching file1 should have forced it back to the start of the list.
         */
        assertEquals(singletonList("/path/one/file1"),
                filesToPaths(rfl.getFilesAndReorder(0)));
    }

    @Test
    public void testGetFilesetNames() {
        assertArrayEquals(
                new String[] {"1 set3file1 etc.", "2 file2", "3 file1"},
                rfl.getFilesetNames());
    }
    
    @Test
    public void testAdd() {
        final RecentFileList rfl = new RecentFileList(store::get);
        for (int i=0; i<10; i++) {
            rfl.add(makeFileSet("file" + i));
        }
        final String[] names = rfl.getFilesetNames();
        assertEquals(8, names.length);
        for (int i=0; i<8; i++) {
            assertEquals((i + 1) + " file" + (9 - i), names[i]);
        }
    }
    
    private RecentFileList makeSimpleFileList() {
        final RecentFileList simpleFileList = new RecentFileList(store::get);
        simpleFileList.add(makeFileSet(filePaths[0]));
        simpleFileList.add(makeFileSet(filePaths[1]));
        simpleFileList.add(makeFileSet(filePaths[2]));
        return simpleFileList;
    }
    
    private static final List<String> filesToPaths(List<File> files) {
        return files.stream().map(file -> file.getPath()).
                collect(Collectors.toList());
    }
    
    private static final List<File> makeFileSet(String... names) {
        return Arrays.asList(names).stream().map(name -> new File(name)).
                collect(Collectors.toList());
    }
    
}
