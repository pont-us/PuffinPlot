/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
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

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.SensorLengths;
import net.talvi.puffinplot.data.file.TwoGeeLoader;

/**
 * PuffinPrefs stores, loads, and saves PuffinPlot's user preferences.
 * It is essentially a convenience wrapper around a 
 * {@link java.util.prefs.Preferences} object.
 * 
 * @author pont
 */
public final class PuffinPrefs {

    private final PuffinApp app;
    private boolean axisScaleLocked;
    private boolean pcaAnchored;
    private final HashMap<String,Rectangle2D> plotSizes = new HashMap<>();
    private final Preferences prefs =
            Preferences.userNodeForPackage(PuffinPrefs.class);
    private SensorLengths sensorLengths;
    private TwoGeeLoader.Protocol twoGeeProtocol;

    /**
     * Instantiates a set of PuffinPlot preferences for the specified
     * {@link PuffinApp} instance. The preference settings are loaded
     * from the {@link java.util.prefs.Preferences} node associated with the package
     * containing PuffinPrefs.
     * 
     * @param app the PuffinPlot instance for which to create the preferences
     */
    public PuffinPrefs(PuffinApp app) {
        this.app = app;
        load();
    }

    /**
     * Returns the underlying {@link java.util.prefs.Preferences} object which holds the 
     * preferences.
     * 
     * @return the Preferences object containing the preferences
     */
    public Preferences getPrefs() {
        return prefs;
    }

    /**
     * Reports whether the axis scale is locked across all samples in
     * the current suite. This preference is currently ignored by PuffinPlot.
     * 
     * @return {@code true} if the axis scale is locked
     */
    public boolean isAxisScaleLocked() {
        return axisScaleLocked;
    }

    /**
     * Sets whether the axis scale is locked across all samples in the
     * current suite. This preference is currently ignored by PuffinPlot.
     * 
     * @param axisScaleLocked {@code true} to lock the axis scale,
     * {@code false} to unlock it
     */
    public void setAxisScaleLocked(boolean axisScaleLocked) {
        this.axisScaleLocked = axisScaleLocked;
    }

    /** Returns the effective sensor lengths for opening 2G data files.
     * @return the effective sensor lengths for opening 2G data files */
    public SensorLengths getSensorLengths() {
        return sensorLengths;
    }

    /** Sets the effective sensor lengths for opening 2G data files.
     * @param sensorLengths the effective sensor lengths for opening 
     * 2G data files */
    public void setSensorLengths(SensorLengths sensorLengths) {
        this.sensorLengths = sensorLengths;
    }

    /** Reports whether PCA fits should be anchored to the origin. 
     * @return {@code true} if PCA fits should be anchored to the origin.
     * @see net.talvi.puffinplot.data.PcaValues
     */
    public boolean isPcaAnchored() {
        return pcaAnchored;
    }

    /** Sets whether PCA fits should be anchored to the origin.
     * @param pcaThroughOrigin {@code true} if PCA fits should 
     * be anchored to the origin.
     * @see net.talvi.puffinplot.data.PcaValues
     */
    public void setPcaAnchored(boolean pcaThroughOrigin) {
        this.pcaAnchored = pcaThroughOrigin;
    }
    
    /**
     * Gives the configured size and position for a specified plot.
     * @param plotName the name of a plot
     * @return the bounding box of the specified plot within the main display area
     * @see net.talvi.puffinplot.plots.Plot
     */
    public Rectangle2D getPlotSize(String plotName) {
        if (!plotSizes.containsKey(plotName))
            plotSizes.put(plotName, new Rectangle2D.Double(100, 100, 100, 100));
        return plotSizes.get(plotName);
    }

    /**
     * Loads the preferences from the preferences backing store. The preferences
     * node used is the one associated with the package containing this class.
     * @see java.util.prefs.Preferences
     */
    public void load() {
        app.setRecentFiles(new RecentFileList(getPrefs()));
        setSensorLengths(SensorLengths.fromPrefs(prefs));
        twoGeeProtocol = TwoGeeLoader.Protocol.valueOf(prefs.get("measurementProtocol", "NORMAL"));
    }
    
    /**
     * Saves the preferences to the preferences backing store. The preferences
     * node used is the one associated with the package containing this class.
     * @see java.util.prefs.Preferences
     */
    public void save() {
        Preferences p = getPrefs();
        app.getRecentFiles().save(p);
        p.put("plotSizes", app.getMainWindow().getGraphDisplay().getPlotSizeString());
        p.put("correction", app.getCorrection().toString());
        getSensorLengths().save(prefs);
        p.put("measurementProtocol", twoGeeProtocol.name());
    }

    /**
     * Exports the current preferences to a specified file.
     * 
     * @param file the file to which to save the preferences
     */
    public void exportToFile(File file) {
        save();
        try {
            FileOutputStream outStream = new FileOutputStream(file);
            prefs.exportSubtree(outStream);
            outStream.close();
        } catch (IOException ex) {
            Logger.getLogger(PuffinPrefs.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BackingStoreException ex) {
            Logger.getLogger(PuffinPrefs.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Imports preferences from a specified file. The current preferences
     * will be overwritten.
     * 
     * @param file the file from which to import the preferences
     */
    public void importFromFile(File file) {
        try {
            FileInputStream inStream = new FileInputStream(file);
            Preferences.importPreferences(inStream);
            inStream.close();
        } catch (InvalidPreferencesFormatException ex) {
            Logger.getLogger(PuffinPrefs.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PuffinPrefs.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /** Returns the measurement protocol used when opening 2G data files.
     * @return the measurement protocol used when opening 2G data files */
    public TwoGeeLoader.Protocol get2gProtocol() {
        return twoGeeProtocol;
    }

    /** Sets the measurement protocol to use when opening 2G data files.
     * @param protocol the measurement protocol to use when opening 2G data files
     */
    public void set2gProtocol(TwoGeeLoader.Protocol protocol) {
        this.twoGeeProtocol = protocol;
    }
}
