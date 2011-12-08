package net.talvi.puffinplot;

import java.awt.FileDialog;
import net.talvi.puffinplot.window.CorrectionWindow;
import net.talvi.puffinplot.window.TableWindow;
import net.talvi.puffinplot.window.FisherWindow;
import net.talvi.puffinplot.window.AboutBox;
import net.talvi.puffinplot.window.MainWindow;
import net.talvi.puffinplot.data.Suite;
import java.awt.event.ActionEvent;
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
import net.talvi.puffinplot.window.GreatCircleWindow;
import net.talvi.puffinplot.window.PrefsWindow;

public final class PuffinApp {

    private static PuffinApp app;
    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");
    private static final ByteArrayOutputStream logStream =
            new ByteArrayOutputStream();
    private static final MemoryHandler logMemoryHandler;
    private final PuffinActions actions;
    private List<Suite> suites = new ArrayList<Suite>();
    private final MainWindow mainWindow;
    private Suite currentSuite;
    private PageFormat currentPageFormat;
    public static final boolean MAC_OS_X =
            System.getProperty("os.name").toLowerCase().startsWith("mac os x");
    private final TableWindow tableWindow;
    private final PuffinPrefs prefs;
    private final FisherWindow fisherWindow;
    private final CorrectionWindow correctionWindow;
    private final GreatCircleWindow greatCircleWindow;
    private final PrefsWindow prefsWindow;
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
    public static final int OSX_POINT_VERSION = getOsxPointVersion();

    static {
        final Handler logStringHandler =
            new StreamHandler(logStream, new SimpleFormatter());
        logStringHandler.setLevel(Level.ALL);
        logger.addHandler(logMemoryHandler =
                new MemoryHandler(logStringHandler, 100, Level.OFF));
        logMemoryHandler.setLevel(Level.ALL);
    }
    
