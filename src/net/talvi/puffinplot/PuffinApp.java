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

import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.FontMapper;
import com.lowagie.text.pdf.PdfContentByte;
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
import java.io.*;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.MemoryHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import net.talvi.puffinplot.data.*;
import net.talvi.puffinplot.data.Suite.AmsCalcType;
import net.talvi.puffinplot.data.file.FileFormat;
import net.talvi.puffinplot.plots.SampleClickListener;
import net.talvi.puffinplot.window.*;
import org.freehep.util.UserProperties;
import org.python.core.PyException;
import org.python.util.PythonInterpreter;

/**
 * This class constitutes the main PuffinPlot application.
 * Instantiating it starts the PuffinPlot desktop application and opens 
 * the main window. Most of {@code PuffinApp}'s
 * functionality involves interfacing the user interface to the classes
 * which handle the actual data. Most of the actions defined in
 * {@link PuffinActions} act as thin wrappers around one or a few calls 
 * to {@code PuffinApp}. Most of {@code PuffinApp}'s interaction with the
 * data is via the {@link Suite}, {@link Site}, and {@link Sample} classes.
 * 
 * @author pont
 */
public final class PuffinApp {

    private static PuffinApp app;
    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");
    private static final ByteArrayOutputStream logStream =
            new ByteArrayOutputStream();
    private static final MemoryHandler logMemoryHandler;

    private final PuffinActions actions;
    private final List<Suite> suites = new ArrayList<>();
    private Suite currentSuite;
    private PageFormat currentPageFormat;
    private final MainWindow mainWindow;
    private final PuffinPrefs prefs;
    private final TableWindow tableWindow;
    private final SuiteEqAreaWindow suiteEqAreaWindow;
    private final CorrectionWindow correctionWindow;
    private final SiteMeanWindow siteEqAreaWindow;
    private final TreatmentWindow treatmentWindow;
    private PrefsWindow prefsWindow;
    private final AboutBox aboutBox;
    private final CiteWindow citeWindow;
    private RecentFileList recentFiles;
    private CustomFieldEditor customFlagsWindow = null;
    private CustomFieldEditor customNotesWindow;
    private boolean emptyCorrectionActive;
    private Correction correction;
    private final IdToFileMap lastUsedFileOpenDirs;
    private BitSet pointSelectionClipboard = new BitSet(0);
    private Properties buildProperties;
    private static final boolean MAC_OS_X =
            System.getProperty("os.name").toLowerCase().startsWith("mac os x");
    private static final int OSX_POINT_VERSION = determineOsxPointVersion();
    private SuiteCalcs multiSuiteCalcs;
    private final Version version;

    static {
        final Handler logStringHandler =
            new StreamHandler(logStream, new SimpleFormatter());
        logStringHandler.setLevel(Level.ALL);
        logger.addHandler(logMemoryHandler =
                new MemoryHandler(logStringHandler, 100, Level.OFF));
        logMemoryHandler.setLevel(Level.ALL);
    }

    private class PuffinAppSampleClickListener implements SampleClickListener {

        public PuffinAppSampleClickListener() { }

        @Override
        public void sampleClicked(Sample sample) {
            final Suite suite = getSuite();
            getSuite().setCurrentSampleIndex(suite.getIndexBySample(sample));
            getMainWindow().getSampleChooser().updateValueFromSuite();
            updateDisplay();
        }
    }
    
