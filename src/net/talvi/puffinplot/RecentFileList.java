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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * RecentFileList manages a list of file-sets. It is intended to be used to
 * manage a collection of recently used files for convenient re-opening by a
 * user. Note that each item in the list can comprise multiple files. The length
 * of the list is currently hard-wired to 8, though this would be trivial to
 * change if necessary.
 * <p>
 * RecentFileList can load and store data to a backing store which maps Strings
 * to Strings; in normal use in the PuffinPlot desktop application, a
 * {@link java.util.prefs.Preferences} object fulfils this role. The constructor
 * and save method take functional interfaces as arguments, so any object
 * providing put, get, and remove methods for a String-to-String map can be used
 * as a store. RecentFileList prefixes its keys with the string
 * {@code recentFileX}, where X is a non-negative integer less than the maximum
 * number of file-sets. Note that RecentFileList does not synchronize with the
 * store, except on instantiation (when it is initialized from the supplied
 * getter) and on a call to {@code save} (when it writes to the store using
 * the supplied remover and putter).
 * 
 * @author pont
 */
public class RecentFileList {

    private static final int MAX_LENGTH = 8;
    private final LinkedList<FileSet> fileSets;

    /**
     * Creates a new file list, reading data (if any) using the supplied getter.
     * The getter must return {@code null} for a non-existent key. If any of the
     * {@code recentFile0} (and so on) keys are absent, no error is raised, and
     * the corresponding slots in the file list are left empty.
     *
     * @param getter a function which takes a key and returns a value from a
     * backing store
     */
    public RecentFileList(UnaryOperator<String> getter) {
        fileSets = new LinkedList<>();
        for (int i = 0; i < MAX_LENGTH; i++) {
            FileSet fileSet =
                    FileSet.fromString(getter.apply("recentFile" + i));
            if (fileSet != null) fileSets.add(fileSet);
        }
    }

    /**
     * Stores the recent file list using the supplied remover and putter
     * functions.
     * 
     * @param remover a function which takes a key and removes the corresponding
     * key-value pair from a backing store. If the key is absent, it should
     * do nothing.
     * @param putter a function which stores a key-value pair, taking a key as
     * the first argument and a value as the second argument. It will not be
     * called with a null key or with a null value.
     */
    public void save(Consumer<String> remover,
            BiConsumer<String, String> putter) {
        for (int i = 0; i < MAX_LENGTH; i++) {
            remover.accept("recentFile" + i);
        }
        for (int i = 0; i < fileSets.size(); i++) {
            putter.accept("recentFile" + i, fileSets.get(i).toString());
        }
    }

    /**
     * Gets a list containing the names of the file-sets in the file list.
     * The names are expected to be displayed to the user. If a file-set
     * contains only one file, its leafname is used. If it contains more
     * than one file, the leafname of the first file is used, followed
     * by ‘etc.’. Each name is prefixed by an index number starting from 1.
     * This is intended to be used as a menu mnemonic.
     * 
     * @return the list of file-set names from this recent files list
     */
    public String[] getFilesetNames() {
        final String[] result = new String[fileSets.size()];
        int i = 0;
        for (FileSet fileSet: fileSets) {
            result[i] = Integer.toString(i+1) + " " + fileSet.getName();
            i++;
        }
        return result;
    }
    
    /**
     * Gets a list containing a long name for each fileset, intended for
     * use in the tooltips of the recent files menu. The long name is 
     * the full pathname of the file (or the first file if there are multiple
     * files in the set), with the leftmost part truncated if necessary to
     * stay within a reasonable length.
     * 
     * @return the list of long names for this recent files list
     */
    public List<String> getFilesetLongNames() {
        return fileSets.stream().map(x -> x.getLongName()).
                collect(Collectors.toList());
    }
    /**
     * Returns a specified file-set and moves it to the top of the list.
     * This method is intended to be called when a user selects a recently
     * used file. It returns the file at a specified index within the list,
     * and (since this file is now the most recently used) moves it to the
     * top of the list.
     * 
     * @param index the zero-based index of a file-set within the list
     * @return the requested file-set
     */
    public List<File> getFilesAndReorder(int index) {
        final FileSet result = fileSets.get(index);
        fileSets.remove(index);
        fileSets.add(0, result);
        return result.getFiles();
    }

    /**
     * Adds a new file-set to the top of the list. If the list is already
     * at its maximum length, the last item will be removed to make room.
     * 
     * @param files the files to be added (as a single file-set) to the list
     */
    public void add(List<File> files) {
        final FileSet fileSet = new FileSet(files);
        fileSets.remove(fileSet);
        fileSets.add(0, fileSet);
        if (fileSets.size() > MAX_LENGTH) {
            fileSets.removeLast();
        }
    }

    private static class FileSet {

        private final List<File> files;
        private final String name;

