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

    private boolean axisScaleLocked;
    private boolean pcaAnchored;
    private HashMap<String,Rectangle2D> plotSizes =
            new HashMap<String, Rectangle2D>();
    static Preferences prefs = Preferences.userNodeForPackage(PuffinPrefs.class);
    private Correction correction;
    
    public PuffinPrefs() {
        load();
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
        PuffinApp.getInstance().setRecentFiles(new RecentFileList(prefs));
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
        PuffinApp.getInstance().getRecentFiles().save(prefs);
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
