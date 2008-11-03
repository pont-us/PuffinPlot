package net.talvi.puffinplot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.prefs.Preferences;

public class PuffinPrefs {

    private static final int MAX_RECENT_FILES = 8;
    
    private boolean axisScaleLocked;
    private boolean pcaAnchored;
    private RecentFile[] recentFiles = new RecentFile[MAX_RECENT_FILES];
    private int nextRecentFile = 0;
    
    static Preferences prefs = Preferences.userNodeForPackage(PuffinPrefs.class);
    
    public PuffinPrefs() {
        load();
    }
    
    public static class RecentFile {

        private final String[] pathNames;
        private final String name;
        
        public RecentFile(File[] files) throws IOException {
            pathNames = new String[files.length];
            name = files[0].getName() + (files.length > 1 ? " etc." : "");
            for (int i=0; i<files.length; i++)
                pathNames[i] = files[i].getCanonicalPath();
        }
        
        private RecentFile(String[] paths) {
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
        
        public static RecentFile fromString(String s) {
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
            return new RecentFile(paths);
        }

        public String getName() {
            return name;
        }
    }
    
    public void addRecentFile(File[] files) throws IOException {
        recentFiles[nextRecentFile++ % MAX_RECENT_FILES] = new RecentFile(files);
    }
    
    public List<RecentFile> getRecentFiles() {
        List<RecentFile> files = new ArrayList<RecentFile>(MAX_RECENT_FILES);
        for (int i=0; i<MAX_RECENT_FILES; i++) {
            RecentFile candidate = recentFiles[(i+nextRecentFile)%MAX_RECENT_FILES];
            if (candidate != null) files.add(candidate);
        }
        return files;
    }
    
    public boolean isAxisScaleLocked() {
        return axisScaleLocked;
    }

    public void setAxisScaleLocked(boolean axisScaleLocked) {
        this.axisScaleLocked = axisScaleLocked;
    }

    public boolean isPcaAnchored() {
        return pcaAnchored;
    }

    public void setPcaAnchored(boolean pcaThroughOrigin) {
        this.pcaAnchored = pcaThroughOrigin;
    }
    
    public void load() {
        for (int i=0; i<MAX_RECENT_FILES; i++)
            recentFiles[i] = RecentFile.fromString(prefs.get("recentFile"+i, null));
        nextRecentFile = prefs.getInt("nextRecentFile", 0);
    }
    
    public void save() {
        for (int i = 0; i < MAX_RECENT_FILES; i++)
            if (recentFiles[i] != null)
                prefs.put("recentFile" + i, recentFiles[i].toString());
        prefs.putInt("nextRecentFile", nextRecentFile);
    }
}
