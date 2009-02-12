package net.talvi.puffinplot;

import java.awt.event.ActionEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import java.util.Properties;
import javax.swing.JOptionPane;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Sample;

public class PuffinApp {

    private static PuffinApp app;
    private static String buildDate;
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
    
    public static PuffinApp getInstance() { return app; }

    public static String getBuildDate() { return buildDate; }

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
                errorDialog("Serious error",
                        "A major error occurred. Please tell Pont.\n" +
                        "I will try to write the details "+
                        "to a file called PUFFIN-ERROR.txt");
                File f = new File(System.getProperty("user.home"), ERROR_FILE);
                try {
                    PrintWriter w = new PrintWriter(new FileWriter(f));
                    exception.printStackTrace(w);
                    w.close();
                } catch (IOException ex) {
                    exception.printStackTrace();
                    ex.printStackTrace();
                }
            }
        });
        new PuffinApp();
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
    }
    
    public void openFiles(File f) {
        openFiles(new File[] {f});
    }

    public void openFiles(File[] files) {
        if (files.length == 0) return;

        try {
            getPrefs().addRecentFile(files);
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
    
    public static void errorDialog(String title, String message) {
        JOptionPane.showMessageDialog
        (getInstance().getMainWindow(), message, title, JOptionPane.ERROR_MESSAGE);
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
    public Sample[] getSelectedSamples() {
        return getMainWindow().sampleChooser.getSelectedSamples();
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
}
