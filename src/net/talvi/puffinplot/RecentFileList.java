package net.talvi.puffinplot;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.prefs.Preferences;

/**
 *
 * @author pont
 */
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

    public File[] getFilesAndReorder(int index) {
        FileSet result = fileSets.get(index);
        fileSets.remove(index);
        fileSets.add(0, result);
        return result.getFiles();
    }

    public void add(File[] files) throws IOException {
        fileSets.add(0, new FileSet(files));
        if (fileSets.size() > MAX_LENGTH) fileSets.removeLast();
    }

    public static class FileSet {

        private final String[] pathNames;
        private final String name;

        public FileSet(File[] files) throws IOException {
            pathNames = new String[files.length];
            name = files[0].getName() + (files.length > 1 ? " etc." : "");
            for (int i=0; i<files.length; i++)
                pathNames[i] = files[i].getCanonicalPath();
        }

        private FileSet(String[] paths) {
            name = new File(paths[0]).getName() +
                    (paths.length > 1 ? " etc." : "");
            pathNames = paths;
        }

        public File[] getFiles() {
            File[] files = new File[pathNames.length];
            for (int i=0; i<pathNames.length; i++)
                    files[i] = new File(pathNames[i]);
            return files;
        }

        /**
         * The string representation is designed to be stored and retrieved
         * through the Preferences API, which seems to have trouble with
         * control characters. Thus the format is now
         *
         * numpaths pathlen1... pathlenN path1path2...pathN
         *
         * which avoids the need to find a suitable separator string
         * (i.e. one which can be handled by the Preferences API on all
         * platforms, and will never appear in a pathname on any platform).
         *
         * @return a string representation of this class
         */
        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(pathNames.length);
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
            int[] lengths = new int[numPaths];
            String[] paths = new String[numPaths];
            for (int i=0; i<numPaths; i++)
                lengths[i] = scanner.nextInt();
            String allThePaths = scanner.findInLine(".*$");
            for (int i=0, pos=1; i<numPaths; i++) {
                paths[i] = allThePaths.substring(pos, pos+lengths[i]);
                pos += lengths[i];
            }
            return new FileSet(paths);
        }

        public String getName() {
            return name;
        }
    }

}
