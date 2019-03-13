/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.MemoryHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.Optional;
import net.talvi.puffinplot.data.AmsCalculationType;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.CsvWriter;
import net.talvi.puffinplot.data.FileType;
import net.talvi.puffinplot.data.MeasurementType;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Site;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.SuiteCalcs;
import net.talvi.puffinplot.data.SuiteRpiEstimate;
import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.file.FileFormat;
import net.talvi.puffinplot.plots.SampleClickListener;
import net.talvi.puffinplot.plots.SampleParamsTable;
import net.talvi.puffinplot.plots.SiteParamsTable;
import net.talvi.puffinplot.plots.VgpTable;
import net.talvi.puffinplot.window.AboutBox;
import net.talvi.puffinplot.window.CustomFieldEditor;
import net.talvi.puffinplot.window.FiletypeDialog;
import net.talvi.puffinplot.window.IapdImportDialog;
import net.talvi.puffinplot.window.ImportAmsDialog;
import net.talvi.puffinplot.window.MainGraphDisplay;
import net.talvi.puffinplot.window.MainWindow;
import net.talvi.puffinplot.plots.PlotParams;
import net.talvi.puffinplot.window.PrefsWindow;
import net.talvi.puffinplot.window.ProgressDialog;
import net.talvi.puffinplot.window.SiteMeanWindow;
import net.talvi.puffinplot.window.SuiteEqAreaWindow;
import net.talvi.puffinplot.window.TableWindow;
import net.talvi.puffinplot.window.TabularImportWindow;
import org.freehep.graphicsbase.util.UserProperties;

import static net.talvi.puffinplot.Util.runningOnOsX;
import net.talvi.puffinplot.window.RpiDialog;

/**
 * Instantiating {@code PuffinApp} starts the PuffinPlot desktop application. It
 * starts with no visible windows; a call to {@link PuffinApp#show()} will open
 * the main window. {@code PuffinApp}'s main responsibility is connecting the
 * GUI classes to the classes which handle the actual data. Most of the actions
 * defined in {@link PuffinActions} act as thin wrappers around one or a few
 * calls to {@code PuffinApp}. The most significant data classes used in
 * {@code PuffinApp} are {@link Suite}, {@link Site}, and {@link Sample}.
 */
public class PuffinApp {

    private static final Logger LOGGER =
            Logger.getLogger("net.talvi.puffinplot");
    private static final ByteArrayOutputStream LOG_STREAM =
            new ByteArrayOutputStream();
    private static final MemoryHandler LOG_MEMORY_HANDLER;

    private final PuffinActions actions;
    private final List<Suite> suites = new ArrayList<>();
    private Suite currentSuite;
    private PageFormat currentPageFormat;
    private final MainGraphDisplay mainGraphDisplay;
    private final PlotParams plotParams;
    private final MainWindow mainWindow;
    private final PuffinPrefs prefs;
    private final TableWindow tableWindow;
    private final SuiteEqAreaWindow suiteEqAreaWindow;
    private final SiteMeanWindow siteEqAreaWindow;
    private PrefsWindow prefsWindow;
    private final AboutBox aboutBox;
    private RecentFileList recentFiles;
    private Correction correction;
    private final IdToFileMap lastUsedFileOpenDirs;
    private java.util.BitSet stepSelectionClipboard = new java.util.BitSet(0);
    private Properties buildProperties;
    private SuiteCalcs multiSuiteCalcs;
    private ScriptEngine pythonEngine = null;
    private final Version version;
    
    /*
     * I don't use IdToFileMap for lastUsedSaveDirectories, because I think it's
     * better that the save directories *don't* persist between restarts of the
     * program. Inkscape has persistent save directories and I've found it to be
     * very counterintuitive.
     */
    private final java.util.Map<String,String> lastUsedSaveDirectories =
            new java.util.HashMap<>();

    static {
        final Handler logStringHandler =
            new StreamHandler(LOG_STREAM, new SimpleFormatter());
        logStringHandler.setLevel(Level.ALL);
        LOGGER.addHandler(LOG_MEMORY_HANDLER =
                new MemoryHandler(logStringHandler, 100, Level.OFF));
        LOG_MEMORY_HANDLER.setLevel(Level.INFO);
    }

    private class PuffinAppSampleClickListener implements SampleClickListener {

        public PuffinAppSampleClickListener() { }

        @Override
        public void sampleClicked(Sample sample) {
            final Suite suite = getCurrentSuite();
            getCurrentSuite().setCurrentSampleIndex(suite.getIndexBySample(sample));
            getMainWindow().getSampleChooser().updateValueFromSuite();
            updateDisplay();
        }
    }
    
    /**
     * Instantiates a new PuffinPlot application object.
     */
    private PuffinApp() {

        LOGGER.info("Instantiating PuffinApp.");
        // com.apple.macos.useScreenMenuBar deprecated since 1.4, I think
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                "PuffinPlot");
        loadBuildProperties();
        version = Version.fromGitProperties(this::getBuildProperty);
        prefs = new PuffinPrefs(this);
        lastUsedFileOpenDirs = new IdToFileMap(
                key -> prefs.getPrefs().get(key, ""),
                prefs.getPrefs()::put);
        actions = new PuffinActions(this);
        plotParams = new PlotParams() {
            @Override
            public Sample getSample() {
                return mainGraphDisplay != null &&
                        mainGraphDisplay.isPrintingInProgress()
                        ? mainGraphDisplay.getCurrentlyPrintingSample()
                        : PuffinApp.this.getCurrentSample();
            }
            @Override
            public List<Sample> getSelectedSamples() {
                return PuffinApp.this.getSelectedSamples();
            }
            @Override
            public Correction getCorrection() {
                return PuffinApp.this.getCorrection();
            }
            @Override
            public MeasurementAxis getVprojXaxis() {
                return getMainWindow().getControlPanel().getVprojXaxis();
            }

            @Override
            public MeasurementAxis getHprojXaxis() {
                return getMainWindow().getControlPanel().getHprojXaxis();
            }

            @Override
            public MeasurementAxis getHprojYaxis() {
                return getMainWindow().getControlPanel().getHprojYaxis();
            }

            @Override
            public List<Sample> getAllSamplesInSelectedSites() {
                return PuffinApp.this.getAllSamplesInSelectedSites();
            }
            
            @Override
            public float getUnitSize() {
                return 0.12f;
            }

            @Override
            public String getSetting(String key, String def) {
                return PuffinApp.this.getPrefs().getPrefs().get(key, def);
            }

            @Override
            public boolean getSettingBoolean(String key, boolean def) {
                return PuffinApp.this.getPrefs().getPrefs().
                        getBoolean(key, def);
            }
            
        };

        tableWindow = new TableWindow(plotParams);
        suiteEqAreaWindow = new SuiteEqAreaWindow(this);
        siteEqAreaWindow = new SiteMeanWindow(plotParams);
        /*
         * NB: main window must be instantiated last, as the Window menu
         * references the other windows.
         */
        mainWindow = MainWindow.getInstance(this);
        mainGraphDisplay = mainWindow.getGraphDisplay();
        setApplicationIcon();
        Correction corr = Correction.fromString(
                prefs.getPrefs().get("correction", "false false NONE false"));
        setCorrection(corr);
        mainWindow.getControlPanel().setCorrection(corr);
        /*
         * The PrefsWindow needs the graph list from MainGraphDisplay from
         * MainWindow, and the correction.
         */
        prefsWindow = new PrefsWindow(this);
        if (runningOnOsX()) {
            createAppleEventHandler();
        }
        currentPageFormat = PrinterJob.getPrinterJob().defaultPage();
        currentPageFormat.setOrientation(PageFormat.LANDSCAPE);
        aboutBox = new AboutBox(this);

