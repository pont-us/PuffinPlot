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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.MemoryHandler;
import java.util.logging.StreamHandler;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Sample;

public class PuffinApp {

    private static PuffinApp app;
    private static String buildDate;
    private final Logger logger;
    private final StreamHandler logHandler;
    private final PuffinActions actions;
    List<Suite> suites;
    private final MainWindow mainWindow;
    private int currentSuiteIndex;
    private PageFormat currentPageFormat;
    public static final boolean MAC_OS_X =
            System.getProperty("os.name").toLowerCase().startsWith("mac os x");
    private final TableWindow tableWindow;
    private final PuffinPrefs prefs;
    private final FisherWindow fisherWindow;
    private final CorrectionWindow correctionWindow;
    private RecentFileList recentFiles;
    
    public static PuffinApp getInstance() { return app; }

    public static String getBuildDate() { return buildDate; }
    private boolean emptyCorrectionActive;

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
        for (Suite suite: suites) {
            for (Sample sample: suite.getSamples()) {
                sample.calculateFisher();
                sample.doPca();
            }
        }
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
            ex.printStackTrace();
            /* The only effect of this on the user is a lack of build date
             * in the about box, so we can get away with just printing a stack trace.
             */
        } finally {
            if (propStream != null)
                try {propStream.close();} catch (IOException e) {}
            buildDate = buildDateTmp;
        }
        System.out.println(getBuildDate());
    }
    private final AboutBox aboutBox;
    
    private PuffinApp() {
        // have to set app here (not in main) since we need it during initialization
        PuffinApp.app = this;
        logger = Logger.getLogger("");
        logHandler = new StreamHandler();
        logger.addHandler(new MemoryHandler(null, 1000, Level.OFF));
        // com.apple.macos.useScreenMenuBar deprecated since 1.4, I think
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "PuffinPlot");
        loadBuildProperties();
        prefs = new PuffinPrefs();
        actions = new PuffinActions(this);
        tableWindow = new TableWindow();
        fisherWindow = new FisherWindow();
        correctionWindow = new CorrectionWindow();
        // NB main window must be instantiated last, as
        // the Window menu references the other windows
        mainWindow = new MainWindow();
        suites = new ArrayList<Suite>();
        if (MAC_OS_X) createAppleEventListener();
        currentPageFormat = PrinterJob.getPrinterJob().defaultPage();
        currentPageFormat.setOrientation(PageFormat.LANDSCAPE);
        aboutBox = new AboutBox(mainWindow);
        mainWindow.getMainMenuBar().updateRecentFiles();
        mainWindow.setVisible(true);
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            public void uncaughtException(Thread thread, Throwable exception) {
                final String ERROR_FILE = "PUFFIN-ERROR.txt";
                boolean quit = unhandledErrorDialog();
                File f = new File(System.getProperty("user.home"), ERROR_FILE);
                try {
                    final PrintWriter w = new PrintWriter(new FileWriter(f));
                    w.println("PuffinPlot error file");
                    w.println("Build date: "+buildDate);
                    final Date now = new Date();
                    final SimpleDateFormat df =
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    w.println("Crash date: "+df.format(now));
                    for (String prop: new String[] {
                                "java.version", "java.vendor",
                                "os.name", "os.arch", "os.version",
                                "user.name"}) {
                        w.println(String.format("%-16s%s", prop,
                                System.getProperty(prop)));
                    }
                    exception.printStackTrace(w);
                    w.close();
                } catch (IOException ex) {
                    exception.printStackTrace();
                    ex.printStackTrace();
                }
                if (quit) System.exit(1);
            }
        });

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PuffinApp();
            }
        });
    }
    
    public PuffinPrefs getPrefs() {
        return prefs;
    }

    public MainWindow getMainWindow() {
        return mainWindow;
    }
 
    public Correction getCorrection() {
        return getMainWindow().controlPanel.getCorrection();
    }
    
    public void setCorrection(Correction c) {
        getMainWindow().controlPanel.setCorrection(c);
    }
    
    public void updateDisplay() {
        getMainWindow().controlPanel.updateSample();
        getMainWindow().repaint(100);
        getMainWindow().getMainMenuBar().sampleChanged();
        getTableWindow().dataChanged();
    }
    
    public void openFiles(File f) {
        openFiles(Collections.singletonList(f));
    }

    public void openFiles(List<File> files) {
        openFiles(files, false);
    }

    public void closeCurrentSuite() {
        if (suites == null || suites.isEmpty()) return;
        suites.remove(currentSuiteIndex);
        getMainWindow().suitesChanged();
    }

    public void openFiles(List<File> files, boolean fromRecentFileList) {

        if (files.size() == 0) return;

        if (!fromRecentFileList) {
            try {
                recentFiles.add(files);
            } catch (IOException ex) {
                errorDialog("Error updating recent files list", ex.getLocalizedMessage());
            }
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
                currentSuiteIndex = suites.size()-1;
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
        final JOptionPane pane = new JOptionPane();
        final JLabel message = new JLabel(
                "<html><body style=\"width: 400pt; font-weight: normal;\">" +
                "<p>An unexpected error occurred. </p><p>" +
                "Please <b>make sure that you are using the latest version</b> " +
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
        if (suites==null) return null;
        return suites.isEmpty() ? null : suites.get(currentSuiteIndex);
    }
    
    public Sample getSample() {
        Suite suite = getSuite();
        if (suite==null) return null;
        return suite.getCurrentSample();
    }

    // Only works for discrete, of course.
    public List<Sample> getSelectedSamples() {
        return getMainWindow().getSampleChooser().getSelectedSamples();
    }

    public void setSuite(int selectedIndex) {
        currentSuiteIndex = selectedIndex;
        if (getSuite() != null) getMainWindow().suitesChanged();
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