    private static int getOsxPointVersion() {
        final boolean osx =
                System.getProperty("os.name").toLowerCase().startsWith("mac os x");
        if (!osx) return -1;
        
        // split the "10.x.y" version number
        String osVersion = System.getProperty("os.version");
        String[] fragments = osVersion.split("\\.");
        
        // sanity check the "10." part of the version
        if (!fragments[0].equals("10")) return -1;
        if (fragments.length < 2) return -1;
        
        try {
            int minorVers = Integer.parseInt(fragments[1]);
            return minorVers;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    public static PuffinApp getInstance() { return app; }
    
    public List<Suite> getSuites() {
        return Collections.unmodifiableList(suites);
    }

    public boolean isEmptyCorrectionActive() {
        return emptyCorrectionActive;
    }

    public void setEmptyCorrectionActive(boolean b) {
        emptyCorrectionActive = b;
    }

    public void redoCalculations() {
        for (Suite suite: suites) suite.doSampleCalculations(getCorrection());
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
    
    public String getBuildProperty(String propertyName) {
        if (buildProperties == null) return "unknown";
        return buildProperties.getProperty(propertyName, "unknown");
    }
    
    public PuffinApp() {
        logger.info("Instantiating PuffinApp.");
        // have to set app here (not in main) since we need it during initialization
        PuffinApp.app = this;
        // com.apple.macos.useScreenMenuBar deprecated since 1.4, I think
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "PuffinPlot");
        loadBuildProperties();
        prefs = new PuffinPrefs(); // needs PuffinApp.app set.
        actions = new PuffinActions(this);
        tableWindow = new TableWindow();
        fisherWindow = new FisherWindow();
        greatCircleWindow = new GreatCircleWindow();
        correctionWindow = new CorrectionWindow();
        // NB main window must be instantiated last, as
        // the Window menu references the other windows
        mainWindow = new MainWindow();
        // prefs window needs the graph list from MainGraphDisplay from MainWindow
        prefsWindow = new PrefsWindow();
        Correction corr =
                Correction.fromString(prefs.getPrefs().get("correction", "false false NONE"));
        setCorrection(corr);
        getMainWindow().controlPanel.setCorrection(corr);
        if (MAC_OS_X) createAppleEventListener();
        currentPageFormat = PrinterJob.getPrinterJob().defaultPage();
        currentPageFormat.setOrientation(PageFormat.LANDSCAPE);
        aboutBox = new AboutBox(mainWindow);
        mainWindow.getMainMenuBar().updateRecentFiles();
        mainWindow.setVisible(true);
        logger.info("PuffinApp instantiation complete.");
    }

    void doGreatCircles(boolean popUpWindow) {
        for (Site site: getSelectedSites()) site.doGreatCircle(getCorrection());
        if (popUpWindow) greatCircleWindow.setVisible(true);
    }

    void fitCircle() {
        for (Sample sample: getSelectedSamples()) {
            sample.useSelectionForCircleFit();
            sample.fitGreatCircle(getCorrection());
        }
    }

    void doPcaOnSelection() {
        for (Sample sample : getSelectedSamples()) {
            if (sample.getSelectedPoints(getCorrection()).size() > 1) {
                sample.useSelectionForPca();
                sample.doPca(getCorrection());
            }
        }
        updateDisplay();
    }

    GreatCircleWindow getGreatCircleWindow() {
        return greatCircleWindow;
    }

    Site getCurrentSite() {
        Sample sample = getSample();
        if (sample==null) return null;
        return sample.getSite();
    }

    void errorDialog(String title, PuffinUserException ex) {
        errorDialog(title, ex.getLocalizedMessage());
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

    public static void main(String[] args) {
        logger.setLevel(Level.ALL);
        logger.info("Entering main method.");
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
                new Runnable() { public void run() { new PuffinApp(); } });
    }
    
    public PuffinPrefs getPrefs() {
        return prefs;
    }

    public MainWindow getMainWindow() {
        return mainWindow;
    }
 
    public Correction getCorrection() {
        return correction;
    }
    
    public void setCorrection(Correction c) {
        correction = c;
    }
    
    public void updateDisplay() {
        getMainWindow().sampleChanged();
        getTableWindow().dataChanged();
    }

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
    
    public void errorDialog(String title, String message) {
        JOptionPane.showMessageDialog
        (getMainWindow(), message, title, JOptionPane.ERROR_MESSAGE);
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

    public Suite getSuite() {
        return currentSuite;
    }
    
    public Sample getSample() {
        Suite suite = getSuite();
        if (suite==null) return null;
        return suite.getCurrentSample();
    }

    public List<Sample> getSelectedSamples() {
        List<Sample> result =
                getMainWindow().getSampleChooser().getSelectedSamples();
        if (result==null) return Collections.emptyList();
        return result;
    }

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
    
    public List<Sample> getAllSamplesInSelectedSites() {
        final List<Sample> samples = new ArrayList<Sample>();
        for (Site s: PuffinApp.getInstance().getSelectedSites()) {
            samples.addAll(s.getSamples());
        }
        return samples;
    }

    public void setSuite(int index) {
        if (index >= 0 && index < suites.size()) {
            currentSuite = suites.get(index);
            getMainWindow().suitesChanged();
        }
    }
    
    public void quit() {
        getActions().quit.actionPerformed
                (new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
    }
    
    public void about() {
        getActions().about.actionPerformed
                (new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
    }
    
    public void preferences() {
        getActions().prefs.actionPerformed
                (new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
    }

    public void showPreferences() {
        prefsWindow.setVisible(true);
    }

    public void showPageSetupDialog() {
        PrinterJob job = PrinterJob.getPrinterJob();
        currentPageFormat = job.pageDialog(currentPageFormat);
    }

    public PageFormat getCurrentPageFormat() {
        return currentPageFormat;
    }

    public TableWindow getTableWindow() {
        return tableWindow;
    }

    public PuffinActions getActions() {
        return actions;
    }

    public AboutBox getAboutBox() {
        return aboutBox;
    }

    public FisherWindow getFisherWindow() {
        return fisherWindow;
    }

    public CorrectionWindow getCorrectionWindow() {
        return correctionWindow;
    }

    public RecentFileList getRecentFiles() {
        return recentFiles;
    }

    public void setRecentFiles(RecentFileList recentFiles) {
        this.recentFiles = recentFiles;
    }

    public void showCustomFlagsWindow() {
        if (currentSuite == null) return;
        customFlagsWindow =
                new CustomFieldEditor(currentSuite.getCustomFlagNames(),
                "Edit custom flags");
    }
    
    public void showCustomNotesWindow() {
        if (currentSuite == null) return;
        customNotesWindow =
                new CustomFieldEditor(currentSuite.getCustomNoteNames(),
                "Edit custom notes");
    }
    
    public void doAmsCalc(AmsCalcType calcType, String scriptName) {
        try {
            final String directory =
                    app.getPrefs().getPrefs().get("data.pmagPyPath",
                    "/usr/local/bin");
            File f = new File(directory, scriptName);
            final String scriptPath = f.getAbsolutePath();
         app.getSuite().doAmsStatistics(app.getAllSamplesInSelectedSites(),
                 calcType, scriptPath);
        } catch (IOException ioe) {
            app.errorDialog("Error running AMS script", "The following error "+
                    "occurred:\n" + ioe.getLocalizedMessage()+"\n" +
                    "Please check that the script path is correctly set in "+
                    "the preferences.");
        } catch (IllegalArgumentException iae) {
            app.errorDialog("Error running AMS script", "The following error "+
                    "occurred:\n" + iae.getLocalizedMessage());
        }
        app.updateDisplay();
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
            int choice = chooser.showOpenDialog(app.getMainWindow());
            if (choice == JFileChooser.APPROVE_OPTION) {
                files = Arrays.asList(chooser.getSelectedFiles());
                lastUsedFileOpenDirs.put(title, chooser.getCurrentDirectory());
            }
        } else {
            final FileDialog fd = new FileDialog(app.getMainWindow(), title,
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
    
    public void openFile() {
        List<File> files = app.openFileDialog("Open file(s)");
        if (files != null) app.openFiles(files);
    }
    
    public void importAms() {
        try {
            List<File> files = openFileDialog("Select AMS files");
            // app.getSuite().importAms(files, true);
            getSuite().importAmsFromAsc(files, false);
            getMainWindow().suitesChanged();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            errorDialog("Error importing AMS", ex.getLocalizedMessage());
        }
    }
    
    public void importPreferences() {
        List<File> files = openFileDialog("Import preferences file");
        if (files != null && files.size() > 0) {
            getPrefs().importFromFile(files.get(0));
            getMainWindow().getGraphDisplay().recreatePlots();
            updateDisplay();
        }
    }
    
    void copyPointSelection() {
        final Sample sample = getSample();
        if (sample==null) return;
        pointSelectionClipboard = sample.getSelectionBitSet();
        updateDisplay();
    }
    
    void pastePointSelection() {
        if (pointSelectionClipboard==null) return;
        for (Sample sample: getSelectedSamples()) {
            sample.setSelectionBitSet(pointSelectionClipboard);
        }
        updateDisplay();
    }
    
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
}