        public FileSet(List<File> files) {
            this.files = files;
            name = files.get(0).getName() + (files.size() > 1 ? " etc." : "");
        }

        public List<File> getFiles() {
            return files;
        }
        
        private int longestCommonPrefix(List<String> strings) {
            final List<Integer> lengths = new ArrayList<>(strings.size());
            for (String string: strings) {
                lengths.add(string.length());
            }
            final int minLength = Collections.min(lengths);
            int i=0;
            outer: while (i<minLength) {
                int firstValue = strings.get(0).codePointAt(i);
                for (String string: strings) {
                    if (string.codePointAt(i) != firstValue) {
                        break outer;
                    }
                }
                i++;
            }
            return i;
        }
        
        /**
         * Get the list of pathnames in a compressed format, where the
         * first string in the list is the longest common prefix of the
         * pathnames, and the other entries are the unique suffixes 
         * which may be appended to this prefix to reconstruct the 
         * full pathnames. This procedure is necessary because the length
         * of a Java Preferences value is limited.
         * 
         * @return a list of compressed pathnames as described above
         */
        private List<String> getCompressedPathNames() {
            final List<String> pathNames = getPathNames();
            final int prefixLength = longestCommonPrefix(pathNames);
            final List<String> cPathNames =
                    new ArrayList<>(pathNames.size() + 1);
            cPathNames.add(pathNames.get(0).substring(0, prefixLength));
            for (String path: pathNames) {
                cPathNames.add(path.substring(prefixLength));
            }
            return cPathNames;
        }
        
        private static List<String> decompressPathNames(
                List<String> compressed) {
            final List<String> result =
                    new ArrayList<>(compressed.size()-1);
            final String prefix = compressed.get(0);
            for (String path: compressed.subList(1, compressed.size())) {
                result.add(prefix + path);
            }
            return result;
        }

        private List<String> getPathNames() {
            List<String> pathNames = new ArrayList<>(files.size());
            for (File file: files) pathNames.add(file.getAbsolutePath());
            return pathNames;
        }

        /**
         * The string representation is designed to be stored and retrieved
         * through the java.util.prefs Preferences API, which seems to have
         * trouble with control characters. Thus the format is
         * <p>
         * numpaths pathlen1... pathlenN path1path2...pathN
         * <p>
         * which avoids the need to find a suitable separator string (i.e. one
         * which can be handled by the Preferences API on all platforms, and
         * will never appear in a pathname on any platform).
         * <p>
         * Note that, to avoid overrunning the size limit for a Preferences
         * value, path1 is the longest common prefix of the paths, and the other
         * paths are unique suffixes which, along with path1, make up the full
         * pathnames. getCompressedPathNames and decompressPathNames deal with
         * this encoding scheme.
         * <p>
         * @return a string representation of this class
         */
        @Override
        public String toString() {
            final List<String> pathNames = getCompressedPathNames();
            final StringBuilder builder = new StringBuilder();
            builder.append(pathNames.size());
            builder.append(" ");
            for (String path: pathNames) {
                builder.append(path.length());
                builder.append(" ");
            }
            for (String path: pathNames) {
                builder.append(path);
            }
            return builder.toString();
        }

        public static FileSet fromString(String string) {
            if (string == null) {
                return null;
            }
            final Scanner scanner = new Scanner(string);
            scanner.useLocale(Locale.ENGLISH);
            final int numPaths = scanner.nextInt();
            if (numPaths < 2) {
                return null;
            }
            final List<Integer> lengths = new ArrayList<>(numPaths);
            final List<String> compressed = new ArrayList<>(numPaths);
            for (int i=0; i<numPaths; i++) {
                lengths.add(scanner.nextInt());
            }
            final String allThePaths = scanner.findInLine(".*$");
            int pos=1;
            for (Integer length: lengths) {
                final String path = allThePaths.substring(pos, pos + length);
                compressed.add(path);
                pos += length;
            }
            final List<String> decompressed = decompressPathNames(compressed);
            final List<File> files = new ArrayList<>(compressed.size());
            for (String filename: decompressed) {
                files.add(new File(filename));
            }
            FileSet fileSet = null;
            if (files.size() > 0) {
                fileSet = new FileSet(files);
            }
            return fileSet;
        }

        public String getName() {
            return name;
        }
        
        public String getLongName() {
            final int MAX_LENGTH = 40;
            final String fullPath = files.get(0).getAbsolutePath() +
                    (files.size() > 1 ? ", …" : "");
            final int firstChar = fullPath.length() - MAX_LENGTH;
            final String path = (firstChar > 0) ?
                    "…"+fullPath.substring(firstChar) : fullPath;
            return path;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof FileSet)) {
                return false;
            }
            final FileSet f = (FileSet) other;
            return (files.equals(f.files));
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 43 * hash + (this.files != null ? this.files.hashCode() : 0);
            return hash;
        }
    }
}
