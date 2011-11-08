package net.talvi.puffinplot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.prefs.Preferences;

public class RecentFileList {

    private static final int MAX_LENGTH = 8;
    private LinkedList<FileSet> fileSets;

    public RecentFileList(int size) {
        fileSets = new LinkedList<FileSet>();
    }

    public RecentFileList(Preferences prefs) {
        fileSets = new LinkedList<FileSet>();
        for (int i = 0; i < MAX_LENGTH; i++) {
            FileSet fileSet =
                    FileSet.fromString(prefs.get("recentFile" + i, null));
            if (fileSet != null) fileSets.add(fileSet);
        }
    }

    public void save(Preferences prefs) {
        for (int i = 0; i < fileSets.size(); i++) {
            prefs.put("recentFile" + i, fileSets.get(i).toString());
        }
    }

    public String[] getFilesetNames() {
        String[] result = new String[fileSets.size()];
        int i=0;
        for (FileSet fileSet: fileSets) {
            result[i++] = fileSet.getName();
        }
        return result;
    }

    public List<File> getFilesAndReorder(int index) {
        FileSet result = fileSets.get(index);
        fileSets.remove(index);
        fileSets.add(0, result);
        return result.getFiles();
    }

    public void add(List<File> files) {
        FileSet f = new FileSet(files);
        fileSets.remove(f);
        fileSets.add(0, f);
        if (fileSets.size() > MAX_LENGTH) fileSets.removeLast();
    }

    public static class FileSet {

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
            final List<Integer> lengths = new ArrayList<Integer>(strings.size());
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
                    new ArrayList<String>(pathNames.size()+1);
            cPathNames.add(pathNames.get(0).substring(0, prefixLength));
            for (String path: pathNames) {
                cPathNames.add(path.substring(prefixLength));
            }
            return cPathNames;
        }
        
        private static List<String> decompressPathNames(List<String> compressed) {
            final List<String> result =
                    new ArrayList<String>(compressed.size()-1);
            String prefix = compressed.get(0);
            for (String path: compressed.subList(1, compressed.size())) {
                result.add(prefix+path);
            }
            return result;
        }

        private List<String> getPathNames() {
            List<String> pathNames = new ArrayList<String>(files.size());
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
            Scanner scanner = new Scanner(s);
            final int numPaths = scanner.nextInt();
            List<Integer> lengths = new ArrayList<Integer>(numPaths);
            List<String> compressed = new ArrayList<String>(numPaths);
            for (int i=0; i<numPaths; i++) lengths.add(scanner.nextInt());
            String allThePaths = scanner.findInLine(".*$");
            int pos=1;
            for (Integer length: lengths) {
                String path = allThePaths.substring(pos, pos + length);
                compressed.add(path);
                pos += length;
            }
            List<String> decompressed = decompressPathNames(compressed);
            List<File> files = new ArrayList<File>(compressed.size());
            for (String filename: decompressed) {
                files.add(new File(filename));
            }
            return new FileSet(files);
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FileSet)) return false;
            FileSet f = (FileSet) o;
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