        final SampleClickListener scListener =
                new PuffinAppSampleClickListener();
        mainGraphDisplay.getPlotByClass(SampleParamsTable.class).
                addSampleClickListener(scListener);
        mainGraphDisplay.getPlotByClass(SiteParamsTable.class).
                addSampleClickListener(scListener);
        mainGraphDisplay.getPlotByClass(VgpTable.class).
                addSampleClickListener(scListener);
        mainWindow.getMainMenuBar().updateRecentFiles();
        LOGGER.info("PuffinApp instantiation complete.");
    }
    
    /**
     * Create and return a new PuffinApp object. The GUI elements are created,
     * but not shown.
     *
     * @return a new PuffinApp object
     */
    public static PuffinApp create() {
        return new PuffinApp();
    }
    
    /**
     * Shows the main window of this PuffinApp. If the main window is already
     * visible, this method has no effect.
     */
    public void show() {
        mainWindow.setVisible(true);
    }
    
    /**
     * Pushes any buffered output to the in-memory log handler, flushes the log
     * handler and log stream, and returns the log stream. This method is mainly
     * intended to let {@link ExceptionHandler} write logging information to a
     * crash report file.
     *
     * @return the output stream of PuffinApp's logger
     * @throws IOException if an I/O error occurred while flushing the stream
     */
    static OutputStream flushLogAndGetStream() throws IOException {
        LOG_MEMORY_HANDLER.push();
        LOG_MEMORY_HANDLER.flush();
        LOG_STREAM.flush();
        return LOG_STREAM;
    }

    /**
     * @return the plot parameters controlled by the GUI of this PuffinApp
     * instance
     */
    public PlotParams getPlotParams() {
        return plotParams;
    }

    /**
     * Recalculates all sample and site calculations in all currently open
     * suites. Intended to be called when the correction (none/sample/formation)
     * has changed.
     */
    public void redoCalculationsForAllSuites() {
        for (Suite suite: getSuites()) {
            suite.doSampleCalculations(getCorrection());
            suite.doSiteCalculations(getCorrection(),
                    getGreatCirclesValidityCondition());
        }
    }

    private void loadBuildProperties() {
        InputStream propStream = null;
        try {
            propStream =
                    PuffinApp.class.getResourceAsStream("build.properties");
            buildProperties = new Properties();
            buildProperties.load(propStream);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Failed to get build date", ex);
            /*
             * The only effect of this on the user is a lack of build date in
             * the about box, so we can get away with just logging it.
             */
        } finally {
            if (propStream != null) {
                try {
                    propStream.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "Error closing property stream",
                            ex);
                }                
            }
        }
        LOGGER.log(Level.INFO, "Build date: {0}",
                getBuildProperty("build.date"));
    }
    
    /**
     * Reads values from the {@code build.properties} file. This is a properties
     * file written into the PuffinPlot jar at build time, and currently
     * contains the keys {@code build.date} and {@code build.year}.
     *
     * @param key the property key to read
     * @return the value of the key
     */
    public String getBuildProperty(String key) {
        if (buildProperties == null) {
            return "unknown (no file)";
        } else {
            return buildProperties.getProperty(key, "unknown (no key)");
        }
    }
    
    /**
     * For all selected sites, calculates the site mean direction by 
     * best-fit intersection of great circles fitted to that
     * site's samples.
     */
    public void calculateGreatCirclesDirections() {
        for (Site site: getSelectedSites()) {
            site.calculateGreatCirclesDirection(getCorrection(),
                    getGreatCirclesValidityCondition());
        }
        updateDisplay();
    }

    /**
     * Redo existing sample and site calculations affected by specified samples.
     * <p>
     * Any existing PCA calculations or great-circle fits will be done for the
     * specified samples, and any existing great-circle or Fisherian
     * calculations will be done for any site containing one of the specified
     * samples. Site calculations will not be done for sites that don't have
     * them already.
     * <p>
     * This method does not update the display.
     * 
     * @param samples samples which have changed
     */
    public void recalculateSamplesAndSites(Collection<Sample> samples) {
        final Set<Site> affectedSites = samples.stream().
                map(sample -> sample.getSite()).filter(site -> site != null).
                collect(Collectors.toSet());

        samples.forEach(s -> s.doPca(getCorrection()));
        samples.forEach(s -> s.fitGreatCircle(getCorrection()));
        for (Site site: affectedSites) {
            if (site.getFisherValues() != null) {
                site.calculateFisherStats(getCorrection());
            }
            if (site.getGreatCircles() != null) {
                /*
                 * PCAs are also used in GC calculations, so this needs to be
                 * recalculated even if only the PCA has changed.
                 */
                site.calculateGreatCirclesDirection(getCorrection(),
                        getGreatCirclesValidityCondition());
            }
        }
    }
    
    /**
     * For all selected samples, fit a great circle to the selected treatment
     * steps.
     */
    public void fitGreatCirclesToSelection() {
        for (Sample sample: getSelectedSamples()) {
            sample.useSelectionForCircleFit();
            sample.fitGreatCircle(getCorrection());
        }
        recalculateSamplesAndSites(getSelectedSamples());
        updateDisplay();
    }
    
    /**
     * For all selected samples, determine a best-fit line to the selected
     * treatment steps by principal component analysis.
     * <p>
     * Any affected Fisherian site means or great-circle directions will be
     * recalculated automatically. These directions will not be calculated for
     * any sites that don't have them already.
     */
    public void doPcaOnSelection() {
        for (Sample sample: getSelectedSamples()) {
            if (sample.getSelectedTreatmentSteps().size() > 1) {
                sample.useSelectionForPca();
                sample.doPca(getCorrection());
            }
        }
        recalculateSamplesAndSites(getSelectedSamples());
        updateDisplay();
    }
    
    /**
     * Returns the preferences for this PuffinApp.
     *
     * @return the preferences for this PuffinApp
     */
    public PuffinPrefs getPrefs() {
        return prefs;
    }

    /**
     * Returns this PuffinApp's main window
     *
     * @return this PuffinApp's main window
     */
    public MainWindow getMainWindow() {
        return mainWindow;
    }
    
    /**
     * Returns the correction currently being applied to the data displayed
     * by this PuffinApp.
     * 
     * @return the correction currently being applied to the displayed data
     */
    public Correction getCorrection() {
        return correction;
    }
    
    /**
     * Sets the correction to apply to the displayed data. This method is
     * final because it is used in the constructor.
     * 
     * @param correction the correction to apply to the displayed data
     */
    final public void setCorrection(Correction correction) {
        this.correction = correction;
    }
    
    /**
     * Updates the main window and table window to reflect any changes in
     * the currently displayed data.
     */
    public void updateDisplay() {
        getMainWindow().sampleChanged();
        getTableWindow().dataChanged();
        getSiteEqAreaWindow().repaint(100);
        getSuiteEqAreaWindow().repaint(100);
    }

    private boolean canSuiteBeClosed(Suite suite) {
        if (suite.isSaved()) {
            return true;
        } else {
            final Object[] buttons = {"Save changes",
                "Discard changes", "Don't close suite"};
            final int choice = JOptionPane.showOptionDialog(mainWindow,
                    "The suite \"" + suite.getName() + "\" "
                            + "has been changed since it was last saved.\n"
                            + "The changes will be lost if you close it.\n"
                            + "Would you like to save the changes before "
                            + "closing the suite?",
                    "Unsaved data",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,     // no custom icon
                    buttons,
                    buttons[2]); // default option
            if (choice==0) { // "Save changes" chosen
                save(suite);
            }
            return choice != 2; // can close unless "Don't close" chosen
        }
    }
    
    /**
     * Closes the suite whose data is currently being displayed.
     */
    public void closeCurrentSuite() {
        if (suites == null || suites.isEmpty()) {
            return;
        }
        int index = suites.indexOf(currentSuite);
        if (!canSuiteBeClosed(currentSuite)) {
            return;
        }
        suites.remove(currentSuite);
        /*
         * Set new current suite to previous (if any), else next (if any), or
         * none.
         */
        if (index > 0) {
            index--;
        }
        if (suites.size() > 0) {
            currentSuite = suites.get(index);
        } else {
            currentSuite = null;
        }
        getMainWindow().suitesChanged();
    }

    /**
     * Creates a new, empty suite and adds it to the suite list. Mainly intended
     * for use by scripts, so not currently accessible via the GUI.
     */
    public void createNewSuite() {
        final Suite suite = new Suite("PuffinPlot " +
                version.getVersionString());
        suite.addSavedListener((boolean newState) -> {
            updateMainWindowTitle();
        });
        suites.add(suite);
        currentSuite = suites.get(suites.size()-1);
        mainWindow.getMainMenuBar().updateRecentFiles();
        mainWindow.updateSampleDataPanel();
        updateDisplay();
    }
    
    /**
     * Reads data into the current suite, or a new suite, from the specified
     * files.
     *
     * @param files the files from which to read data
     * @param createNewSuite whether to create a new suite; if this parameter is
     * {@code true} or if there is no current suite, a new suite will be created
     * for the data.
     *
     */
    public void openFiles(List<File> files, boolean createNewSuite) {
        if (files.isEmpty()) {
            return;
        }
        
        /*
         * If this fileset is already in the recent-files list, it will be
         * bumped up to the top; otherwise it will be added to the top and the
         * last member removed.
         */
        recentFiles.add(files);
        
        final boolean reallyCreateNewSuite = createNewSuite || getCurrentSuite()==null;

        try {
            final FileType guessedType = FileType.guess(files.get(0));
            FileType fileType;
            if (guessedType == FileType.PUFFINPLOT_NEW ||
                    guessedType == FileType.PUFFINPLOT_OLD) {
                fileType = guessedType;
            } else {
                final FiletypeDialog ft = new FiletypeDialog(getMainWindow());
                ft.setVisible(true);
                fileType = ft.getSelectedFileType();
            }
            
            if (fileType == null) {
                return;
            }

            FileFormat format = null;
            final java.util.Map<Object,Object> importOptions =
                    new java.util.HashMap<>();
            switch (fileType) {
                case CUSTOM_TABULAR:
                    final TabularImportWindow tabularDialog =
                            new TabularImportWindow(getMainWindow(),
                                    getPrefs().getPrefs());
                    tabularDialog.setVisible(true);
                    format = tabularDialog.getFormat();
                    if (format == null) {
                        return;
                    }
                    format.writeToPrefs(getPrefs().getPrefs());
                    break;
                case IAPD:
                    final IapdImportDialog iapdDialog =
                            new IapdImportDialog(getMainWindow());
                    if (!iapdDialog.wasOkPressed()) {
                        return;
                    }
                    importOptions.put(TreatmentType.class,
                            iapdDialog.getTreatmentType());
                    importOptions.put(MeasurementType.class,
                            iapdDialog.getMeasurementType());
                    break;
            }
            
            final Suite suite;
            if (reallyCreateNewSuite) {
                suite = new Suite("PuffinPlot " + version.getVersionString());
                suite.addSavedListener(newState -> updateMainWindowTitle());
            } else {
                suite = getCurrentSuite();
            }
            suite.readFiles(files, prefs.getSensorLengths(),
                    prefs.get2gProtocol(),
                    !"X/Y/Z".equals(prefs.getPrefs().
                            get("readTwoGeeMagFrom", "X/Y/Z")),
                    fileType, format, importOptions);
            suite.doAllCalculations(getCorrection(),
                    getGreatCirclesValidityCondition());
            final List<String> warnings = suite.getLoadWarnings();
            if (warnings.size() > 0) {
                final StringBuilder sb =
                        new StringBuilder(warnings.size() == 1 ? "" :
                                "The following problems occurred:\n");
                int nWarnings = 0;
                final int MAX_WARNINGS = 10;
                for (String warning: warnings) {
                    if (nWarnings == MAX_WARNINGS) {
                        final int remainder = warnings.size() - MAX_WARNINGS;
                        if (remainder == 1) {
                            /*
                             * No point adding a "1 more warning omitted" line
                             * when we could just use that line to show the
                             * final warning!
                             */
                            sb.append(warning);
                        } else {
                            sb.append("(").append(remainder).
                                    append(" more errors not shown.)");
                        }
                        break;
                    }
                    sb.append(warning);
                    sb.append("\n");
                    nWarnings++;
                }
                errorDialog(warnings.size() == 1 ?
                        "Error during file opening" :
                        "Errors during file opening", sb.toString());
            }
            
            if (suite.getNumSamples() == 0) {
                errorDialog("Error during file loading",
                        "The selected file(s) contained no readable data.");
            } else {
                if (reallyCreateNewSuite) {
                    suites.add(suite);
                }
            }
            if (suites.size() > 0 && reallyCreateNewSuite) {
                currentSuite = suites.get(suites.size()-1);
            }
            getMainWindow().suitesChanged();
            /*
             * A newly created suite is of course unmodified. If on the other
             * hand we have appended data to an existing suite, it *is* now
             * modified.
             */
            suite.setSaved(reallyCreateNewSuite);
        } catch (FileNotFoundException e) {
            errorDialog("File not found", e.getMessage());
        } catch (IOException e) {
            errorDialog("Error reading file", e.getMessage());
        }
        mainWindow.getMainMenuBar().updateRecentFiles();
        mainWindow.updateSampleDataPanel();
        updateDisplay();
    }
    
    private void updateMainWindowTitle() {
        final MainWindow window = getMainWindow();
        if (currentSuite == null) {
            window.setTitle("PuffinPlot: no data");
            return;
        }
        window.setTitle("PuffinPlot: " + currentSuite.getName() +
                (currentSuite.isSaved() || runningOnOsX()? "" : " *"));
        if (runningOnOsX()) {
            /*
             * See Apple tech note TN2196:
             * https://developer.apple.com/library/mac/technotes/tn2007/tn2196.html#WINDOW_DOCUMENTMODIFIED
             * See also:
             * http://nadeausoftware.com/articles/2009/01/mac_java_tip_how_control_window_decorations
             */
            window.getRootPane().putClientProperty("Window.documentModified",
                    !currentSuite.isSaved());
        }
    }
    
    /**
     * Displays a dialog box reporting an error. The supplied message will be
     * placed within an HTML {@code <p>} element to allow it to word-wrap.
     *
     * @param title the title for the dialog box
     * @param message the message to be displayed
     * @param parent the dialog will be displayed in this component's Frame
     */
    public static void errorDialog(String title, String message,
            Component parent) {
        JOptionPane.showMessageDialog(parent,
                "<html><body><p style='width: 320px;'>"
                + message + "</p></body></html>",
                title,
                JOptionPane.ERROR_MESSAGE);        
    }
    
    /**
     * Displays a dialog box reporting an error over this PuffinApp's main
     * window. This is a convenience wrapper for the static
     * {@link PuffinApp#errorDialog(java.lang.String, java.lang.String, java.awt.Component)}
     * method.
     * 
     * @param title the title for the dialog box
     * @param message the message to be displayed
     */
    public void errorDialog(String title, String message) {
        JOptionPane.showMessageDialog(getMainWindow(),
                "<html><body><p style='width: 320px;'>"
                + message + "</p></body></html>",
                title,
                JOptionPane.ERROR_MESSAGE);
    }
    
    @SuppressWarnings("unchecked")
    private void createAppleEventHandler() {
        try {
            final Class appleEventHandler =
                    ClassLoader.getSystemClassLoader().
                    loadClass("net.talvi.puffinplot.AppleEventHandler");
            appleEventHandler.
                    getDeclaredMethod("initialize", PuffinApp.class).
                    invoke(appleEventHandler, this);
        } catch (NoClassDefFoundError e) {
            errorDialog("EAWT error", "Apple EAWT not supported: Application" +
                    " Menu handling disabled\n(" + e + ")");
        } catch (ClassNotFoundException | NoSuchMethodException |
                SecurityException | IllegalAccessException | 
                IllegalArgumentException | InvocationTargetException e) {
            errorDialog("EAWT error", "Error creating AppleEventHandler: " +
                    e.getLocalizedMessage());
            LOGGER.log(Level.SEVERE, e.getLocalizedMessage());
        }
    }
    
    /**
     * Returns all the Suites currently open within this PuffinApp.
     * 
     * @return all the currently open suites as an unmodifiable list
     */
    public List<Suite> getSuites() {
        return Collections.unmodifiableList(suites);
    }

    /**
     * Returns the currently displayed Suite.
     *
     * @return the current Suite
     */
    public Suite getCurrentSuite() {
        return currentSuite;
    }
    
    /**
     * Sets the currently displayed Suite.
     *
     * @param index the index of the suite to be displayed within PuffinApp's
     * list of suites
     */
    public void setCurrentSuite(int index) {
        if (index >= 0 && index < getSuites().size()) {
            currentSuite = getSuites().get(index);
            getMainWindow().suitesChanged();
            updateMainWindowTitle();
        }
    }
    
    /**
     * Gets the currently displayed Sample
     * 
     * @return the current Sample, or {@code null} if there is no current
     * sample.
     */
    public Sample getCurrentSample() {
        final Suite suite = getCurrentSuite();
        if (suite == null) {
            return null;
        }
        return suite.getCurrentSample();
    }

    /**
     * Gets all the currently selected samples. If there are no selected
     * samples, an empty list will be returned.
     *
     * @return the currently selected samples
     */
    public List<Sample> getSelectedSamples() {
        final List<Sample> result =
                getMainWindow().getSampleChooser().getSelectedSamples();
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }
    
    /**
     * Applies the supplied function to each of the currently selected samples,
     * then redoes any existing calculations for the selected samples and any
     * sites that contain them.
     *
     * @param function function to apply to the currently selected samples
     */
    public void modifySelectedSamples(Consumer<Sample> function) {
        final List<Sample> samples = getSelectedSamples();
        samples.stream().forEach(function);
        recalculateSamplesAndSites(samples);
        updateDisplay();
    }

    /**
     * Returns the site for which data is currently being displayed.
     *
     * @return the current site, or {@code null} if there is no current site
     */
    public Site getCurrentSite() {
        final Sample sample = getCurrentSample();
        if (sample == null) {
            return null;
        }
        return sample.getSite();
    }

    /**
     * Gets all the sites containing any of the currently selected samples.
     *
     * @return all the sites which contain any of the currently selected samples
     */
    public List<Site> getSelectedSites() {
        final List<Sample> samples = getSelectedSamples();
        final Set<Site> siteSet = new java.util.LinkedHashSet<>();
        for (Sample sample: samples) {
            // re-insertion doesn't affect iteration order
            final Site site = sample.getSite();
            if (site != null) {
                siteSet.add(sample.getSite());
            }
        }
        return new ArrayList<>(siteSet);
    }
    
    /**
     * Gets all the samples in all the sites having at least one selected
     * sample.
     *
     * @return all the samples contained in any site containing at least one
     * selected sample
     */
    public List<Sample> getAllSamplesInSelectedSites() {
        final List<Sample> samples = new ArrayList<>();
        for (Site site: getSelectedSites()) {
            samples.addAll(site.getSamples());
        }
        return samples;
    }
    
    /**
     * Terminates this instance of PuffinApp immediately.
     */
    public void quit() {
        for (Suite suite: getSuites()) {
            if (!canSuiteBeClosed(suite)) return;
        }
        getPrefs().save();
        System.exit(0);
    }
    
    /**
     * Shows the application's ‘About’ dialog box, giving brief information
     * about PuffinPlot.
     */
    public void about() {
        aboutBox.setLocationRelativeTo(getMainWindow());
        aboutBox.setVisible(true);
    }

    /**
     * Opens the preferences window.
     */
    public void showPreferences() {
        prefsWindow.setVisible(true);
    }

    /**
     * Opens the page setup dialog box.
     */
    public void showPageSetupDialog() {
        final PrinterJob job = PrinterJob.getPrinterJob();
        currentPageFormat = job.pageDialog(currentPageFormat);
    }

    /**
     * Returns the current page format.
     *
     * @return the current page format
     */
    public PageFormat getCurrentPageFormat() {
        return currentPageFormat;
    }

    /**
     * Returns the data table window.
     *
     * @return the data table window
     */
    public TableWindow getTableWindow() {
        return tableWindow;
    }

    /**
     * Returns the actions associated with this PuffinApp.
     *
     * @return the action associated with this PuffinApp
     */
    public PuffinActions getActions() {
        return actions;
    }

    /**
     * Returns the suite equal-area plot window.
     *
     * @return the suite equal-area plot window
     */
    public SuiteEqAreaWindow getSuiteEqAreaWindow() {
        return suiteEqAreaWindow;
    }
    
    /**
     * Returns the great-circle statistics window.
     *
     * @return the great-circle statistics window
     */
    public SiteMeanWindow getSiteEqAreaWindow() {
        return siteEqAreaWindow;
    }
    
    /**
     * Returns the list of recently used files.
     *
     * @return the list of recently used files
     */
    public RecentFileList getRecentFiles() {
        return recentFiles;
    }

    /**
     * Sets the list of recently used files (allowing it to be restored after
     * restarting the application).
     *
     * @param recentFiles the list of recently used files
     */
    public void setRecentFiles(RecentFileList recentFiles) {
        this.recentFiles = recentFiles;
    }

    /**
     * Shows the window for editing the titles of the custom flags.
     */
    public void showCustomFlagsWindow() {
        if (currentSuite == null) {
            return;
        }
        new CustomFieldEditor(currentSuite.getCustomFlagNames(),
                "Edit custom flags", this);
    }
    
    /**
     * Shows the window for editing the titles of the custom notes.
     */
    public void showCustomNotesWindow() {
        if (currentSuite == null) {
            return;
        }
        new CustomFieldEditor(currentSuite.getCustomNoteNames(),
                "Edit custom notes", this);
    }
    
    /**
     * Performs statistical calculations on AMS data using a script from Lisa
     * Tauxe's pmagpy software suite.
     *
     * @param calcType the type of calculation to perform
     * @param scriptName the external script which will perform the calculations
     */
    public void doAmsCalc(AmsCalculationType calcType, String scriptName) {
        if (showErrorIfNoSuite()) {
            return;
        }
        try {
            final String directory =
                    getPrefs().getPrefs().get("data.pmagPyPath",
                    "/usr/local/bin");
            final File file = new File(directory, scriptName);
            final String scriptPath = file.getAbsolutePath();
            getCurrentSuite().calculateAmsStatistics(getAllSamplesInSelectedSites(),
                 calcType, scriptPath);
        } catch (IOException ioe) {
            errorDialog("Error running AMS script", "The following error "+
                    "occurred:\n" + ioe.getLocalizedMessage()+"\n" +
                    "Please check that the script path is correctly set in "+
                    "the preferences.");
        } catch (IllegalArgumentException iae) {
            errorDialog("Error running AMS script", "The following error "+
                    "occurred:\n" + iae.getLocalizedMessage());
        }
        updateDisplay();
    }
        
    /**
     * Show an "open file" dialog.
     * 
     * @param title title for the dialog
     * @return a list of the files selected by the user; may be empty
     * if no files were selected
     */
    public List<File> openFileDialog(String title) {
        final File startingDir = lastUsedFileOpenDirs.get(title);
        // Returns null if none set, but JFileChooser handles it appropriately.
        List<File> files = Collections.emptyList();
        final boolean useSwingChooserForOpen = !runningOnOsX();
        
        /*
         * If we are on OS X, having this flag set would *prohibit* selection of
         * files, so we make sure it's clear. showOpenMacFileDialog is meant to
         * clear it after showing its dialog, but there's no harm in clearing it
         * here too.
         */
        System.clearProperty("apple.awt.fileDialogForDirectories");
        
        if (useSwingChooserForOpen) {
            final JFileChooser chooser = new JFileChooser(startingDir);
            chooser.setDialogTitle(title);
            chooser.setMultiSelectionEnabled(true);
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int choice = chooser.showOpenDialog(getMainWindow());
            if (choice == JFileChooser.APPROVE_OPTION) {
                files = Arrays.asList(chooser.getSelectedFiles());
                lastUsedFileOpenDirs.put(title, chooser.getCurrentDirectory());
            }
        } else {
            final FileDialog fd = new FileDialog(getMainWindow(), title,
                    FileDialog.LOAD);
            if (startingDir != null) {
                fd.setDirectory(startingDir.getAbsolutePath());
            }
            fd.setMultipleMode(true);
            fd.setVisible(true);
            final File[] fileArray = fd.getFiles();
            if (fileArray.length != 0) {
                lastUsedFileOpenDirs.put(title, new File(fd.getDirectory()));
                files = Arrays.asList(fileArray);
            }
        }
        return files;
    }
    
    /**
     * Show the "Open folder" dialog on Mac OS X.
     */
    public void showMacOpenFolderDialog() {
        final String title = "Open folder";
        List<File> files = Collections.emptyList();
        final FileDialog fd = new FileDialog(getMainWindow(), title,
                    FileDialog.LOAD);
        final String lastUsedDir = lastUsedFileOpenDirs.getString(title);
        if (lastUsedDir != null) {
            fd.setDirectory(lastUsedDir);
        }
        fd.setMultipleMode(true);
        try {
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            fd.setVisible(true);
            final File[] fileArray = fd.getFiles();
            if (fileArray.length != 0) {
                lastUsedFileOpenDirs.put(title, new File(fd.getDirectory()));
                files = Arrays.asList(fileArray);
            }
        } finally {
            /*
             * Setting this property prohibits non-directory selection, and
             * persists for all file open dialogs for the rest of the
             * application's run, so it's important to ensure that it's cleared
             * as soon as we're done with the open-folder dialog.
             */
            System.clearProperty("apple.awt.fileDialogForDirectories");
        }
        if (files != null) {
            openFiles(files, true);
        }
    }
    
    /**
     * Shows an ‘open files’ dialog box.
     *
     * @param createNewSuite If {@code true}, or if there is no current suite, a
     * new suite will be created for the data from the files; otherwise, the
     * data will be added to the current suite.
     */
    public void showOpenFilesDialog(boolean createNewSuite) {
        final List<File> files = openFileDialog("Open file(s)");
        if (files != null) {
            openFiles(files, createNewSuite);
        }
    }
    
    private boolean showErrorIfNoSuite() {
        if (getCurrentSuite() == null) {
            errorDialog("No data file open",
                    "You must open a data file\n"
                    + "before you can perform this operation.");
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Shows an ‘open files’ dialog box; if the user selects any files, AMS data
     * will be imported from them. The files are expected to be in Agico ASC
     * format, as produced by the SAFYR and SUSAR programs.
     */
    public void showImportAmsDialog() {
        if (showErrorIfNoSuite()) {
            return;
        }
        final ImportAmsDialog importAmsDialog = new ImportAmsDialog(this);
        importAmsDialog.setLocationRelativeTo(mainWindow);
        importAmsDialog.setVisible(true);

    }
    
    /**
     * Shows an ‘open file’ dialog box; if the user selects a file, the current
     * preferences will be overwritten with preferences data from that file. The
     * file is expected to contain Java Preferences data in XML format.
     */
    public void showImportPreferencesDialog() {
        final List<File> files = openFileDialog("Import preferences file");
        if (files != null && files.size() > 0) {
            getPrefs().importFromFile(files.get(0));
            getMainWindow().getGraphDisplay().recreatePlots();
            updateDisplay();
        }
    }
    
    /**
     * Shows a confirmation dialog. If the user confirms, all user preferences
     * data is deleted and preferences revert to default values.
     */
    public void clearPreferences() {
        int result = JOptionPane.showConfirmDialog
                (getMainWindow(), "Are you sure you wish to clear "
                + "your preferences\nand reset all preferences to default "
                + "values?",
                "Confirm clear preferences", 
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            getPrefs().getPrefs().clear();
            getPrefs().getPrefs().flush();
            getPrefs().load();
            getMainWindow().getGraphDisplay().recreatePlots();
            updateDisplay();
            prefsWindow = new PrefsWindow(this);
        } catch (BackingStoreException ex) {
            LOGGER.log(Level.WARNING, "Error clearing preferences", ex);
            errorDialog("Error clearing preferences", ex.getLocalizedMessage());
        }
    }
    
    /**
     * Copies the current pattern of selected treatment steps to a clipboard.
     *
     * @see #pasteStepSelection()
     */
    public void copyStepSelection() {
        final Sample sample = getCurrentSample();
        if (sample == null) {
            return;
        }
        stepSelectionClipboard = sample.getSelectionBitSet();
        updateDisplay();
    }
    
    /**
     * For each selected sample, selects the treatment steps corresponding to
     * those last copied to the clipboard.
     *
     * @see #copyStepSelection()
     */
    public void pasteStepSelection() {
        if (stepSelectionClipboard == null) {
            return;
        }
        modifySelectedSamples(s ->
                s.setSelectionBitSet(stepSelectionClipboard));
    }
    
    /**
     * For all selected samples, rotates magnetization data 180° around the
     * specified axis. The intended use is to correct erroneous data caused by
     * incorrect sample orientation during measurement.
     *
     * @param axis the axis around which to flip the selected samples
     */
    public void flipSelectedSamples(MeasurementAxis axis) {
        final List<Sample> samples = getSelectedSamples();
        if (samples.isEmpty()) {
            return;
        }
        final String msgFmt = 
                "You are about to rotate the data for %d selected sample%s\n"
                + "by 180° about the %s axis.\n"
                + "Are you sure you wish to do this?\n"
                + "Press OK to confirm, or Cancel to abort.";
        final String msg = String.format(msgFmt, samples.size(),
                samples.size() == 1 ? "" : "s", axis.toString());
        final int choice = JOptionPane.showConfirmDialog(getMainWindow(), msg,
                "Flip samples", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            modifySelectedSamples(s -> s.flip(axis));
        }
    }
    
    void invertSelectedSamples() {
                final List<Sample> samples = getSelectedSamples();
        if (samples.isEmpty()) return;
        final String msgFmt = 
                "You are about to invert the magnetization data for "
                + "%d selected sample%s.\n"
                + "Are you sure you wish to do this?\n"
                + "Press OK to confirm, or Cancel to abort.";
        final String msg = String.format(msgFmt, samples.size(),
                samples.size() == 1 ? "" : "s");
        final int choice = JOptionPane.showConfirmDialog(getMainWindow(), msg,
                "Invert samples", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            modifySelectedSamples(Sample::invertMoments);
        }
    }
    
    /**
     * Scales all magnetic susceptibility values in the current suite by
     * a user-specified factor.
     */
    public void showRescaleMagSusDialog() {
        if (showErrorIfNoSuite()) {
            return;
        }
        final String factorString = JOptionPane.showInputDialog(
                getMainWindow(),
                "Please enter magnetic susceptibility scaling factor.");
        // My empirically determined value for the Bartington is 4.3e-5.
        if (factorString == null) {
            return;
        }
        try {
            final double factor = Double.parseDouble(factorString);
            getCurrentSuite().rescaleMagSus(factor);
        } catch (NumberFormatException exception) {
            errorDialog("Input error", "That didn't look like a number.");
        }
    }
    
    /**
     * Clears any previously calculated Fisherian or great-circle site
     * directions.
     */
    public void clearSiteCalculations() {
        if (showErrorIfNoSuite()) {
            return;
        }
        for (Site site: getSelectedSites()) {
            site.clearGcFit();
            site.clearFisherStats();
        }
        updateDisplay();
    }
    
    /**
     * Clears the results of any AMS calculations for the current suite.
     */
    public void clearAmsCalcs()  {
        if (showErrorIfNoSuite()) {
            return;
        }
        getCurrentSuite().clearAmsCalculations();
        updateDisplay();
    }
    
    /**
     * Writes a PDF file containing data plots with the current layout
     * for all selected samples. One page is produced per selected sample.
     * 
     * @param pdfFile the PDF file to which to write the plots
     * @throws DocumentException if an error occurred while writing the PDF
     * @throws FileNotFoundException if the file exists but is a directory
     * rather than a regular file, does not exist but cannot be created,
     * or cannot be opened for any other reason 
     */
    public void exportPdfItext(File pdfFile)
            throws FileNotFoundException, DocumentException {
        final MainGraphDisplay display = getMainWindow().getGraphDisplay();
        final Dimension size = display.getMaximumSize();
        com.itextpdf.text.Document document = new com.itextpdf.text.Document(
                new Rectangle(size.width, size.height));
        /*
         * The font mapping is fairly rudimentary at present, and will probably
         * only work for the `standard' Java fonts. Getting it to work properly
         * is non-trivial. One possible approach is to use
         * DefaultFontMapper.insertDirectory for all known possible platform
         * font paths to build up a mapping, but apparently this is too slow to
         * be practical -- see
         * http://www.mail-archive.com/itext-questions@lists.sourceforge.net/msg01669.html
         * The way to go would probably be a custom font mapper (since we only
         * use one font anyway) which uses some platform-informed heuristics to
         * locate the font on disk. See p. 483 of the itext book for more
         * details.
         */
        com.itextpdf.text.pdf.PdfWriter writer =
                com.itextpdf.text.pdf.PdfWriter.getInstance(document,
                new java.io.FileOutputStream(pdfFile));
        final DefaultFontMapper mapper = new DefaultFontMapper();
        
        document.open();
        final PdfContentByte content = writer.getDirectContent();
        int pdfPage = 0;
        boolean finished;
        // a rough imitation of the Java printing interface
        do {
            document.newPage();  // shouldn't make a difference on first pass
            /*
             * It might be worth exposing the "onlyShapes" argument at some
             * point: when set, it converts text to paths. Annoying for anyone
             * who wants to select it, of course, but a better chance that it
             * will be accurately reproduced in the PDF.
             */
            final boolean onlyShapes = false;
            final Graphics2D g2 = new com.itextpdf.awt.PdfGraphics2D(content,
                    size.width, size.height, mapper, onlyShapes, onlyShapes, 99);
            finished = display.printPdfPage(g2, pdfPage);
            g2.dispose();
            pdfPage++;
        } while (!finished);
        document.close();
    }
    
    /**
     * Writes a PDF file containing data plots with the current layout
     * for all selected samples. One page is produced per selected sample.
     * 
     * @param pdfFile the PDF file to which to write the plots
     * @throws IOException if there was an error during file writing
     */
    public void exportPdfFreehep(File pdfFile) throws IOException {
        final MainGraphDisplay display = getMainWindow().getGraphDisplay();
        final Dimension size = display.getMaximumSize();

        final UserProperties userProps = (UserProperties)
                org.freehep.graphicsio.pdf.PDFGraphics2D.getDefaultProperties();
        userProps.setProperty(
                org.freehep.graphicsio.pdf.PDFGraphics2D.TEXT_AS_SHAPES, false);
        userProps.setProperty(
                org.freehep.graphicsio.pdf.PDFGraphics2D.COMPRESS, true);
        userProps.setProperty(
                org.freehep.graphicsio.pdf.PDFGraphics2D.ORIENTATION,
                org.freehep.graphicsio.PageConstants.LANDSCAPE);
        
        org.freehep.graphicsio.pdf.PDFGraphics2D.setDefaultProperties(userProps);
        org.freehep.graphicsio.pdf.PDFGraphics2D graphics2d =
                new org.freehep.graphicsio.pdf.PDFGraphics2D(pdfFile, size);
        final Properties p = new Properties();
        p.setProperty("PageSize", "A4");
        graphics2d.setProperties(p);
        graphics2d.setMultiPage(true);
        graphics2d.startExport();
        
        int pdfPage = 0;
        boolean finished;
        // a rough imitation of the Java printing interface
        do {
            graphics2d.openPage(size, null);
            finished = display.printPdfPage(graphics2d, pdfPage);
            pdfPage++;
            graphics2d.closePage();
        } while (!finished);
        
        graphics2d.endExport();
        /*
         * Don't call graphics2d.closeStream() here: endExport already 
         * calls it, and trying to re-close it will throw an exception. See
         * http://java.freehep.org/vectorgraphics/xref/org/freehep/graphicsio/AbstractVectorGraphicsIO.html#261
         */
    }
    
    /**
     * Calculate means across all currently loaded suites.
     */
    public void calculateMultiSuiteMeans() {
        multiSuiteCalcs = Suite.calculateMultiSuiteMeans(getSuites());
        final StringBuilder meansBuilder = new StringBuilder();
        final List<List<String>> meansStrings = new ArrayList<>(8);
        meansStrings.add(SuiteCalcs.getHeaders());
        meansStrings.addAll(multiSuiteCalcs.toStrings());
        for (List<String> line: meansStrings) {
            boolean first = true;
            for (String cell: line) {
                if (!first) {
                    meansBuilder.append("\t");
                }
                meansBuilder.append(cell);
                first = false;
            }
            meansBuilder.append("\n");
        }
        final String meansString = meansBuilder.toString();
        final JTextArea textArea = new JTextArea(meansString);
        textArea.setTabSize(10);
        JOptionPane.showMessageDialog(getMainWindow(),
                textArea, "Means across all suites",
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    
    void exportCalcsMultiSuite() {
        if (getCurrentSuite() == null) {
                errorDialog("Error saving calculations", "No file loaded.");
                return;
        }
        String pathname = getSavePath("Export multi-suite calculations", ".csv",
                "Comma Separated Values");
        if (pathname != null) {
            try {
                CsvWriter writer = null;
                try {
                    if (multiSuiteCalcs == null) {
                        throw new PuffinUserException(
                                "There are no calculations to save.");
                    }
                    writer = new CsvWriter(new FileWriter(pathname));
                    writer.writeCsv(SuiteCalcs.getHeaders());
                    for (List<String> line: multiSuiteCalcs.toStrings()) {
                        writer.writeCsv(line);
                    }
                } catch (IOException ex) {
                    throw new PuffinUserException(ex);
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            LOGGER.warning(e.getLocalizedMessage());
                        }
                    }
                }
            } catch (PuffinUserException ex) {
                errorDialog("Error saving multi-suite calculations",
                        ex.getLocalizedMessage());
            }
        }
    }
    
    /**
     * Runs a specified Python script, first downloading Jython if required.
     *
     * This method attempts to run a specified Python script. First it checks if
     * the Jython jar is already cached locally in PuffinPlot's application data
     * folder. If not, it prompts the user for confirmation and downloads and
     * caches it from a hardcoded URL. If the download is successful, or if
     * Jython is already available, the script will then be run, with the
     * variable "puffin_app" set to this PuffinApp.
     *
     * @param scriptPath the path to the script
     * @throws IOException if an IO error occurred while running the script
     * @throws ScriptException if a scripting error occurred
     */
    public void runPythonScriptInGui(String scriptPath)
            throws IOException, ScriptException  {
        /*
         * Check whether there's a cached Jython JAR with the expected size. If
         * not, delete and redownload. We don't calculate the SHA-1 here as
         * doing it on every run would be slow, but verifying the size is a very
         * cheap operation.
         */
        if (!JythonJarManager.checkInstalled(true)) {
            final Object[] buttons = {"Download Jython", "Cancel"};
            final String message = "<html><body><p style='width: 400px;'>"
                    + "To run Python scripts, PuffinPlot first needs "
                    + "to download the Jython package from the "
                    + "Internet. Jython will be saved on this computer "
                    + "for future use. The size of the download is "
                    + "around %d MB. Do you wish to proceed with this "
                    + "download now?</p>";
            final long downloadSizeInMB =
                    JythonJarManager.getExpectedDownloadSize() / 1_000_000;
            final int choice = JOptionPane.showOptionDialog(getMainWindow(),
                    String.format(message, downloadSizeInMB),
                    "Jython download required",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,        // no custom icon
                    buttons,
                    buttons[0]); // default option
            if (choice==1) {     // "Cancel" chosen
                return;
            }
            
            final DownloadWorker worker = new DownloadWorker(
                    new URL(JythonJarManager.SOURCE_URL_STRING),
                    JythonJarManager.getPath());
            ProgressDialog.showDialog("Downloading Jython",
                    getMainWindow(), worker);
            /*
             * The progress dialog is modal, so this will block until the
             * download is complete or cancelled.
             */
            if (worker.isCancelled()) {
                JythonJarManager.delete();
                errorDialog("Download cancelled", "The Jython download was "
                        + "cancelled, so the script will not be run.");
                return;
            }
            
            // Verify the SHA-1 checksum.
            try {
                if (!JythonJarManager.checkSha1Digest(true)) {
                    if (worker.getStoredException() != null) {
                        errorDialog("Error during download", "<html>"
                                + "<p style='width: 400px';>"
                                + "An error occured during the "
                                + "download. (Error description: ‘"
                                + worker.getStoredException().getLocalizedMessage()
                                + "’) Please try again. If the error persists, "
                                + "please report it to puffinplot@gmail.com.");
                    } else {
                        // No exception occurred, but the file is corrupted.
                        errorDialog("Download failed",
                                "<html><p style='width: 400px';>"
                                        + "There was a problem with the "
                                        + "downloaded Jython file.\n"
                                        + "Please try again.");
                    }
                    return;
                }
            } catch (NoSuchAlgorithmException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        
        if (pythonEngine == null) {
            pythonEngine = createPythonScriptEngine();
            assert(pythonEngine != null);
        }
        
        pythonEngine.put("puffin_app", this);
        
        try (Reader reader = new FileReader(scriptPath)) {
            pythonEngine.eval(reader);
        }

        updateDisplay();
        getMainWindow().suitesChanged();
    }
    
    static ScriptEngine createPythonScriptEngine() throws IOException {
        final URL url = JythonJarManager.getPath().toUri().toURL();
        final URLClassLoader child = new URLClassLoader (new URL[] {url},
                PuffinApp.class.getClassLoader());
        final ScriptEngineManager sem = new ScriptEngineManager(child);
        return sem.getEngineByName("python");
    }
    
    /**
     * Opens a file selection dialog and runs the Python script (if any) which
     * the user selects from that dialog.
     */
    public void showRunPythonScriptDialog() {
        final List<File> files = openFileDialog("Select Python script");
        if (files.isEmpty()) {
            return;
        }
        final File file = files.get(0);
        try {
            runPythonScriptInGui(file.getAbsolutePath());
        } catch (IOException | ScriptException ex) {
            JOptionPane.showMessageDialog
                    (getMainWindow(), ex.toString(),
                    "Error running Python script",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Runs a specified script written in JavaScript
     *
     * @param scriptPath the path to the JavaScript script
     */
    public void runJavascriptScript(String scriptPath) {
        final ScriptEngineManager sem = new ScriptEngineManager();
        final ScriptEngine se =
                sem.getEngineByMimeType("application/javascript");
        se.put("puffin_app", this);
        try {
            Reader reader = new FileReader(scriptPath);
            se.eval(reader);
        } catch (FileNotFoundException | ScriptException ex) {
            System.out.println(ex.getMessage());
        }
    }
    
    /**
     * Opens a file selection dialog and runs the Javascript script (if any)
     * which the user selects from that dialog.
     */
    public void showRunJavascriptScriptDialog() {
        final List<File> files = openFileDialog("Select Javascript script");
        if (files.isEmpty()) {
            return;
        }
        final File file = files.get(0);
        try {
            runJavascriptScript(file.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog
                    (getMainWindow(), ex.toString(),
                    "Error running Javascript script",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    void showImportLocationsDialog() {
        final List<File> files = openFileDialog("Select location file");
        if (files.isEmpty()) {
            return;
        }
        final File file = files.get(0);
        final Suite suite = getCurrentSuite();
        if (suite==null) return;
        try {
            suite.importLocations(file);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            errorDialog("Error importing locations", ex.getLocalizedMessage());
        }
    }

    /**
     * Returns the version of this PuffinPlot build.
     * 
     * @return the version of this PuffinPlot build
     */
    public Version getVersion() {
        return version;
    }
    
    private void setApplicationIcon() {
        List<Image> icons = new ArrayList<>(10);
        final Toolkit kit = Toolkit.getDefaultToolkit();
        for (String iconName: "256 128 48 32 16".split(" ")) {
            final URL url =
                    PuffinApp.class.getResource("icons/"+iconName+".png" );
            icons.add(kit.createImage(url));
        }
        getMainWindow().setIconImages(icons);
    }
    
    /**
     * Opens the specified URI in the default system browser.
     * 
     * @param uriString the URI to open
     */
    public void openWebPage(String uriString) {
        try {
            Desktop.getDesktop().browse(new URI(uriString));
        } catch (URISyntaxException | IOException ex) {
            errorDialog("Error opening web page", ex.getLocalizedMessage());
        }
    }
    
    /**
     * Saves the current suite under its current filename.
     * <p>
     * If the suite has no current filename, one will be requested from the user
     * using a standard file dialog.
     */
    public void save() {
        save(getCurrentSuite());
    }
    
    /**
     * Shows a print dialog.
     *
     * @param window An identifier specifying the window to print; valid values
     * are MAIN, SITE, and SUITE.
     */
    public void showPrintDialog(String window) {
        final PrinterJob job = PrinterJob.getPrinterJob();
        Printable printable = null;
        switch (window) {
            case "MAIN":
                printable = getMainWindow().getGraphDisplay();
                break;
            case "SITE":
                printable = (Printable) getSiteEqAreaWindow().getContentPane();
                break;
            case "SUITE":
                printable = (Printable) getSuiteEqAreaWindow().getContentPane();
                break;
        }
        job.setPrintable(printable, getCurrentPageFormat());
        try {
            /*
             * Note: if we pass an attribute set to printDialog(), it forces the
             * use of a cross-platform Swing print dialog rather than the
             * default native one.
             */
            if (job.printDialog()) {
                job.print();
            }
        } catch (PrinterException exception) {
            errorDialog("Printing error", exception.getLocalizedMessage());
        }
    }
    
    /**
     * Saves the specified suite under its current filename.
     * 
     * If the suite has no current filename, one will be requested from
     * the user using a standard file dialog.
     * 
     * @param suite the suite to save
     */
    public void save(Suite suite) {
        if (suite != null) {
            if (suite.isFilenameSet()) try {
                suite.save();
                final File file = suite.getPuffinFile();
                getRecentFiles().add(Collections.singletonList(file));
                getMainWindow().getMainMenuBar().updateRecentFiles();
            } catch (PuffinUserException ex) {
                errorDialog("Error saving file", ex.getLocalizedMessage());
            }
            else showSaveAsDialog(suite);
        }
    }
    
    void showSaveAsDialog(Suite suite) {
        final String pathname =
                getSavePath("Save data", ".ppl", "PuffinPlot data");
        if (pathname != null) {
            try {
                final File file = new File(pathname);
                suite.saveAs(file);
                getRecentFiles().add(Collections.singletonList(file));
                getMainWindow().getMainMenuBar().updateRecentFiles();
                updateMainWindowTitle();
            } catch (PuffinUserException ex) {
                errorDialog("Error saving file", ex.getLocalizedMessage());
            }
        }
    }
    
    String getSavePath(final String title, final String extension,
            final String type) {
        final boolean useSwingChooserForSave = !runningOnOsX();
        final String lastDirKey = title + extension + type;
        /*
         * It's a deliberate choice not to use IdToFileMap here -- see comment
         * on lastUsedSaveDirectories declaration.
         */
        final String startingDir =
                lastUsedSaveDirectories.containsKey(lastDirKey) ?
                lastUsedSaveDirectories.get(lastDirKey) : null;
        String pathname = null;
        if (useSwingChooserForSave) {
            final JFileChooser chooser = new JFileChooser(startingDir);
            chooser.setDialogTitle(title);
            if (extension != null && type != null) {
                chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.getName().toLowerCase().endsWith(extension) ||
                            f.isDirectory();
                }

                @Override
                public String getDescription() {
                    return type;
                }
            });}
            int choice = chooser.showSaveDialog(getMainWindow());
            if (choice == JFileChooser.APPROVE_OPTION) {
                pathname = chooser.getSelectedFile().getPath();
            }
        } else {
            final FileDialog fd = new FileDialog(getMainWindow(), title,
                    FileDialog.SAVE);
            if (startingDir != null) {
                fd.setDirectory(startingDir);
            }
            if (extension != null) {
                fd.setFilenameFilter((File dir, String name) ->
                        name.toLowerCase().endsWith(extension) ||
                                (new File(dir, name)).isDirectory());
            }
            fd.setVisible(true);
            if (fd.getFile() == null) { // "cancel" selected
                pathname = null;
            } else { // "save" selected
               pathname = new File(fd.getDirectory(), fd.getFile()).getPath();
            }
        }
        if (pathname != null && extension != null &&
                !pathname.toLowerCase().endsWith(extension))
            pathname += extension;
        if (pathname != null) {
            final File file = new File(pathname);
            lastUsedSaveDirectories.put(lastDirKey, file.getParent());
            if (file.exists()) {
                if (file.isDirectory()) {
                    errorDialog("File exists",
                            "There is already a folder with this filename.\n"
                            + "Please choose another name for the file.");
                    pathname = null;
                } else if (!file.canWrite()) {
                    errorDialog("File exists",
                            "There is already a file with this filename.\n"
                            + "This file cannot be overwritten.\n"
                            + "Please choose another filename.");
                    pathname = null;
                } else {
                    final String[] options = {"Overwrite", "Cancel"};
                    final int option =
                            JOptionPane.showOptionDialog(getMainWindow(),
                            "There is already a file with this name.\n"
                            + "Are you sure you wish to overwrite it?",
                            "File exists",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            null, // icon
                            options,
                            options[1]);
                    if (option == 1) {
                        pathname = null;
                    }
                }
            }
        }
        return pathname;
    }
    
    /**
     * Calculates RPI using two loaded suites.
     */
    public void showCalculateRpiDialog() {
        final RpiDialog rpiDialog = new RpiDialog(suites, getMainWindow());
        if (!rpiDialog.show()) {
            return;
        }
        final String destinationPath = getSavePath("Select RPI output file",
                ".csv", "Comma Separated Values");
        if (destinationPath == null) {
            return;
        }
        SuiteRpiEstimate rpis = null;
        switch (rpiDialog.getEstimateType()) {
            case ARM_DEMAG:
            case IRM_DEMAG:
                rpis = SuiteRpiEstimate.calculateWithStepwiseAF(
                        rpiDialog.getNrm(), rpiDialog.getNormalizer(),
                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
                break;
            case MAG_SUS:
                rpis = SuiteRpiEstimate.calculateWithMagSus(
                        rpiDialog.getNrm(), rpiDialog.getNormalizer());
                break;
        }
        rpis.writeToFile(destinationPath);
    }
    
    /**
     * Shows the dialog for a discrete to continuous sample conversion.
     */
    public void showDiscreteToContinuousDialog() {
        final List<File> files =
                openFileDialog("Select CSV file for conversion");
        if (files.isEmpty()) {
            return;
        }
        final File file = files.get(0);
        final Suite suite = getCurrentSuite();
        if (suite==null) {
            return;
        }
        try {
            suite.convertDiscreteToContinuous(file);
            getMainWindow().suitesChanged();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            errorDialog("Error opening file", ex.getLocalizedMessage());
        } catch (Suite.MissingSampleNameException ex) {
            errorDialog("Error converting suite", "The CSV file does not "
                    + "contain all the sample names for the suite.");
        }
    }
    
    /**
     * Shows the dialog for creating and exporting a data and code bundle.
     */
    public void showCreateBundleDialog() {
        if (getCurrentSuite() == null) {
            errorDialog("No suite loaded", "PuffinPlot cannot create a bundle, "
                    + "as there is no data suite loaded.");
            return;
        }
        
        final JCheckBox includeJarCheckBox =
                new JCheckBox("Include PuffinPlot in bundle");
        includeJarCheckBox.setMnemonic(KeyEvent.VK_I);
        final int selectedOption = JOptionPane.showConfirmDialog(
                getMainWindow(), includeJarCheckBox, "Create bundle",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (selectedOption == JOptionPane.CANCEL_OPTION) {
            return;
        }
        final boolean includeJar = includeJarCheckBox.isSelected();
        
        final String zipPathString = getSavePath("Choose bundle location",
                ".zip", "ZIP archive");
        if (zipPathString == null) {
            return;
        }
        final Path zipPath = Paths.get(zipPathString);
        try {
            final Optional<Exception> jarCopyException =
                    Bundle.createBundle(getCurrentSuite(), zipPath,
                            getCorrection(), getSelectedSamples(),
                            getSelectedSites(), includeJar);
            if (jarCopyException.isPresent()) {
                LOGGER.log(Level.WARNING, "Exception thrown copying jar",
                        jarCopyException.get());
                errorDialog("Error copying jar file", String.format(
                        "An error occurred while copying the PuffinPlot jar "
                        + "file, so the bundle was created without this "
                        + "file. Error message: \"%s\".",
                        jarCopyException.get().getLocalizedMessage()));
            }
        } catch (IOException | PuffinUserException ex) {
            LOGGER.log(Level.SEVERE, "Exception thrown creating bundle", ex);
            errorDialog("Error creating bundle", ex.getLocalizedMessage());
        }
    }
    
    static String getGreatCirclesValidityCondition() {
        return Preferences.userNodeForPackage(PuffinPrefs.class).
                get("data.greatcircles.validityExpr", "true");
    }
}
