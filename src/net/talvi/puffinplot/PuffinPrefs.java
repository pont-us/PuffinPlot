package net.talvi.puffinplot;

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.SensorLengths;
import net.talvi.puffinplot.data.file.TwoGeeLoader;

public final class PuffinPrefs {

    private boolean axisScaleLocked;
    private boolean pcaAnchored;
    private HashMap<String,Rectangle2D> plotSizes =
            new HashMap<String, Rectangle2D>();
    private Preferences prefs = Preferences.userNodeForPackage(PuffinPrefs.class);
    private SensorLengths sensorLengths;
    private TwoGeeLoader.Protocol twoGeeProtocol;

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

    public SensorLengths getSensorLengths() {
        return sensorLengths;
    }

    public void setSensorLengths(SensorLengths sensorLengths) {
        this.sensorLengths = sensorLengths;
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
        setSensorLengths(SensorLengths.fromPrefs(prefs));
        twoGeeProtocol = TwoGeeLoader.Protocol.valueOf(prefs.get("measurementProtocol", "NORMAL"));
    }
    
    public void save() {
        Preferences p = getPrefs();
        PuffinApp app = PuffinApp.getInstance();
        app.getRecentFiles().save(p);
        p.put("plotSizes", app.getMainWindow().getGraphDisplay().getPlotSizeString());
        p.put("correction", app.getCorrection().toString());
        getSensorLengths().save(prefs);
        p.put("measurementProtocol", twoGeeProtocol.name());
    }

    public TwoGeeLoader.Protocol get2gProtocol() {
        return twoGeeProtocol;
    }

    public void set2gProtocol(TwoGeeLoader.Protocol p) {
        this.twoGeeProtocol = p;
    }

}
