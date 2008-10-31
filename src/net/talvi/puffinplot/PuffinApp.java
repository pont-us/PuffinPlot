package net.talvi.puffinplot;

import java.awt.event.ActionEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Sample;

public class PuffinApp {

    private static PuffinApp app;

    public static PuffinApp getApp() {
        return app;
    }
    final PuffinActions actions;
    List<Suite> suites;
    private MainWindow mainWindow;
    private int currentSuiteIndex;
    private PageFormat currentPageFormat;
    public static final boolean MAC_OS_X = (System.getProperty("os.name").
            toLowerCase().startsWith("mac os x"));
    private TableWindow tableWindow;
    private Prefs prefs;

    public Prefs getPrefs() {
        return prefs;
    }

    public MainWindow getMainWindow() {
        return mainWindow;
    }
    
    public static class Prefs {
        private boolean axisScaleLocked;
        private boolean pcaAnchored;

        public boolean isAxisScaleLocked() {
            return axisScaleLocked;
        }

        public void setAxisScaleLocked(boolean axisScaleLocked) {
            this.axisScaleLocked = axisScaleLocked;
        }

        public boolean isPcaAnchored() {
            return pcaAnchored;
        }

        public void setPcaAnchored(boolean pcaThroughOrigin) {
            this.pcaAnchored = pcaThroughOrigin;
        }
    }

    private PuffinApp() {
        // have to set app here (not in main) since we need it during initialization
        PuffinApp.app = this; 
        
        // com.apple.macos.useScreenMenuBar deprecated since 1.4, I think
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "PuffinPlot");

        prefs = new Prefs();
        actions = new PuffinActions(this);
        tableWindow = new TableWindow();
        mainWindow = new MainWindow();
        suites = new ArrayList<Suite>();
        if (MAC_OS_X) createAppleEventListener();
        
        currentPageFormat = PrinterJob.getPrinterJob().defaultPage();
        currentPageFormat.setOrientation(PageFormat.LANDSCAPE);
        
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
                File f = new File(System.getProperty("user.home"), "PUFFIN-ERROR.txt");
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
    
    public Correction currentCorrection() {
        return getMainWindow().controlPanel.getCorrection();
    }
    
    public void openFiles(File f) {
        openFiles(new File[] {f});
    }

    public void openFiles(File[] files) {
        if (files.length == 0) return;

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
    }
    
    public static void errorDialog(String title, String message) {
        JOptionPane.showMessageDialog
        (getApp().getMainWindow(), message, title, JOptionPane.ERROR_MESSAGE);
    }
    
    @SuppressWarnings("unchecked")
    public void createAppleEventListener() {
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
            errorDialog("EAWT error", "Exception while loading the OSXAdapter:");
            e.printStackTrace();
        }
    }

    public Suite getCurrentSuite() {
        if (suites==null) return null;
        return suites.isEmpty() ? null : suites.get(currentSuiteIndex);
    }
    
    public Sample getCurrentSample() {
        Suite suite = getCurrentSuite();
        if (suite==null) return null;
        return suite.getCurrentSample();
    }
    
    // Only works for discrete, of course.
    public Sample[] getSelectedSamples() {
        return getMainWindow().sampleChooser.getSelectedSamples();
    }

    public void setCurrentSuite(int selectedIndex) {
        currentSuiteIndex = selectedIndex;
        if (getCurrentSuite() != null) getMainWindow().suitesChanged();
    }
    
    public void quit() {
        actions.quit.actionPerformed
        (new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
    }
    
    public void about() {
        actions.about.actionPerformed
        (new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
    }
    
    public void preferences() {
        actions.prefs.actionPerformed
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
}
