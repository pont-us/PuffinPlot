package net.talvi.puffinplot;

// import groovy.ui.Console;

import java.awt.event.ActionEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Sample;

public class PuffinApp {

    public static PuffinApp app;
    final PuffinActions actions;
    private List<FileOpenedListener> fileOpenedListeners =
        new LinkedList<FileOpenedListener>();
    List<Suite> suites;
    public MainWindow mainWindow;
    private int currentSuiteIndex;
    private PageFormat currentPageFormat = PrinterJob.getPrinterJob().defaultPage();

    public static boolean MAC_OS_X = (System.getProperty("os.name").
            toLowerCase().startsWith("mac os x"));
    private TableWindow tableWindow;
    private Prefs prefs;

    public Prefs getPrefs() {
        return prefs;
    }
    
    public class Prefs {
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

        // currentSample = new ArrayList<Datum>();
        suites = new ArrayList<Suite>();
        if (MAC_OS_X) createAppleEventListener();
        // openFiles(new File("/home/pont/work/analysis/puffinplot/zplot.txt"));
        // openFiles(new File("/home/pont/TAN051319A.DAT"));
        mainWindow.setVisible(true);
        
//         Console console = new Console();
//        console.run();

                //        try {
//            SwingUtilities.invokeAndWait(new Runnable() {
//                public void run() {
//                    System.setProperty("sun.awt.exception.handler",
//                            PuffinExceptionHandler.class.getName());
//                }
//            });
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }

    public static void main(String[] args) {
        new PuffinApp();
    }
    
    public Correction currentCorrection() {
        return mainWindow.controlPanel.getCorrection();
    }
    
    public void openFiles(File f) {
        openFiles(new File[] {f});
    }

    public void openFiles(File[] files) {
        if (files.length == 0) return;

        try {
            Suite suite;
            suites.add(suite = new Suite(files));
            if (suites.size() == 1) {
                currentSuiteIndex = 0; // FIXME
                mainWindow.suiteChanged(getCurrentSuite());                
            }
            for (FileOpenedListener fol: fileOpenedListeners) fol.fileOpened();
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
        } catch (FileNotFoundException e) {
            errorDialog("File not found", e.getMessage());
        } catch (IOException e) {
            errorDialog("Error reading file", e.getMessage());
        }

    }
    
    public void errorDialog(String title, String message) {
        JOptionPane.showMessageDialog
        (app.mainWindow, message, title, JOptionPane.ERROR_MESSAGE);
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
        return mainWindow.sampleChooser.getSelectedSamples();
    }
    
    public void addFileOpenedListener(FileOpenedListener fol) {
        fileOpenedListeners.add(fol);
    }
    
    public void removeFileOpenedListener(FileOpenedListener fol) {
        fileOpenedListeners.remove(fol);
    }

    public void setCurrentSuite(int selectedIndex) {
        currentSuiteIndex = selectedIndex;
        if (getCurrentSuite() != null) mainWindow.suiteChanged(getCurrentSuite());
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
