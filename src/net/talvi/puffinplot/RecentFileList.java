/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2015 Pontus Lurcock.
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
import java.util.prefs.Preferences;
import java.util.stream.Collectors;


/**
 * <p>RecentFileList manages a list of file-sets. It is intended to be used
 * to manage a collection of recently used files for convenient re-opening
 * by a user. Note that each item in the list can comprise multiple files.
 * The length of the list is currently hard-wired to 8, though this would
 * be trivial to change if necessary.</p>
 * 
 * <p>RecentFileList loads and saves its data to a {@link java.util.prefs.Preferences}
 * object, using the keys of the form {@code recentFileX}, where X is a non-negative
 * integer less than the maximum number of file-sets.
 * </p>
 * 
 * @author pont
 */
public class RecentFileList {

    private static final int MAX_LENGTH = 8;
    private final LinkedList<FileSet> fileSets;

    /**
     * Creates a new file list, reading data (if any) from the supplied
     * {@link Preferences} object. If any {@code recentFile0} (and so on)
     * keys are absent, no error is raised, and the corresponding slots
     * in the file list are left empty.
     * 
     * @param prefs the preferences object from which the read file list
     * data
     */
    public RecentFileList(Preferences prefs) {
        fileSets = new LinkedList<>();
        for (int i = 0; i < MAX_LENGTH; i++) {
            FileSet fileSet =
                    FileSet.fromString(prefs.get("recentFile" + i, null));
            if (fileSet != null) fileSets.add(fileSet);
        }
    }

    /**
     * Saves the recent file list to the specified Preferences object.
     * 
     * @param prefs the Preferences to which to save the recent file list
     */
    public void save(Preferences prefs) {
        for (int i = 0; i < MAX_LENGTH; i++) {
            prefs.remove("recentFile" + i);
        }
        for (int i = 0; i < fileSets.size(); i++) {
            prefs.put("recentFile" + i, fileSets.get(i).toString());
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
        int i=0;
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
    public List<String> getFilesetLongNames()
    {
        return fileSets.stream().map(x -> x.getLongName()).collect(Collectors.toList());
    }
    /**
     * Returns a specified file-set and moves it to the top of the list.
     * This method is intended to be called when a user selects a recently
     * used file. It returns the file at a specified index within the list,
     * and (since this file is now the most recently used) moves it to the
     * top of the list.
     * 
     * @param index the index of a file-set within the list
     * @return the requested file-set
     */
    public List<File> getFilesAndReorder(int index) {
        FileSet result = fileSets.get(index);
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
        FileSet f = new FileSet(files);
        fileSets.remove(f);
        fileSets.add(0, f);
        if (fileSets.size() > MAX_LENGTH) fileSets.removeLast();
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
                    new ArrayList<>(pathNames.size()+1);
            cPathNames.add(pathNames.get(0).substring(0, prefixLength));
            for (String path: pathNames) {
                cPathNames.add(path.substring(prefixLength));
            }
            return cPathNames;
        }
        
        private static List<String> decompressPathNames(List<String> compressed) {
            final List<String> result =
                    new ArrayList<>(compressed.size()-1);
            String prefix = compressed.get(0);
            for (String path: compressed.subList(1, compressed.size())) {
                result.add(prefix+path);
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
         * through the Preferences API, which seems to have trouble with
         * control characters. Thus the format is
         *
         * numpaths pathlen1... pathlenN path1path2...pathN
         *
         * which avoids the need to find a suitable separator string
         * (i.e. one which can be handled by the Preferences API on all
         * platforms, and will never appear in a pathname on any platform).
         *
         * Note that, to avoid overrunning the size limit for a Preferences
         * value, path1 is the longest common prefix of the paths, and
         * the other paths are unique suffixes which, along with path1,
         * make up the full pathnames. getCompressedPathNames and
         * decompressPathNames deal with this encoding scheme.
         * 
         * @return a string representation of this class
         */
        @Override
        public String toString() {
            List<String> pathNames = getCompressedPathNames();
            StringBuilder sb = new StringBuilder();
            sb.append(pathNames.size());
            sb.append(" ");
            for (String path: pathNames) {
                sb.append(path.length());
                sb.append(" ");
            }
            for (String path: pathNames) {
                sb.append(path);
            }
            return sb.toString();
        }

        public static FileSet fromString(String s) {
            if (s==null) return null;
            final Scanner scanner = new Scanner(s);
            scanner.useLocale(Locale.ENGLISH);
            final int numPaths = scanner.nextInt();
            if (numPaths<2) return null;
            final List<Integer> lengths = new ArrayList<>(numPaths);
            final List<String> compressed = new ArrayList<>(numPaths);
            for (int i=0; i<numPaths; i++) lengths.add(scanner.nextInt());
            final String allThePaths = scanner.findInLine(".*$");
            int pos=1;
            for (Integer length: lengths) {
                String path = allThePaths.substring(pos, pos + length);
                compressed.add(path);
                pos += length;
            }
            List<String> decompressed = decompressPathNames(compressed);
            List<File> files = new ArrayList<>(compressed.size());
            for (String filename: decompressed) {
                files.add(new File(filename));
            }
            FileSet fileSet = null;
            if (files.size()>0) {
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
        public boolean equals(Object o) {
            if (!(o instanceof FileSet)) return false;
            final FileSet f = (FileSet) o;
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
