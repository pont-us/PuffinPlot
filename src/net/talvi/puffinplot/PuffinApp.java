package net.talvi.puffinplot;

import java.awt.FileDialog;
import java.util.prefs.BackingStoreException;
import net.talvi.puffinplot.data.SuiteCalcs;
import net.talvi.puffinplot.window.CorrectionWindow;
import net.talvi.puffinplot.window.TableWindow;
import net.talvi.puffinplot.window.FisherWindow;
import net.talvi.puffinplot.window.AboutBox;
import net.talvi.puffinplot.window.MainWindow;
import net.talvi.puffinplot.data.Suite;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.MemoryHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import static java.lang.Thread.UncaughtExceptionHandler;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Site;
import net.talvi.puffinplot.data.Suite.AmsCalcType;
import net.talvi.puffinplot.window.CustomFieldEditor;
import net.talvi.puffinplot.window.SiteMeanWindow;
import net.talvi.puffinplot.window.PrefsWindow;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.FontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.FileOutputStream;
import javax.swing.JTextArea;
import net.talvi.puffinplot.data.CsvWriter;
import net.talvi.puffinplot.window.*;
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
    private List<Suite> suites = new ArrayList<Suite>();
    private Suite currentSuite;
    private PageFormat currentPageFormat;
    private final MainWindow mainWindow;
    private final PuffinPrefs prefs;
    private final TableWindow tableWindow;
    private final FisherWindow fisherWindow;
    private final CorrectionWindow correctionWindow;
    private final SiteMeanWindow greatCircleWindow;
    private PrefsWindow prefsWindow;
    private final AboutBox aboutBox;
    private RecentFileList recentFiles;
    private CustomFieldEditor customFlagsWindow = null;
    private CustomFieldEditor customNotesWindow;
    private boolean emptyCorrectionActive;
    private Correction correction;
    private final Map<String,File> lastUsedFileOpenDirs =
            new HashMap<String,File>();
    private static final boolean useSwingChooserForOpen = true;
    private BitSet pointSelectionClipboard = new BitSet(0);
    private Properties buildProperties;
    private static final boolean MAC_OS_X =
            System.getProperty("os.name").toLowerCase().startsWith("mac os x");
    private static final int OSX_POINT_VERSION = determineOsxPointVersion();
    private SuiteCalcs multiSuiteCalcs;

    static {
        final Handler logStringHandler =
            new StreamHandler(logStream, new SimpleFormatter());
        logStringHandler.setLevel(Level.ALL);
        logger.addHandler(logMemoryHandler =
                new MemoryHandler(logStringHandler, 100, Level.OFF));
        logMemoryHandler.setLevel(Level.ALL);
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
        prefs = new PuffinPrefs(this);
        actions = new PuffinActions(this);
        tableWindow = new TableWindow();
        fisherWindow = new FisherWindow();
        greatCircleWindow = new SiteMeanWindow();
        correctionWindow = new CorrectionWindow();
        // NB main window must be instantiated last, as
        // the Window menu references the other windows
        mainWindow = new MainWindow();
        // prefs window needs the graph list from MainGraphDisplay from MainWindow
        prefsWindow = new PrefsWindow();
        Correction corr =
                Correction.fromString(prefs.getPrefs().get("correction", "false false NONE"));
        setCorrection(corr);
        getMainWindow().getControlPanel().setCorrection(corr);
        if (MAC_OS_X) createAppleEventListener();
        currentPageFormat = PrinterJob.getPrinterJob().defaultPage();
        currentPageFormat.setOrientation(PageFormat.LANDSCAPE);
        aboutBox = new AboutBox(mainWindow);
        mainWindow.getMainMenuBar().updateRecentFiles();
        mainWindow.setVisible(true);
        logger.info("PuffinApp instantiation complete.");
    }

    private static class ExceptionHandler implements UncaughtExceptionHandler {
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
                    w.println(String.format("%-16s%s", prop,
                            System.getProperty(prop)));
                }
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
            if ("Native".equals(lnf)) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } else if ("Metal".equals(lnf)) {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } else if ("Nimbus".equals(lnf)) {
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
                }
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error setting look-and-feel", ex);
        }

        java.awt.EventQueue.invokeLater(
                new Runnable() { public void run() { 
                    final PuffinApp app = new PuffinApp();
                    app.processArgs(args);
                } });
    }
    
    private void processArgs(String[] args) {
        int i=0;
        String scriptPath = null;
        for (i=0; i<args.length; i++) {
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
     * 
     * @param popUpWindow if {@code true}, open a new window showing 
     * great circle directions
     */
    public void calculateGreatCirclesDirections(boolean popUpWindow) {
        for (Site site: getSelectedSites()) {
            site.calculateGreatCirclesDirection(getCorrection());
        }
        if (popUpWindow) greatCircleWindow.setVisible(true);
    }

    /**
     * For all selected samples, fit a great circle to the selected points.
     */
    public void fitGreatCirclesToSelection() {
        for (Sample sample: getSelectedSamples()) {
            sample.useSelectionForCircleFit();
            sample.fitGreatCircle(getCorrection());
        }
    }

    /**
     * For all selected samples, determine a best-fit line to the selected
     * points by principal component analysis.
     */
    public void doPcaOnSelection() {
        for (Sample sample: getSelectedSamples()) {
            if (sample.getSelectedData().size() > 1) {
                sample.useSelectionForPca();
                sample.doPca(getCorrection());
            }
        }
        updateDisplay();
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

    /** Closes the suite whose data is currently being displayed. */
    public void closeCurrentSuite() {
        if (suites == null || suites.isEmpty()) return;
        int index = suites.indexOf(currentSuite);
        suites.remove(currentSuite);
        // Set new current suite to previous (if any), else next (if any),
        // or none.
        if (index > 0) index--;
        if (suites.size() > 0) currentSuite = suites.get(index);
        else currentSuite = null;
        getMainWindow().suitesChanged();
    }

    /** Creates a new suite and reads data into it from the specified files.
     * @param files the files from which to read data */
    public void openFiles(List<File> files) {
        if (files.isEmpty()) return;
        // If this fileset is already in the recent-files list,
        // it will be bumped up to the top; otherwise it will be
        // added to the top and the last member removed.
        recentFiles.add(files);
        
        try {
            Suite suite = new Suite(files, prefs.getSensorLengths(),
                    prefs.get2gProtocol());
            suite.doAllCalculations(getCorrection());
            List<String> warnings = suite.getLoadWarnings();
            if (warnings.size() > 0) {
                StringBuilder sb =
                        new StringBuilder("The following problems occurred:\n");
                for (String w: warnings) {
                    sb.append(w);
                    sb.append("\n");
                }
                errorDialog("Errors during file loading", sb.toString());
            }
            
            if (suite.getNumSamples() == 0) {
                errorDialog("Error during file loading",
                        "The selected file(s) contained no readable data.");
            } else {
                suites.add(suite);
            }
            if (suites.size() > 0) {
                currentSuite = suites.get(suites.size()-1);
                getMainWindow().suitesChanged();
            }
        } catch (FileNotFoundException e) {
            errorDialog("File not found", e.getMessage());
        } catch (IOException e) {
            errorDialog("Error reading file", e.getMessage());
        }
        mainWindow.getMainMenuBar().updateRecentFiles();
        mainWindow.updateSampleDataPanel();
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
        final JLabel message = new JLabel(
                "<html><body style=\"width: 400pt; font-weight: normal;\">" +
                "<p>An unexpected error occurred. </p><p>" +
                "Please make sure that you are using the latest version " +
                "of PuffinPlot. If so, report the error to Pont. " +
                "I will try to write the details " +
                "to a file called PUFFIN-ERROR.txt . "+
                "I recommend that you quit, but if you have unsaved "+
                "data you could try to continue and save it.</p></body></html>")
                ;
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
        } catch (Exception e) {
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
     * PuffinApp's list of suites
     * 
     */
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
        List<Sample> samples = getSelectedSamples();
        Set<Site> siteSet = new LinkedHashSet<Site>();
        for (Sample sample: samples) {
            // re-insertion doesn't affect iteration order
            Site site = sample.getSite();
            if (site != null) {
                siteSet.add(sample.getSite());
            }
        }
        return new ArrayList<Site>(siteSet);
    }
    
    /** Gets all the samples in all the sites having at least one selected sample.
     * @return all the samples contained in any site containing at least 
     * one selected sample */
    public List<Sample> getAllSamplesInSelectedSites() {
        final List<Sample> samples = new ArrayList<Sample>();
        for (Site s: PuffinApp.getInstance().getSelectedSites()) {
            samples.addAll(s.getSamples());
        }
        return samples;
    }
    
    /** Terminates this instance of PuffinApp immediately. */
    public void quit() {
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

    /** Returns the Fisherian statistics window. 
     * @return the Fisherian statistics window */
    public FisherWindow getFisherWindow() {
        return fisherWindow;
    }
    
    /** Returns the great-circle statistics window. 
     * @return the great-circle statistics window */
    public SiteMeanWindow getGreatCircleWindow() {
        return greatCircleWindow;
    }
    
    /** Returns the window for user editing of correction data
     * (sample orientation, formation orientation, geomagnetic declination)
     * @return the correction editing window */
    public CorrectionWindow getCorrectionWindow() {
        return correctionWindow;
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
        if (!lastUsedFileOpenDirs.containsKey(title)) {
            lastUsedFileOpenDirs.put(title, null);
        }
        final File startingDir = lastUsedFileOpenDirs.get(title);
        List<File> files = Collections.emptyList();
        if (useSwingChooserForOpen) {
            final JFileChooser chooser = new JFileChooser(startingDir);
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
            fd.setVisible(true);
            String filename = fd.getFile();
            if (filename != null) {
                final File file = new File(fd.getDirectory(), fd.getFile());
                files = Collections.singletonList(file);
                lastUsedFileOpenDirs.put(title, new File(fd.getDirectory()));
            }
        }
        return files;
    }
    
    /** Shows an ‘open files’ dialog box; if the user selects any files,
     * they will be opened in a new suite. */
    public void openFilesWithDialog() {
        List<File> files = openFileDialog("Open file(s)");
        if (files != null) openFiles(files);
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
    
    /** <p>Shows an <q>open files</q> dialog box; if the user selects any files,
     * AMS data will be imported from them. The files are expected to
     * be in Agico ASC format.</p> */
    public void importAmsWithDialog() {
        if (showErrorIfNoSuite()) return;
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
    public void importPreferencesWithDialog() {
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
            site.clearFisher();
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
    public void exportPdf(File pdfFile) throws FileNotFoundException, DocumentException {
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
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
        FontMapper mapper = new DefaultFontMapper();
        document.open();
        PdfContentByte content = writer.getDirectContent();
        int pdfPage = 0;
        boolean finished = false;
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
    
    public void calculateMultiSuiteMeans() {
        multiSuiteCalcs = Suite.calculateMultiSuiteMeans(suites);
        StringBuilder meansBuilder = new StringBuilder();
        List<List<String>> meansStrings = new ArrayList<List<String>>(8);
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
        String pathname = actions.getSavePath("Export multi-suite calculations", ".csv",
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
    public void runPythonScriptWithDialog() {
        final List<File> files = openFileDialog("Select Python script");
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
    
    void importTabularData() {
        TabularImportWindow importWindow = new TabularImportWindow();
        importWindow.setVisible(true);
    }

}
