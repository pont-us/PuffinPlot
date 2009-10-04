package net.talvi.puffinplot;

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.prefs.Preferences;

public class PuffinPrefs {

    private boolean axisScaleLocked;
    private boolean pcaAnchored;
    private HashMap<String,Rectangle2D> plotSizes =
            new HashMap<String, Rectangle2D>();
    private Preferences prefs = Preferences.userNodeForPackage(PuffinPrefs.class);
    private boolean useOldSquidOrientations;
    
    public PuffinPrefs() {
        load();
    }

    public Preferences getPrefs() {
        return prefs;
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
    
    public Rectangle2D getPlotSize(String plotName) {
        if (!plotSizes.containsKey(plotName))
            plotSizes.put(plotName, new Rectangle2D.Double(100, 100, 100, 100));
        return plotSizes.get(plotName);
    }

    public void load() {
        PuffinApp.getInstance().setRecentFiles(new RecentFileList(getPrefs()));
        setUseOldSquidOrientations(Boolean.parseBoolean(getPrefs().get("useOldSquidOrientations", "false")));
    }
    
    public void save() {
        PuffinApp.getInstance().getRecentFiles().save(getPrefs());
        getPrefs().put("plotSizes", PuffinApp.getInstance().getMainWindow().getGraphDisplay().getPlotSizeString());
        getPrefs().put("correction", PuffinApp.getInstance().getCorrection().toString());
        getPrefs().put("useOldSquidOrientations", Boolean.toString(isUseOldSquidOrientations()));
    }

    public boolean isUseOldSquidOrientations() {
        return useOldSquidOrientations;
    }

    public void setUseOldSquidOrientations(boolean useOldSquidOrientations) {
        this.useOldSquidOrientations = useOldSquidOrientations;
    }
}
