package net.talvi.puffinplot;

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

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Site;
import net.talvi.puffinplot.window.CustomFieldEditor;
import net.talvi.puffinplot.window.GreatCircleWindow;
import net.talvi.puffinplot.window.PrefsWindow;

public final class PuffinApp {

    private static PuffinApp app;
    private String buildDate;
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
    private RecentFileList recentFiles;


    static {
        final Handler logStringHandler =
            new StreamHandler(logStream, new SimpleFormatter());
        logStringHandler.setLevel(Level.ALL);
        logger.addHandler(logMemoryHandler =
                new MemoryHandler(logStringHandler, 100, Level.OFF));
        logMemoryHandler.setLevel(Level.ALL);
    }

    public static PuffinApp getInstance() { return app; }

    public String getBuildDate() { return buildDate; }
    private boolean emptyCorrectionActive;
    private Correction correction;
    private final GreatCircleWindow greatCircleWindow;
    private final PrefsWindow prefsWindow;

    public List<Suite> getSuites() {
        return suites;
    }

    public boolean isEmptyCorrectionActive() {
        return emptyCorrectionActive;
    }

    public void setEmptyCorrectionActive(boolean b) {
        emptyCorrectionActive = b;
    }

    public void redoCalculations() {
        for (Suite suite: suites) suite.doSampleCalculations();
    }

    private void loadBuildProperties() {
        InputStream propStream = null;
        String buildDateTmp = "unknown";
        try {
            propStream = PuffinApp.class.getResourceAsStream("build.properties");
            Properties props = new Properties();
            props.load(propStream);
            buildDateTmp = props.getProperty("build.date");
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to get build date", ex);
            /* The only effect of this on the user is a lack of build date
             * in the about box, so we can get away with just logging it.
             */
        } finally {
            if (propStream != null)
                try {propStream.close();} catch (IOException e) {}
            buildDate = buildDateTmp;
        }
        logger.log(Level.INFO, "Build date: {0}", getBuildDate());
    }
    private final AboutBox aboutBox;
    
    private PuffinApp() {
        logger.info("Instantiating PuffinApp.");
        // have to set app here (not in main) since we need it during initialization
        PuffinApp.app = this;
        // com.apple.macos.useScreenMenuBar deprecated since 1.4, I think
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "PuffinPlot");
        loadBuildProperties();
        prefs = new PuffinPrefs();
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
        CustomFieldEditor cfe = new CustomFieldEditor();
        logger.info("PuffinApp instantiation complete.");
    }

    void doGreatCircles() {
        for (Site site: getSelectedSites()) site.doGreatCircle();
        greatCircleWindow.setVisible(true);
    }

    void fitCircle() {
        for (Sample sample: getSelectedSamples()) {
            sample.useSelectionForCircleFit();
            sample.fitGreatCircle();
        }
    }

    void doPcaOnSelection() {
        for (Sample sample : getSelectedSamples()) {
            if (sample.getSelectedPoints().size() > 1) {
                sample.useSelectionForPca();
                sample.doPca();
            }
        }
    }

    GreatCircleWindow getGreatCircleWindow() {
        return greatCircleWindow;
    }

    private static class ExceptionHandler implements UncaughtExceptionHandler {
        public void uncaughtException(Thread thread, Throwable exception) {
            final String ERROR_FILE = "PUFFIN-ERROR.txt";
            boolean quit = unhandledErrorDialog();
            File f = new File(System.getProperty("user.home"), ERROR_FILE);
            try {
                final PrintWriter w = new PrintWriter(new FileWriter(f));
                w.println("PuffinPlot error file");
                w.println("Build date: " + getInstance().getBuildDate());
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
        logger.info("Entering main method.");
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
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
        try {
            // If this fileset is already in the recent-files list,
            // it will be bumped up to the top; otherwise it will be
            // added to the top and the last member removed.
            recentFiles.add(files);
        } catch (IOException ex) {
            errorDialog("Error updating recent files list", ex.getLocalizedMessage());
        }
        
        try {
            Suite suite = new Suite(files);
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
            Class appleListener = ClassLoader.getSystemClassLoader()
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
        return getMainWindow().getSampleChooser().getSelectedSamples();
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
}
