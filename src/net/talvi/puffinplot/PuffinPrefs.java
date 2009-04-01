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
import net.talvi.puffinplot.plots.Plot;

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
        correction = Correction.valueOf(prefs.get("correction", "NONE"));
    }
    
    public void save() {
        PuffinApp.getInstance().getRecentFiles().save(prefs);
        prefs.put("plotSizes", PuffinApp.getInstance().getMainWindow().getGraphDisplay().getPlotSizeString());
        prefs.put("correction", PuffinApp.getInstance().getCorrection().name());
    }
}