    /**
     * Instantiates a new PuffinPlot application object. Instantiating PuffinApp
     * will cause the main PuffinPlot window to be opened immediately.
     */
    public PuffinApp() {
        logger.info("Instantiating PuffinApp.");
        // have to set app here (not in main) since we need it during initialization
        PuffinApp.app = this;
        // com.apple.macos.useScreenMenuBar deprecated since 1.4, I think
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "PuffinPlot");
        loadBuildProperties();
        version = new Version();
        prefs = new PuffinPrefs(this);
        lastUsedFileOpenDirs = new IdToFileMap(prefs.getPrefs());
        actions = new PuffinActions(this);
        tableWindow = new TableWindow();
        suiteEqAreaWindow = new SuiteEqAreaWindow();
        siteEqAreaWindow = new SiteMeanWindow();
        correctionWindow = new CorrectionWindow();
        treatmentWindow = new TreatmentWindow();
        citeWindow = new CiteWindow();
        // NB main window must be instantiated last, as
        // the Window menu references the other windows
        mainWindow = new MainWindow();
        setApplicationIcon();
        Correction corr =
                Correction.fromString(prefs.getPrefs().get("correction", "false false NONE false"));
        setCorrection(corr);
        getMainWindow().getControlPanel().setCorrection(corr);
        // prefs window needs the graph list from MainGraphDisplay from MainWindow
        // prefs window also needs the correction.
        prefsWindow = new PrefsWindow();
        if (MAC_OS_X) createAppleEventListener();
        currentPageFormat = PrinterJob.getPrinterJob().defaultPage();
        currentPageFormat.setOrientation(PageFormat.LANDSCAPE);
        aboutBox = new AboutBox(mainWindow);
        final MainGraphDisplay display = mainWindow.getGraphDisplay();
        final SampleClickListener scListener = new PuffinAppSampleClickListener();
        display.getPlotByClassName("SampleParamsTable").
                addSampleClickListener(scListener);
        display.getPlotByClassName("SiteParamsTable").
                addSampleClickListener(scListener);
        display.getPlotByClassName("VgpTable").
                addSampleClickListener(scListener);
        mainWindow.getMainMenuBar().updateRecentFiles();
        mainWindow.setVisible(true);
        logger.info("PuffinApp instantiation complete.");
    }

    private static class ExceptionHandler implements UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable exception) {
            final String ERROR_FILE = "PUFFIN-ERROR.txt";
            boolean quit = unhandledErrorDialog();
            File f = new File(System.getProperty("user.home"), ERROR_FILE);
            try {
                final PrintWriter w = new PrintWriter(new FileWriter(f));
                w.println("PuffinPlot error file");
                w.println("Build date: " + getInstance().
                        getBuildProperty("build.date"));
                final Date now = new Date();
                final SimpleDateFormat df =
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                w.println("Crash date: " + df.format(now));
                for (String prop : new String[]{"java.version", "java.vendor",
                            "os.name", "os.arch", "os.version", "user.name"}) {
                    w.println(String.format(Locale.ENGLISH,
                            "%-16s%s", prop,
                            System.getProperty(prop)));
                }
                w.println("Locale: " + Locale.getDefault().toString());
                exception.printStackTrace(w);
                w.println("\nLog messages: \n");
                logMemoryHandler.push();
                logMemoryHandler.flush();
                logStream.flush();
                w.append(logStream.toString());
                w.close();
            } catch (IOException ex) {
                exception.printStackTrace();
                ex.printStackTrace();
            }
            if (quit) {
                System.exit(1);
            }
        }
    }
    
    /**
     * Instantiates and starts a new PuffinApp.
     * @param args command-line arguments for the application
     */
    public static void main(final String[] args) {
        logger.setLevel(Level.ALL);
        logger.fine("Entering main method.");
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        
        final Preferences prefs =
                Preferences.userNodeForPackage(PuffinPrefs.class);
        String lnf = prefs.get("lookandfeel", "Default");
        try {
            if (null != lnf) switch (lnf) {
                case "Native":
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    break;
                case "Metal":
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    break;
                case "Nimbus":
                    /* Nimbus isn't guaranteed to be available on all systems,
                    * so we make sure it's there before trying to set it.
                    * If it's not there, nothing will happen so the system
                    * default will be used.
                    */
                    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                        if ("Nimbus".equals(info.getName())) {
                            UIManager.setLookAndFeel(info.getClassName());
                            break;
                        }
                    }   break;
            }
        } catch (ClassNotFoundException | InstantiationException |
                 IllegalAccessException | UnsupportedLookAndFeelException ex) {
            logger.log(Level.WARNING, "Error setting look-and-feel", ex);
        }

        java.awt.EventQueue.invokeLater(
                new Runnable() { @Override public void run() { 
                    final PuffinApp app = new PuffinApp();
                    app.processArgs(args);
                } });
    }
    
    private void processArgs(String[] args) {
        String scriptPath = null;
        for (int i=0; i<args.length; i++) {
            final String arg = args[i];
            if ("-script".equals(arg) && i<args.length-1) {
                scriptPath = args[i+1];
            }
        }
        if (scriptPath != null) {
            try {
                runPythonScript(scriptPath);
            } catch (PyException ex) {
                System.err.println("Error running Python script "+scriptPath);
                ex.printStackTrace(System.err);
            }
        }
    }
    
    /**
     * Reports whether this PuffinApp is running on Mac OS X.
     * 
     * @return {@code true} if this PuffinApp is running on Mac OS X
     */
    public boolean isOnOsX() {
        return PuffinApp.MAC_OS_X;
    }
    
    private static int determineOsxPointVersion() {
        final boolean osx =
                System.getProperty("os.name").toLowerCase().startsWith("mac os x");
        if (!osx) return -1;
        final String osVersion = System.getProperty("os.version");
        final String[] parts = osVersion.split("\\.");
        if (!parts[0].equals("10")) return -1;
        if (parts.length < 2) return -1;
        try {
            int pointVersion = Integer.parseInt(parts[1]);
            return pointVersion;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * If this PuffinApp is running on Mac OS X, this method returns the
     * point version (minor version) of the operating system. Thus, for 
     * example, it would return 5 if the PuffinApp is running on Mac OS X
     * version 10.5 or any sub-version thereof (10.5.1, 10.5.2, etc.).
     * If PuffinApp is not running on Mac OS X, or if the version cannot
     * be determined, this method returns −1.
     * 
     * @return the OS X minor version number, or −1 if it cannot be determined
     */
    public int getOsxPointVersion() {
        return PuffinApp.OSX_POINT_VERSION;
    }
    
    /**
     * Returns the single instance of PuffinApp. Only one instance of
     * PuffinApp can exist at a time, and it may always be found using
     * this method.
     * 
     * @return the single instance of PuffinApp
     */
    public static PuffinApp getInstance() { return app; }

    /**
     * Reports whether the empty-slot correction is currently active.
     * The empty-slot correction is not currently used, and this
     * method is reserved for a future re-implementation of the feature.
     * 
     * @return {@code} true if the empty-slot correction is active
     */
    public boolean isEmptyCorrectionActive() {
        return emptyCorrectionActive;
    }

    /**
     * Activates or deactivates the empty-slot correction.
     * 
     * @param b {@code true} to activate the empty-slot correction;
     * {@code false} to deactivate it.
     */
    public void setEmptyCorrectionActive(boolean b) {
        emptyCorrectionActive = b;
    }

    /**
     * Recalculates all sample and site calculations in all currently open
     * suites; intended to be called when the correction (none/sample/formation)
     * has changed.
     */
    public void redoCalculations() {
        for (Suite suite: suites) {
            suite.doSampleCalculations(getCorrection());
            suite.doSiteCalculations(getCorrection());
        }
    }

    private void loadBuildProperties() {
        InputStream propStream = null;
        try {
            propStream = PuffinApp.class.getResourceAsStream("build.properties");
            buildProperties = new Properties();
            buildProperties.load(propStream);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to get build date", ex);
            /* The only effect of this on the user is a lack of build date
             * in the about box, so we can get away with just logging it.
             */
        } finally {
            if (propStream != null)
                try {propStream.close();} catch (IOException e) {}
        }
        logger.log(Level.INFO, "Build date: {0}", getBuildProperty("build.date"));
    }
    
    /**
     * Reads values from the {@code build.properties} file. This is a
     * properties file written into the PuffinPlot jar at build time,
     * and currently contains the keys {@code build.date} and
     * {@code build.year}.
     * 
     * @param key the property key to read
     * @return the value of the key
     */
    public String getBuildProperty(String key) {
        if (buildProperties == null) return "unknown";
        return buildProperties.getProperty(key, "unknown");
    }
    
    /**
     * For all selected sites, calculates the site mean direction by 
     * best-fit intersection of great circles fitted to that
     * site's samples.
     */
    public void calculateGreatCirclesDirections() {
        for (Site site: getSelectedSites()) {
            site.calculateGreatCirclesDirection(getCorrection());
        }
        updateDisplay();
    }

    /**
     * Redo existing site calculations affected by specified samples.
     * 
     * Any existing great-circle or Fisherian calculations will be done
     * for any site containing one of the specified samples. Site 
     * calculations will not be done for sites that don't have them already.
     * 
     * This method does not update the display.
     * 
     * @param samples samples which have changed
     */
    private void recalculateAffectedSites(Collection<Sample> samples) {
        Set<Site> affectedSites = new HashSet<>();
        for (Sample sample: samples) {
            if (sample.getSite() != null) {
                affectedSites.add(sample.getSite());
            }
        }
        for (Site site: affectedSites) {
            if (site.getFisherValues() != null) {
                site.calculateFisherStats(correction);
            }
            if (site.getGreatCircles() != null) {
                // PCAs are also used in GC calculations, so this needs
                // to be recalculated even if only the PCA has changed.
                site.calculateGreatCirclesDirection(correction);
            }
        }
    }
    
    /**
     * For all selected samples, fit a great circle to the selected points.
     * 
     * 
     */
    public void fitGreatCirclesToSelection() {
        for (Sample sample: getSelectedSamples()) {
            sample.useSelectionForCircleFit();
            sample.fitGreatCircle(getCorrection());
        }
        recalculateAffectedSites(getSelectedSamples());
        updateDisplay();
    }
    
    /**
     * For all selected samples, determine a best-fit line to the selected
     * points by principal component analysis.
     * 
     * Any affected Fisherian site means or great-circle directions will be
     * recalculated automatically. These directions will not be calculated for
     * any sites that don't have them already.
     */
    public void doPcaOnSelection() {
        for (Sample sample: getSelectedSamples()) {
            if (sample.getSelectedData().size() > 1) {
                sample.useSelectionForPca();
                sample.doPca(getCorrection());
            }
        }
        recalculateAffectedSites(getSelectedSamples());
        updateDisplay();
    }
    
    /**
     * Clear PCA calculations for selected samples.
     * 
     * Any affected site data will also be recalculated.
     * 
     */
    public void clearSelectedSamplePcas() {
        for (Sample s: getSelectedSamples()) {
            s.clearPca();
        }
        recalculateAffectedSites(getSelectedSamples());
        app.updateDisplay();
    }
    
    /**
     * Clear great circle fits for selected samples.
     * 
     * Any affected site data will also be recalculated.
     * 
     */
    public void clearSelectedSampleGcs() {
        for (Sample s: getSelectedSamples()) {
            s.clearGreatCircle();
        }
        recalculateAffectedSites(getSelectedSamples());
        app.updateDisplay();
    }
    
    /**
     * Clear selected points and stored calculations for selected samples.
     * 
     * Any affected site data will also be recalculated.
     * 
     */
    public void clearSelectedSampleCalculations() {
        for (Sample s: getSelectedSamples()) {
            s.clearCalculations();
        }
        recalculateAffectedSites(getSelectedSamples());
        app.updateDisplay();
    }
    
    /** Returns the preferences for this PuffinApp.
     * @return the preferences for this PuffinApp
     */
    public PuffinPrefs getPrefs() {
        return prefs;
    }

    /** Returns this PuffinApp's main window
     * @return this PuffinApp's main window
     */
    public MainWindow getMainWindow() {
        return mainWindow;
    }
    
    /** Returns this PuffinApp's citation window
     * @return this PuffinApp's citation window
     */
    public CiteWindow getCiteWindow() {
        return citeWindow;
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
     * Sets the correction to apply to the displayed data.
     * 
     * @param correction the correction to apply to the displayed data
     */
    public void setCorrection(Correction correction) {
        this.correction = correction;
    }
    
    /**
     * Updates the main window and table window to reflect any changes in
     * the currently displayed data.
     */
    public void updateDisplay() {
        getMainWindow().sampleChanged();
        getTableWindow().dataChanged();
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
    
    /** Closes the suite whose data is currently being displayed. */
    public void closeCurrentSuite() {
        if (suites == null || suites.isEmpty()) return;
        int index = suites.indexOf(currentSuite);
        if (!canSuiteBeClosed(currentSuite)) return;
        suites.remove(currentSuite);
        // Set new current suite to previous (if any), else next (if any),
        // or none.
        if (index > 0) index--;
        if (suites.size() > 0) currentSuite = suites.get(index);
        else currentSuite = null;
        getMainWindow().suitesChanged();
    }

    /** Reads data into the current suite, or a new suite,
     * from the specified files.
     * 
     * @param files the files from which to read data
     * @param createNewSuite whether to create a new suite; if this parameter
     *   is {@code true} or if there is no current suite, a new suite will be
     *   created for the data.
     *
    */
    public void openFiles(List<File> files, boolean createNewSuite) {
        if (files.isEmpty()) return;
        // If this fileset is already in the recent-files list,
        // it will be bumped up to the top; otherwise it will be
        // added to the top and the last member removed.
        recentFiles.add(files);
        
        final boolean reallyCreateNewSuite = createNewSuite || getSuite()==null;

        try {
            final FileType guessedType = FileType.guess(files.get(0));
            FileType fileType;
            if (guessedType == FileType.PUFFINPLOT_NEW ||
                    guessedType == FileType.PUFFINPLOT_OLD) {
                fileType = guessedType;
            } else {
                final FiletypeDialog ft = new FiletypeDialog(getMainWindow());
                ft.setVisible(true);
                fileType = ft.getFileType();
            }
            
            if (fileType == null) {
                return;
            }

            FileFormat format = null;
            Map<Object,Object> importOptions = new HashMap<>();
            switch (fileType) {
                case CUSTOM_TABULAR:
                    final TabularImportWindow tabularDialog =
                            new TabularImportWindow(this);
                    tabularDialog.setVisible(true);
                    format = tabularDialog.getFormat();
                    if (format == null) {
                        return;
                    }
                    format.writeToPrefs(app.getPrefs().getPrefs());
                    break;
                case IAPD:
                    final IapdImportDialog iapdDialog =
                            new IapdImportDialog(getMainWindow());
                    if (!iapdDialog.wasOkPressed()) {
                        return;
                    }
                    importOptions.put(TreatType.class,
                            iapdDialog.getTreatType());
                    importOptions.put(MeasType.class,
                            iapdDialog.getMeasType());
                    break;
            }
            
            final Suite suite;
            if (reallyCreateNewSuite) {
                suite = new Suite("PuffinPlot " + version.versionString);
            } else {
                suite = getSuite();
            }
            suite.readFiles(files, prefs.getSensorLengths(),
                    prefs.get2gProtocol(),
                    !"X/Y/Z".equals(prefs.getPrefs().get("readTwoGeeMagFrom", "X/Y/Z")),
                    fileType, format, importOptions);
            suite.doAllCalculations(getCorrection());
            final List<String> warnings = suite.getLoadWarnings();
            if (warnings.size() > 0) {
                final StringBuilder sb =
                        new StringBuilder(warnings.size() == 1 ? "" :
                                "The following problems occurred:\n");
                int i = 0;
                final int MAX_WARNINGS = 10;
                for (String w: warnings) {
                    if (i == MAX_WARNINGS) {
                        final int remainder = warnings.size() - MAX_WARNINGS;
                        if (remainder == 1) {
                            /* No point adding a "1 more warning omitted" line
                             * when we could just use that line to show the
                             * final warning!
                             */
                            sb.append(w);
                        } else {
                            sb.append("(").append(remainder).
                                    append(" more errors not shown.)");
                        }
                        break;
                    }
                    sb.append(w);
                    sb.append("\n");
                    i++;
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
            // A newly created suite is of course unmodified. If on the other
            // hand we have appended data to an existing suite, it *is* now
            // modified.
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
    
    /**
     * Displays a dialog box reporting an error.
     * 
     * @param title the title for the dialog box
     * @param message the message to be displayed
     */
    public void errorDialog(String title, String message) {
        JOptionPane.showMessageDialog
        (getMainWindow(), message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Displays a dialog box reporting an error.
     * The text of the error box is taken from the supplied
     * exception.
     * 
     * @param title the title for the dialog box
     * @param ex the exception from which to take the message text
     */
    public void errorDialog(String title, PuffinUserException ex) {
        errorDialog(title, ex.getLocalizedMessage());
    }
    
    private static boolean unhandledErrorDialog() {
        final Object[] options = {"Continue", "Quit"};
        String messageText =
                "<html><body style=\"width: 400pt; font-weight: normal;\">" +
                "<p><b>An unexpected error occurred.</b></p>" +
                "<p>We apologize for the inconvenience. Please report this " +
                "error to PP_ADDRESS@gmail.com. " +
                "PuffinPlot will try to write the details " +
                "to a file called PUFFIN-ERROR.txt in your home folder. "+
                "Please attach this file to your report. " +
                "If you have no unsaved data it is recommended that you " +
                "quit now and restart PuffinPlot. " +
                "If you have unsaved data, press Continue, then save your "+
                "data to a new file before quitting PuffinPlot." +
                "</p></body></html>";
        messageText = messageText.replaceAll("PP_ADDRESS", "puffinplot");
        final JLabel message = new JLabel(messageText);
        int response =
                JOptionPane.showOptionDialog(getInstance().getMainWindow(),
                message, "Unexpected error",
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[1]);
        return (response==1);
    }

    @SuppressWarnings("unchecked")
    private void createAppleEventListener() {
        try {
            final Class appleListener = ClassLoader.getSystemClassLoader()
                    .loadClass("net.talvi.puffinplot.AppleListener");
            appleListener.getDeclaredMethod("initialize", PuffinApp.class)
                    .invoke(appleListener, this);
        } catch (NoClassDefFoundError e) {
            // We couldn't find the ApplicationAdapter class.
            errorDialog("EAWT error", "Apple EAWT not supported: Application" +
                    " Menu handling disabled\n(" + e + ")");
        } catch (ClassNotFoundException | NoSuchMethodException |
                SecurityException | IllegalAccessException | 
                IllegalArgumentException | InvocationTargetException e) {
            errorDialog("EAWT error", "Error while loading the OSXAdapter:");
            e.printStackTrace();
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

    /** Returns the current Suite.
     * @return the current Suite */
    public Suite getSuite() {
        return currentSuite;
    }
    
    /** Sets the currently displayed Suite.
     * @param index the index of the suite to be displayed within 
     * PuffinApp's list of suites */
    public void setSuite(int index) {
        if (index >= 0 && index < suites.size()) {
            currentSuite = suites.get(index);
            getMainWindow().suitesChanged();
        }
    }
    
    /**
     * Gets the current Sample
     * 
     * @return the current Sample
     */
    public Sample getSample() {
        Suite suite = getSuite();
        if (suite==null) return null;
        return suite.getCurrentSample();
    }

    /** Gets all the currently selected samples.
     * @return the currently selected samples */
    public List<Sample> getSelectedSamples() {
        List<Sample> result =
                getMainWindow().getSampleChooser().getSelectedSamples();
        if (result==null) return Collections.emptyList();
        return result;
    }

    /** Returns the site for which data is currently being displayed.
     * @return the current site
     */
    public Site getCurrentSite() {
        Sample sample = getSample();
        if (sample==null) return null;
        return sample.getSite();
    }
    
    /** Gets all the sites containing any of the currently selected samples.
     * @return all the sites which contain any of the currently selected samples */
    public List<Site> getSelectedSites() {
        final List<Sample> samples = getSelectedSamples();
        final Set<Site> siteSet = new LinkedHashSet<>();
        for (Sample sample: samples) {
            // re-insertion doesn't affect iteration order
            final Site site = sample.getSite();
            if (site != null) {
                siteSet.add(sample.getSite());
            }
        }
        return new ArrayList<>(siteSet);
    }
    
    /** Gets all the samples in all the sites having at least one selected sample.
     * @return all the samples contained in any site containing at least 
     * one selected sample */
    public List<Sample> getAllSamplesInSelectedSites() {
        final List<Sample> samples = new ArrayList<>();
        for (Site s: PuffinApp.getInstance().getSelectedSites()) {
            samples.addAll(s.getSamples());
        }
        return samples;
    }
    
    /** Terminates this instance of PuffinApp immediately. */
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

    /** Opens the preferences window. */
    public void showPreferences() {
        prefsWindow.setVisible(true);
    }

    /** Opens the page setup dialog box. */
    public void showPageSetupDialog() {
        PrinterJob job = PrinterJob.getPrinterJob();
        currentPageFormat = job.pageDialog(currentPageFormat);
    }

    /** Returns the current page format. 
     * @return the current page format */
    public PageFormat getCurrentPageFormat() {
        return currentPageFormat;
    }

    /** Returns the data table window. 
     * @return the data tabe window */
    public TableWindow getTableWindow() {
        return tableWindow;
    }

    /** Returns the actions associated with this PuffinApp. 
     * @return the action associated with this PuffinApp */
    public PuffinActions getActions() {
        return actions;
    }

    /** Returns the suite equal-area plot window. 
     * @return the suite equal-area plot window */
    public SuiteEqAreaWindow getSuiteEqAreaWindow() {
        return suiteEqAreaWindow;
    }
    
    /** Returns the great-circle statistics window. 
     * @return the great-circle statistics window */
    public SiteMeanWindow getSiteEqAreaWindow() {
        return siteEqAreaWindow;
    }
    
    /** Returns the window for user editing of correction data
     * (sample orientation, formation orientation, geomagnetic declination)
     * @return the correction editing window */
    public CorrectionWindow getCorrectionWindow() {
        return correctionWindow;
    }

    
    /** Returns a window allowing the user to set the treatment type.
     * @return the ‘set treatment type’ window
     */
    public TreatmentWindow getTreatmentWindow() {
        return treatmentWindow;
    }
    
    
    /** Returns the list of recently used files. 
     * @return the list of recently used files */
    public RecentFileList getRecentFiles() {
        return recentFiles;
    }

    /** Sets the list of recently used files (allowing it to be restored
     * after restarting the application).
     * @param recentFiles the list of recently used files
     */
    public void setRecentFiles(RecentFileList recentFiles) {
        this.recentFiles = recentFiles;
    }

    /** Shows the window for editing the titles of the custom flags. */
    public void showCustomFlagsWindow() {
        if (currentSuite == null) return;
        customFlagsWindow =
                new CustomFieldEditor(currentSuite.getCustomFlagNames(),
                "Edit custom flags");
    }
    
    /** Shows the window for editing the titles of the custom notes. */
    public void showCustomNotesWindow() {
        if (currentSuite == null) return;
        customNotesWindow =
                new CustomFieldEditor(currentSuite.getCustomNoteNames(),
                "Edit custom notes");
    }
    
    /**
     * Performs statistical calculations on AMS data using a script from
     * Lisa Tauxe's pmagpy software suite.
     * 
     * @param calcType the type of calculation to perform
     * @param scriptName the external script which will perform the calculations
     */
    public void doAmsCalc(AmsCalcType calcType, String scriptName) {
        if (showErrorIfNoSuite()) return;
        try {
            final String directory =
                    getPrefs().getPrefs().get("data.pmagPyPath",
                    "/usr/local/bin");
            final File file = new File(directory, scriptName);
            final String scriptPath = file.getAbsolutePath();
            getSuite().calculateAmsStatistics(getAllSamplesInSelectedSites(),
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
        
    private List<File> openFileDialog(String title) {
        final File startingDir = lastUsedFileOpenDirs.get(title);
        // Returns null if none set, but JFileChooser handles it appropriately.
        List<File> files = Collections.emptyList();
        final boolean useSwingChooserForOpen = !isOnOsX();
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
            fd.setMultipleMode(true);
            // System.setProperty("apple.awt.fileDialogForDirectories", "true");
            fd.setVisible(true);
            final File[] fileArray = fd.getFiles();
            if (fileArray.length != 0) {
                lastUsedFileOpenDirs.put(title, new File(fd.getDirectory()));
                files = Arrays.asList(fileArray);
            }
        }
        return files;
    }
    
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
        System.setProperty("apple.awt.fileDialogForDirectories", "true");
        fd.setVisible(true);
        final File[] fileArray = fd.getFiles();
        if (fileArray.length != 0) {
            lastUsedFileOpenDirs.put(title, new File(fd.getDirectory()));
            files = Arrays.asList(fileArray);
        }
        if (files != null) openFiles(files, true);
    }
    
    /** Shows an ‘open files’ dialog box; if the user selects any files,
     * they will be opened in a new suite.
     * @param createNewSuite If {@code true}, or if there is no current suite,
     * a new suite will be created for the data from the files; otherwise,
     * the data will be added to the current suite.
     */
    public void showOpenFilesDialog(boolean createNewSuite) {
        List<File> files = openFileDialog("Open file(s)");
        if (files != null) {
            openFiles(files, createNewSuite);
        }
    }
    
    private boolean showErrorIfNoSuite() {
        if (getSuite() == null) {
            errorDialog("No data file open",
                    "You must open a data file\n"
                    + "before you can perform this operation.");
            return true;
        } else {
            return false;
        }
    }
    
    /** <p>Shows an <q>open files</q> dialog box; if the user selects
     * any files, AMS data will be imported from them. The files are 
     * expected to be in Agico ASC format, as produced by the SAFYR
     * and SUSAR programs.</p> */
    public void showImportAmsDialog() {
        if (showErrorIfNoSuite()) {
            return;
        }
        try {
            List<File> files = openFileDialog("Select AMS files");
            getSuite().importAmsFromAsc(files, false);
            getMainWindow().suitesChanged();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            errorDialog("Error importing AMS", ex.getLocalizedMessage());
        }
    }
    
    /** <p>Shows an <q>open file</q> dialog box; if the user select a file,
     * the current preferences will be overwritten with preferences data
     * from that file. The file is expected to contain Java Preferences
     * data in XML format.</p> */
    public void showImportPreferencesDialog() {
        List<File> files = openFileDialog("Import preferences file");
        if (files != null && files.size() > 0) {
            getPrefs().importFromFile(files.get(0));
            getMainWindow().getGraphDisplay().recreatePlots();
            updateDisplay();
        }
    }
    
    /** Shows a confirmation dialog. If the user confirms, all user preferences
     * data is deleted and preferences revert to default values. */
    public void clearPreferences() {
        int result = JOptionPane.showConfirmDialog
                (getMainWindow(), "Are you sure you wish to clear "
                + "your preferences\nand reset all preferences to default "
                + "values?",
                "Confirm clear preferences", 
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) return;
        try {
            getPrefs().getPrefs().clear();
            getPrefs().getPrefs().flush();
            getPrefs().load();
            getMainWindow().getGraphDisplay().recreatePlots();
            updateDisplay();
            prefsWindow = new PrefsWindow();
        } catch (BackingStoreException ex) {
            logger.log(Level.WARNING, "Error clearing preferences", ex);
            errorDialog("Error clearing preferences", ex.getLocalizedMessage());
        }
    }
    
    /** Copies the current pattern of selected points to a clipboard.
     *  @see #pastePointSelection()
     */
    public void copyPointSelection() {
        final Sample sample = getSample();
        if (sample==null) return;
        pointSelectionClipboard = sample.getSelectionBitSet();
        updateDisplay();
    }
    
    /**
     * For each selected sample, selects the points corresponding to those
     * last copied to the clipboard.
     * @see #copyPointSelection()
     */
    public void pastePointSelection() {
        if (pointSelectionClipboard==null) return;
        for (Sample sample: getSelectedSamples()) {
            sample.setSelectionBitSet(pointSelectionClipboard);
        }
        updateDisplay();
    }
    
    /**
     * For all selected samples, rotates magnetization data 180° around
     * the specified axis. The intended use is to correct erroneous
     * data caused by incorrect sample orientation during measurement.
     * 
     * @param axis the axis around which to flip the selected samples
     */
    public void flipSelectedSamples(MeasurementAxis axis) {
        final List<Sample> samples = getSelectedSamples();
        if (samples.isEmpty()) return;
        final String msgFmt = 
                "You are about to rotate the data for %d selected sample%s\n"
                + "by 180° about the %s axis.\n"
                + "Are you sure you wish to do this?\n"
                + "Press OK to confirm, or Cancel to abort.";
        final String msg = String.format(msgFmt, samples.size(),
                samples.size()==1 ? "" : "s", axis.toString());
        final int choice = JOptionPane.showConfirmDialog(getMainWindow(), msg,
                "Flip samples", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            for (Sample sample: getSelectedSamples()) {
                sample.flip(axis);
            }
        }
        updateDisplay();
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
                samples.size()==1 ? "" : "s");
        final int choice = JOptionPane.showConfirmDialog(getMainWindow(), msg,
                "Invert samples", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            for (Sample sample: getSelectedSamples()) {
                sample.invertMoments();
            }
        }
        updateDisplay();
    }
    
    /**
     * Scales all magnetic susceptibility values in the current suite by
     * a user-specified factor.
     */
    public void rescaleMagSus() {
        if (showErrorIfNoSuite()) return;
        final String factorString = JOptionPane.showInputDialog(
                getMainWindow(),
                "Please enter magnetic susceptibility scaling factor.");
        // my empirically determined value for the Bartington is 4.3e-5.
        if (factorString == null) return;
        try {
            final double factor = Double.parseDouble(factorString);
            getSuite().rescaleMagSus(factor);
        } catch (NumberFormatException exception) {
            errorDialog("Input error", "That didn't look like a number.");
        }
    }
    
    /**
     * Clears any previously calculated Fisherian or great-circle site directions.
     */
    public void clearSiteCalculations() {
        if (showErrorIfNoSuite()) return;
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
        if (showErrorIfNoSuite()) return;
        getSuite().clearAmsCalculations();
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
    public void exportPdfItext(File pdfFile) throws FileNotFoundException, DocumentException {
        final MainGraphDisplay display = getMainWindow().getGraphDisplay();
        final Dimension size = display.getMaximumSize();
        com.lowagie.text.Document document =
                new com.lowagie.text.Document(new Rectangle(size.width, size.height));
        // The font mapping is fairly rudimentary at present, and will
        // probably only work for the `standard' Java fonts. Getting it to
        // work properly is non-trivial. One possible approach is to 
        // use DefaultFontMapper.insertDirectory for all known possible
        // platform font paths to build up a mapping, but apparently this
        // is too slow to be practical -- see
        // http://www.mail-archive.com/itext-questions@lists.sourceforge.net/msg01669.html
        // The way to go would probably be a custom font mapper (since 
        // we only use one font anyway) which uses some platform-informed
        // heuristics to locate the font on disk.
        // See p. 483 of the itext book for more details.
        com.lowagie.text.pdf.PdfWriter writer =
                com.lowagie.text.pdf.PdfWriter.getInstance(document,
                new FileOutputStream(pdfFile));
        FontMapper mapper = new DefaultFontMapper();
        document.open();
        PdfContentByte content = writer.getDirectContent();
        int pdfPage = 0;
        boolean finished;
        // a rough imitation of the Java printing interface
        do {
            document.newPage();  // shouldn't make a difference on first pass
            Graphics2D g2 = content.createGraphics(size.width, size.height, mapper);
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
        //OutputStream outStream = new FileOutputStream(pdfFile);
        //org.freehep.graphicsio.pdf.PDFWriter pdfWriter =
        //        new org.freehep.graphicsio.pdf.PDFWriter(outStream);
        UserProperties props=(UserProperties)org.freehep.graphicsio.pdf.PDFGraphics2D.getDefaultProperties();
        props.setProperty(org.freehep.graphicsio.pdf.PDFGraphics2D.TEXT_AS_SHAPES, false);
        props.setProperty(org.freehep.graphicsio.pdf.PDFGraphics2D.COMPRESS, true);
        props.setProperty(org.freehep.graphicsio.pdf.PDFGraphics2D.ORIENTATION, org.freehep.graphicsio.PageConstants.LANDSCAPE);
        org.freehep.graphicsio.pdf.PDFGraphics2D.setDefaultProperties(props);
        org.freehep.graphicsio.pdf.PDFGraphics2D graphics2d =
                new org.freehep.graphicsio.pdf.PDFGraphics2D(pdfFile, size);
        Properties p = new Properties();
        p.setProperty("PageSize","A4");
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
        
        //getMainWindow().getGraphDisplay().print(graphics2d);
        graphics2d.endExport();
        graphics2d.closeStream();
        
    }
    
    /**
     * Calculate means across all currently loaded suites.
     */
    public void calculateMultiSuiteMeans() {
        multiSuiteCalcs = Suite.calculateMultiSuiteMeans(suites);
        StringBuilder meansBuilder = new StringBuilder();
        List<List<String>> meansStrings = new ArrayList<>(8);
        meansStrings.add(SuiteCalcs.getHeaders());
        meansStrings.addAll(multiSuiteCalcs.toStrings());
        for (List<String> line: meansStrings) {
            boolean first = true;
            for (String cell: line) {
                if (!first) meansBuilder.append("\t");
                meansBuilder.append(cell);
                first = false;
            }
            meansBuilder.append("\n");
        }
        String meansString = meansBuilder.toString();
        final JTextArea textArea = new JTextArea(meansString);
        textArea.setTabSize(10);
        JOptionPane.showMessageDialog(getMainWindow(),
                textArea, "Means across all suites",
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    
    void exportCalcsMultiSuite() {
        if (app.getSuite() == null) {
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
                        throw new PuffinUserException("There are no calculations to save.");
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
                        try { writer.close(); }
                        catch (IOException e) { logger.warning(e.getLocalizedMessage()); }
                    }
                }
            } catch (PuffinUserException ex) {
                errorDialog("Error saving multi-suite calculations",
                        ex.getLocalizedMessage());
            }
        }
    }
    
    /**
     * Runs a specified Python script
     * 
     * @param scriptPath the path to the script
     * @throws PyException if an error occurred while running the script
     */
    public void runPythonScript(String scriptPath) throws PyException {
        PythonInterpreter interp = new PythonInterpreter();
        interp.set("puffin_app", this);
        interp.execfile(scriptPath);
        updateDisplay();
        getMainWindow().suitesChanged();
    }
    
    /**
     * Opens a file selection dialog and runs the Python script
     * (if any) which the user selects from that dialog. 
     */
    public void showRunPythonScriptDialog() {
        final List<File> files = openFileDialog("Select Python script");
        if (files.isEmpty()) return;
        final File file = files.get(0);
        try {
            runPythonScript(file.getAbsolutePath());
        } catch (PyException ex) {
            JOptionPane.showMessageDialog
                    (getMainWindow(), ex.toString(),
                    "Error running Python script",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    void showImportLocationsDialog() {
        final List<File> files = openFileDialog("Select location file");
        if (files.isEmpty()) return;
        final File file = files.get(0);
        final Suite suite = getSuite();
        if (suite==null) return;
        try {
            suite.importLocations(file);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
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
    
    /**
     * A class containing information about PuffinPlot's version.
     */
    public class Version {
        private final String versionString;
        private final String dateString;
        private final String yearRange;
        
        private Version() {
            String hgRev = getBuildProperty("build.hg.revid");
            final String hgDate = getBuildProperty("build.hg.date");
            final String hgTag = getBuildProperty("build.hg.tag");
            final boolean modified = hgRev.endsWith("+");
            hgRev = hgRev.replace("+", "");
            if (hgTag.startsWith("version_") && !modified) {
                versionString = hgTag.substring(8);
            } else {
                versionString = hgRev +
                        (modified ? " (modified)" : "");
            }
            /* The filtered hgdate format consists of an epoch time
             * in UTC, a space, and a timezone offset in seconds.
             * We don't care about the timezone, so we just take the
             * first part. */
            final String hgEpochDate = hgDate.split(" ")[0];
            final String buildDate = getBuildProperty("build.date");
            String dateStringTmp =  buildDate +
                    " (date of build; revision date not available)";
            try {
                final Date date = new Date(Long.parseLong(hgEpochDate) * 1000);
                final DateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm");
                dateStringTmp = df.format(date);
            } catch (NumberFormatException ex) {
                 // Nothing to do -- we just fall back to the default string.
            }
            dateString = dateStringTmp;
            
            String yearTmp = "2012";
            if (!"unknown".equals(buildDate)) {
                yearTmp = buildDate.substring(0, 4);
            }
            if ("2012".equals(yearTmp)) {
                yearRange = "2012";
            } else {
                yearRange = "2012–"+yearTmp;
            }
        }

        /**
         * @return a string representing this Version
         */
        public String getVersionString() {
            return versionString;
        }

        /**
         * @return a string representing the release date of this Version
         */
        public String getDateString() {
            return dateString;
        }
        
        /**
         * @return a string representing the copyright year range of this Version
         */
        public String getYearRange() {
            return yearRange;
        }
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
            app.errorDialog("Error opening web page", ex.getLocalizedMessage());
        }
    }
    
    /**
     * Saves the current suite under its current filename.
     * 
     * If the suite has no current filename, one will be requested from
     * the user using a standard file dialog.
     */
    public void save() {
        save(getSuite());
    }
    
    /**
     * Shows a print dialog.
     * 
     * @param window An identifier specifying the window to print;
     * valid values are MAIN, SITE, and SUITE.
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
            /* Note: if we pass an attribute set to printDialog(),
            * it forces the use of a cross-platform Swing print
            * dialog rather than the default native one. */
            if (job.printDialog()) job.print();
        } catch (PrinterException pe) {
            errorDialog("Printing error", pe.getLocalizedMessage());
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
                    app.getRecentFiles().add(Collections.singletonList(file));
                    app.getMainWindow().getMainMenuBar().updateRecentFiles();
                } catch (PuffinUserException ex) {
                    app.errorDialog("Error saving file", ex);
                }
                else doSaveAs(suite);
            }
    }
    
    void doSaveAs(Suite suite) {
        String pathname = getSavePath("Save data", ".ppl", "PuffinPlot data");
        if (pathname != null) {
            try {
                final File file = new File(pathname);
                suite.saveAs(file);
                getRecentFiles().add(Collections.singletonList(file));
                getMainWindow().getMainMenuBar().updateRecentFiles();
            } catch (PuffinUserException ex) {
                errorDialog("Error saving file", ex);
            }
        }
    }
    
    String getSavePath(final String title, final String extension,
            final String type) {
        final boolean useSwingChooserForSave = !app.isOnOsX();
        String pathname = null;
        if (useSwingChooserForSave) {
            JFileChooser chooser = new JFileChooser();
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
            int choice = chooser.showSaveDialog(app.getMainWindow());
            if (choice == JFileChooser.APPROVE_OPTION)
                pathname = chooser.getSelectedFile().getPath();
        } else {
            FileDialog fd = new FileDialog(app.getMainWindow(), title,
                    FileDialog.SAVE);
            if (extension != null) {
            fd.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(extension) ||
                            (new File(dir, name)).isDirectory();
                }
            });}
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
            if (file.exists()) {
                if (file.isDirectory()) {
                    app.errorDialog("File exists",
                            "There is already a folder with this filename.\n"
                            + "Please choose another name for the file.");
                    pathname = null;
                } else if (!file.canWrite()) {
                    app.errorDialog("File exists",
                            "There is already a file with this filename.\n"
                            + "This file cannot be overwritten.\n"
                            + "Please choose another filename.");
                    pathname = null;
                } else {
                    final String[] options = {"Overwrite", "Cancel"};
                    final int option =
                            JOptionPane.showOptionDialog(app.getMainWindow(),
                            "There is already a file with this name.\n"
                            + "Are you sure you wish to overwrite it?",
                            "File exists",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            null, // icon
                            options,
                            options[1]);
                    if (option==1) pathname = null;
                }
            }
        }
        return pathname;
    }
    
    /**
     * Calculate RPI using two loaded suites.
     * 
     * Currently experimental.
     */
    public void calculateRpi() {
        final Suite nrmSuite = getSuites().get(0);
        final Suite armSuite = getSuites().get(1);
        //final double demagStep = 0.05;
        final File outFile = new File("/home/pont/test.tsv");
        FileWriter fw = null;
        try {
                fw = new FileWriter(outFile);

                for (Sample nrmSample: nrmSuite.getSamples()) {
                    final String depth = nrmSample.getData().get(0).getDepth();
                    final Sample armSample = armSuite.getSampleByName(depth);
                    if (armSample != null) {
                        final double[] levels = {0.03, 0.04, 0.05, 0.06, 0.07};
                        fw.write(depth);
                        for (double demagStep: levels) {
                            final Datum nrmStep = nrmSample.getDatumByTreatmentLevel(demagStep);
                            final Datum armStep = armSample.getDatumByTreatmentLevel(demagStep);
                            if (nrmStep != null && armStep != null) {
                                final double nrmInt = nrmStep.getIntensity();
                                final double armInt = armStep.getIntensity();
                                fw.write(String.format(Locale.ENGLISH,
                                        "\t%g", nrmInt/armInt));
                            }
                        }
                        fw.write("\n");
                    }
                }

            } catch (IOException e) {
                logger.log(Level.WARNING,
                        "calculateRpi: exception writing file.", e);
            } finally {
                try {
                    if (fw != null) fw.close();
                } catch (IOException e2) {
                    logger.log(Level.WARNING, 
                            "calculateRpi: exception closing file.", e2);
                }
            }
    }
    
}
