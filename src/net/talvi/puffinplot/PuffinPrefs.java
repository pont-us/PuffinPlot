package net.talvi.puffinplot;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.Correction;

public class PuffinPrefs {

    private static final int MAX_RECENT_FILES = 8;
    
    private boolean axisScaleLocked;
    private boolean pcaAnchored;
    private RecentFile[] recentFiles = new RecentFile[MAX_RECENT_FILES];
    private int nextRecentFile = 0;
    private HashMap<String,Rectangle2D> plotSizes =
            new HashMap<String, Rectangle2D>();
    
    static Preferences prefs = Preferences.userNodeForPackage(PuffinPrefs.class);
    private Correction correction;
    
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
    
    public Correction getCorrection() {
        return correction;
    }
    
    public Rectangle2D getPlotSize(String plotName) {
        if (!plotSizes.containsKey(plotName))
            plotSizes.put(plotName, new Rectangle2D.Double(100, 100, 100, 100));
        return plotSizes.get(plotName);
    }
    
    public void load() {
        for (int i=0; i<MAX_RECENT_FILES; i++)
            recentFiles[i] = RecentFile.fromString(prefs.get("recentFile"+i, null));
        nextRecentFile = prefs.getInt("nextRecentFile", 0);
        final String plotSizeString = prefs.get("plotSizes",
                "zplot 407 32 610 405 pcatable 518 708 195 67 " +
                "sampletable 24 13 215 39 fishertable 837 60 155 60 " +
                "datatable 43 324 349 441 demag 50 69 323 213 " +
                "equarea 685 439 338 337");
        final Scanner scanner = new Scanner(plotSizeString);
        while (scanner.hasNext()) {
            String plotName = scanner.next();
            Rectangle2D geometry = 
                    new Rectangle2D.Double(scanner.nextDouble(),
                    scanner.nextDouble(), scanner.nextDouble(),
                    scanner.nextDouble());
            plotSizes.put(plotName, geometry);
        }
        correction = Correction.valueOf(prefs.get("correction", "NONE"));
    }
    
    public void save() {
        for (int i = 0; i < MAX_RECENT_FILES; i++)
            if (recentFiles[i] != null)
                prefs.put("recentFile" + i, recentFiles[i].toString());
        prefs.putInt("nextRecentFile", nextRecentFile);
        StringBuffer sb = new StringBuffer();
        for (String plotName: plotSizes.keySet()) {
            Rectangle2D r = PuffinApp.getInstance().getMainWindow().
                    getGraphDisplay().getPlotSize(plotName);
            sb.append(String.format("%s %f %f %f %f ",
                    plotName, r.getMinX(), r.getMinY(),
                    r.getWidth(), r.getHeight()));
        }
        prefs.put("plotSizes", sb.toString());
        prefs.put("correction", PuffinApp.getInstance().getCorrection().name());
    }
}
