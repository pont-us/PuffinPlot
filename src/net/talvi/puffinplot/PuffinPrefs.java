package net.talvi.puffinplot;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.SensorLengths;
import net.talvi.puffinplot.data.Vec3;

public class PuffinPrefs {

    private boolean axisScaleLocked;
    private boolean pcaAnchored;
    private HashMap<String,Rectangle2D> plotSizes =
            new HashMap<String, Rectangle2D>();
    private Preferences prefs = Preferences.userNodeForPackage(PuffinPrefs.class);
    private SensorLengths sensorLengths;
    private boolean twoPosProtocol;

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
        twoPosProtocol = prefs.getBoolean("twoPosProtocol", false);
    }
    
    public void save() {
        Preferences p = getPrefs();
        PuffinApp app = PuffinApp.getInstance();
        app.getRecentFiles().save(p);
        p.put("plotSizes", app.getMainWindow().getGraphDisplay().getPlotSizeString());
        p.put("correction", app.getCorrection().toString());
        getSensorLengths().save(prefs);
        p.putBoolean("twoPosProtocol", twoPosProtocol);
    }

    public boolean getTwoPosProtocol() {
        return twoPosProtocol;
    }

    public void setTwoPosProtocol(boolean twoPosProtocol) {
        this.twoPosProtocol = twoPosProtocol;
    }
}
