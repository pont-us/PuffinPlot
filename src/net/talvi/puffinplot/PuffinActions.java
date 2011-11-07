package net.talvi.puffinplot;

import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.PrintService;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import net.talvi.puffinplot.data.DatumField;
import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Site;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.Suite.AmsCalcType;

public class PuffinActions {

    private static final Logger logger = Logger.getLogger(PuffinActions.class.getName());
    private final PuffinApp app;
    private static final boolean useSwingChooserForOpen = true;
    private static final boolean useSwingChooserForSave = !PuffinApp.MAC_OS_X;
    // control or apple key as appropriate
    private static final int modifierKey =
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    PuffinActions(PuffinApp app) {
        this.app = app;
    }

    public static abstract class PuffinAction extends AbstractAction {
        private boolean specialMacMenuItem;

        public PuffinAction(String name, String description,
                Character accelerator, boolean shift, Integer mnemonic,
                boolean specialMacMenuItem) {
            super(name);
            this.specialMacMenuItem = specialMacMenuItem;
            putValue(SHORT_DESCRIPTION, description);
            if (accelerator != null) putValue(ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(accelerator,
                    modifierKey | (shift ? InputEvent.SHIFT_DOWN_MASK : 0),
                    false));
            if (mnemonic != null) putValue(MNEMONIC_KEY, mnemonic);
        }
        
        public PuffinAction(String name, String description,
                Character accelerator, boolean shift, Integer mnemonic) {
            this(name, description, accelerator, shift, mnemonic, false);
        }

        public boolean excludeFromMenu() {
            return PuffinApp.MAC_OS_X && specialMacMenuItem;
        }
    }
    
    public final Action about = new PuffinAction("About PuffinPlot",
            "Show information about this program",
            null, false, KeyEvent.VK_A) {

        public void actionPerformed(ActionEvent e) {
            app.getAboutBox().setLocationRelativeTo(app.getMainWindow());
            app.getAboutBox().setVisible(true);
        }
    };
    
    private List<File> openFileDialog(String title) {
        List<File> files = Collections.emptyList();
        if (useSwingChooserForOpen) {
            JFileChooser chooser = new JFileChooser();
            chooser.setMultiSelectionEnabled(true);
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int choice = chooser.showOpenDialog(app.getMainWindow());
            if (choice == JFileChooser.APPROVE_OPTION)
                files = Arrays.asList(chooser.getSelectedFiles());
        } else {
            FileDialog fd = new FileDialog(app.getMainWindow(), title,
                    FileDialog.LOAD);
            fd.setVisible(true);
            String filename = fd.getFile();
            if (filename != null) {
                File file = new File(fd.getDirectory(), fd.getFile());
                files = Collections.singletonList(file);
            }
        }
        return files;
    }
    
    public final Action open = new PuffinAction("Open…",
            "Open a 2G, PPL, or ZPlot data file.", 'O', false,
            KeyEvent.VK_O) {

        public void actionPerformed(ActionEvent e) {
            List<File> files = openFileDialog("Open file(s)");
            if (files != null) app.openFiles(files);
        }
    };

    public final Action close = new PuffinAction("Close",
            "Close this suite of data", 'W', false, KeyEvent.VK_C) {
        public void actionPerformed(ActionEvent e) {
            app.closeCurrentSuite();
        }
    };

    private String getSavePath(final String title, final String extension,
            final String type) {
        String pathname = null;
        if (useSwingChooserForSave) {
            JFileChooser chooser = new JFileChooser();
            if (extension != null && type != null) {
                chooser.setFileFilter(new FileFilter() {
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
        return pathname;
    }

    public final Action exportCalcsSample = new AbstractAction("Export sample calculations…") {
        public void actionPerformed(ActionEvent arg0) {
            if (app.getSuite() == null) {
                app.errorDialog("Error saving calculation", "No file loaded.");
                            return;
            }
            String pathname = getSavePath("Export sample calculations", ".csv",
                    "Comma Separated Values");
            if (pathname != null)
                app.getSuite().saveCalcsSample(new File(pathname),
                        app.getCorrection());
        }
    };

    public final Action exportCalcsSite = new AbstractAction("Export site calculations…") {

        public void actionPerformed(ActionEvent arg0) {
            if (app.getSuite() == null) {
                app.errorDialog("Error saving calculation", "No file loaded.");
                            return;
            }
            String pathname = getSavePath("Export site calculations", ".csv",
                    "Comma Separated Values");

            if (pathname != null)
                app.getSuite().saveCalcsSite(new File(pathname));
        }
    };

    public final Action exportCalcsSuite = new AbstractAction("Export suite calculations…") {

        public void actionPerformed(ActionEvent arg0) {
            if (app.getSuite() == null) {
                app.errorDialog("Error saving calculation", "No file loaded.");
                return;
            }
            String pathname = getSavePath("Export suite calculations", ".csv",
                    "Comma Separated Values");

            if (pathname != null)
                app.getSuite().saveCalcsSuite(new File(pathname));
        }
    };

    private void doSaveAs(Suite suite) {
        String pathname = getSavePath("Save data", ".ppl", "PuffinPlot data");
        if (pathname != null) suite.saveAs(new File(pathname));
    }

    public final Action save = new PuffinAction("Save",
            "Re-save the current file", 'S', false, KeyEvent.VK_A) {
        public void actionPerformed(ActionEvent e) {
            Suite suite = app.getSuite();
            if (suite != null) {
                if (suite.isFilenameSet()) suite.save();
                else doSaveAs(suite);
            }
        }
    };

    public final Action saveAs = new PuffinAction("Save as…",
            "Save this suite of data in a new file.", 'S', true, KeyEvent.VK_S) {
        public void actionPerformed(ActionEvent arg0) {
            Suite suite = app.getSuite();
            if (suite != null) doSaveAs(suite);
        }
    };
    
    public final Action pageSetup = new PuffinAction("Page Setup…",
            "Edit the page setup for printing", 'P', true, KeyEvent.VK_G) {
        public void actionPerformed(ActionEvent arg0) {
            app.showPageSetupDialog();
        }
    };
    
    public final Action flipSample = new AbstractAction("Flip sample(s)") {
        public void actionPerformed(ActionEvent arg0) {
            for (Sample s: app.getSelectedSamples()) s.flip();
        }
    };
    
    public final Action pcaOnSelection = new PuffinAction("PCA",
            "Perform PCA on selected points", 'R', false, KeyEvent.VK_P) {
        public void actionPerformed(ActionEvent e) {
            app.doPcaOnSelection();
            app.updateDisplay();
        }
    };

    public final Action useAsEmptySlot = new PuffinAction("Use as empty slot",
            "Use this sample as a control for machine noise", null, false,
            KeyEvent.VK_E) {
        public void actionPerformed(ActionEvent e) {
            for (Sample s : app.getSuite().getSamples()) s.setEmptySlot(false);
            app.getSample().setEmptySlot(true);
            app.updateDisplay();
        }
    };
   
    public final Action unsetEmptySlot = new AbstractAction("Unset empty slot") {
        public void actionPerformed(ActionEvent e) {
            for (Sample s : app.getSuite().getSamples()) s.setEmptySlot(false);
            app.updateDisplay();
        }
    };

    public final Action fisherBySample = new PuffinAction("Fisher by sample",
            "Calculate Fisher statistics for each individual sample", 'F', true,
            KeyEvent.VK_A) {
        public void actionPerformed(ActionEvent e) {
            List<Sample> samples = app.getSelectedSamples();
            if (samples == null || samples.isEmpty()) {
                app.errorDialog("Fisher on sample", "No sample selected.");
            } else {
                for (Sample s: samples) {
                    s.calculateFisher();
                }
                app.getMainWindow().repaint();
            }
        }
    };
    
    public final Action fisherBySite = new PuffinAction("Fisher by site",
            "Fisher statistics on PCA directions grouped by site", 'F', false,
            KeyEvent.VK_I) {
        public void actionPerformed(ActionEvent e) {
            Suite suite = app.getSuite();
            if (suite == null) {
                app.errorDialog("Fisher by site", "No suite loaded.");
            } else if (!suite.getMeasType().isDiscrete()) {
                app.errorDialog("Fisher by site", "Only discrete suites can have sites.");
            } else {
                suite.doFisherOnSites();
                app.getFisherWindow().getPlot().setGroupedBySite(true);
                app.getFisherWindow().setVisible(true);
            }
        }
    };
        
    public final Action fisherOnSuite = new PuffinAction("Fisher on suite",
            "Fisher statistics on PCA directions for entire selection",
            'F', true, KeyEvent.VK_U) {
        public void actionPerformed(ActionEvent e) {
            Suite suite = app.getSuite();
            if (suite == null) {
                app.errorDialog("Fisher on suite", "No suite loaded.");
            } else {
                suite.doFisherOnSuite();
                app.getFisherWindow().getPlot().setGroupedBySite(false);
                app.getFisherWindow().setVisible(true);
            }
        }
    };

    public final Action circleFit = new PuffinAction("Fit circle",
            "Fit great circle to selected points", 'L', false, KeyEvent.VK_C) {
        public void actionPerformed(ActionEvent e) {
        app.fitCircle();
        app.doGreatCircles(false);
        app.getMainWindow().repaint();
        }
    };

    public final Action greatCircleAnalysis = new PuffinAction("Great circles",
            "Great circle analysis for site", 'I', false, KeyEvent.VK_L) {
        public void actionPerformed(ActionEvent e) {
        app.doGreatCircles(true);
        }
    };

    public final Action clearSiteCalcs = new PuffinAction("Clear site calculations",
            "Clear site Fisher and Great Circle calculations", 'I', true, 0) {
        public void actionPerformed(ActionEvent e) {
        Site site = app.getCurrentSite();
        site.clearGcFit();
        site.clearFisher();
        app.updateDisplay();
        }
    };

    public final Action mdf = new PuffinAction("MDF",
            "Calculate median destructive field (or temperature) on selected samples",
            'M', false, KeyEvent.VK_M) {
        public void actionPerformed(ActionEvent e) {
            for (Sample s: app.getSelectedSamples()) s.calculateMdf(app.getCorrection());
            app.getMainWindow().repaint();
        }
    };

    public final Action editCorrections = new PuffinAction("Corrections…",
            "Edit corrections for sample, formation, and magnetic deviation",
            null, false, KeyEvent.VK_R) {
        public void actionPerformed(ActionEvent e) {
            app.getCorrectionWindow().setVisible(true);
        }
    };
    
    public final Action clear = new PuffinAction("Clear calculations",
            "Clear point selections and calculations for selected samples",
            'Z', false, KeyEvent.VK_C) {
        public void actionPerformed(ActionEvent e) {
            for (Sample s: app.getSelectedSamples()) s.clear();
            app.getMainWindow().repaint();
        }
    };
    
    public final Action selectAll = new PuffinAction("Select all",
            "Select all visible points in selected samples", 'D', false, KeyEvent.VK_A) {
        public void actionPerformed(ActionEvent e) {
            for (Sample s: app.getSelectedSamples()) s.selectVisible();
                app.getMainWindow().repaint();
        }
    };

    public final Action clearSelection = new PuffinAction("Clear selection",
            "Unselect all points in selected samples", 'D', true, KeyEvent.VK_L) {
        public void actionPerformed(ActionEvent e) {
            for (Sample s: app.getSelectedSamples()) s.selectNone();
                app.getMainWindow().repaint();
        }
    };

    public final Action copyPointSelection = new PuffinAction("Copy point selection",
            "Select these points for every selected sample", 'C', true, KeyEvent.VK_P) {
        public void actionPerformed(ActionEvent e) {
            Sample source = app.getSample();
            if (source != null) {
                for (Sample dest: app.getSelectedSamples()) {
                    dest.copySelectionFrom(source);
                }
                app.getMainWindow().repaint();
            }
        }
    };

    // we can't use ctrl-H because Apples use it already.
    public final Action hideSelectedPoints = new PuffinAction("Hide points",
            "Hide the selected points in all selected samples", 'G', false, KeyEvent.VK_H) {
        public void actionPerformed(ActionEvent e) {
           for (Sample s: app.getSelectedSamples())  s.hideSelectedPoints();
            app.getMainWindow().repaint();
        }
    };

    public final Action unhideAllPoints = new PuffinAction("Show all points",
            "Make hidden points visible again for all selected samples", 'G', true, KeyEvent.VK_O) {
        public void actionPerformed(ActionEvent e) {
            for (Sample s : app.getSelectedSamples()) s.unhideAllPoints();
            app.getMainWindow().repaint();
        }
    };


    public final Action prefs = new PuffinAction("Preferences…",
            "Show the preferences window", ',', false, KeyEvent.VK_R, true) {
        public void actionPerformed(ActionEvent e) {
            app.showPreferences();
        }
    };
    
    public final Action print = new PuffinAction("Print…",
            "Print the selected samples", 'P', false, KeyEvent.VK_E) {

        public void actionPerformed(ActionEvent e) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(app.getMainWindow().getGraphDisplay(),
                             app.getCurrentPageFormat());

            PrintService[] services = PrinterJob.lookupPrintServices();

            if (services.length > 0) {
                try {
                    job.setPrintService(services[0]);
                    /* Note: if we pass an attribute set to printDialog(),
                     * it forces the use of a cross-platform Swing print
                     * dialog rather than the default native one.
                    */
                    if (job.printDialog()) job.print();
                } catch (PrinterException pe) {
                    System.err.println(pe);
                }
            }
        }
    };
    
    public final Action printFisher = new PuffinAction("Print Fisher…",
            "Print the Fisher statistics plot", null, false, KeyEvent.VK_F) {

        public void actionPerformed(ActionEvent e) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable((Printable) app.getFisherWindow().getContentPane(),
                    app.getCurrentPageFormat());

            PrintService[] services = PrinterJob.lookupPrintServices();

            if (services.length > 0) {
                try {
                    job.setPrintService(services[0]);
                    /* Note: if we pass an attribute set to printDialog(),
                     * it forces the use of a cross-platform Swing print
                     * dialog rather than the default native one.
                    */
                    if (job.printDialog()) job.print();
                } catch (PrinterException pe) {
                    System.err.println(pe);
                }
            }
        }
    };
    
    public final Action quit = new PuffinAction("Quit",
            null, 'Q', false, KeyEvent.VK_Q, true) {
        public void actionPerformed(ActionEvent e) {
            app.getPrefs().save();
            System.exit(0);
        }
    };

    public final Action resetLayout = new PuffinAction("Reset layout",
            "Move plots back to their original positions", null, false,
            KeyEvent.VK_L) {
        public void actionPerformed(ActionEvent e) {
            app.getMainWindow().getGraphDisplay().resetLayout();
        }
    };

    public final Action importAms = new PuffinAction("Import AMS…",
            "Import AMS data", null, false,
            KeyEvent.VK_L) {
        public void actionPerformed(ActionEvent e) {
            try {
                List<File> files = openFileDialog("Select AMS files");
                //app.getSuite().importAms(files, true);
                app.getSuite().importAmsFromAsc(files, false);
                app.updateDisplay();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
                app.errorDialog("Error importing AMS", ex.getLocalizedMessage());
            }
        }
    };

    public final Action printGc = new PuffinAction("Print Great Circles…",
            "Print Great Circles", null, false,
            KeyEvent.VK_N) {
        public void actionPerformed(ActionEvent e) {

            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable((Printable) app.getGreatCircleWindow().getContentPane(),
                    app.getCurrentPageFormat());

            PrintService[] services = PrinterJob.lookupPrintServices();

            if (services.length > 0) {
                try {
                    job.setPrintService(services[0]);
                    /* Note: if we pass an attribute set to printDialog(),
                     * it forces the use of a cross-platform Swing print
                     * dialog rather than the default native one.
                    */
                    if (job.printDialog()) job.print();
                } catch (PrinterException pe) {
                    System.err.println(pe);
                }
            }
        }
    };

    public final Action reversalTest = new PuffinAction("Reversal test",
            "Perform reversal test on all loaded suites", null, false,
            KeyEvent.VK_V) {
        public void actionPerformed(ActionEvent e) {
            List<FisherValues> fv = Suite.doReversalTest(app.getSuites());
            System.out.println(fv.get(0));
            System.out.println(fv.get(1));
                    JOptionPane.showMessageDialog
        (app.getMainWindow(), new JTextArea("Normal " +fv.get(0).toString() +
                            "\nReversed: "+fv.get(1).toString()),
                            "Reversals test", JOptionPane.INFORMATION_MESSAGE);
        }
    };

    public final Action exportIrm = new PuffinAction("Export IRM data…",
            "Export IRM field/remanence for this suite", null, false, 0) {
        public void actionPerformed(ActionEvent e) {
            if (app.getSuite() == null) return;
            String pathname = getSavePath("Export IRM data", null,
                    null);
            app.getSuite().exportToFiles(new File(pathname),
                    Arrays.asList(new DatumField[] {DatumField.IRM_FIELD,
                    DatumField.VIRT_MAGNETIZATION}));
        }
    };

    public final Action showCustomFlagsWindow = new PuffinAction("Edit custom flags…",
            "Edit user-defined flags for samples",
            null, false, 0) {
        public void actionPerformed(ActionEvent e) {
           app.showCustomFlagsWindow();
        }
    };

    public final Action showCustomNotesWindow = new PuffinAction("Edit custom notes…",
            "Edit user-defined notes for samples",
            null, false, 0) {
        public void actionPerformed(ActionEvent e) {
           app.showCustomNotesWindow();
        }
    };

    public final Action exportSvg = new PuffinAction("Export SVG…",
            "Save current display to an SVG file",
            '9', false, 0) {
        public void actionPerformed(ActionEvent e) {
            String pathname = getSavePath("Export preferences", ".svg",
                    "Scalable Vector Graphics");
            if (pathname != null)
                app.getMainWindow().getGraphDisplay().printToSvg(pathname);
        }
    };

    public final Action bootAmsNaive = new PuffinAction("Calculate bootstrap AMS",
            "Calculate bootstrap statistics for AMS data of selected samples",
            null, false, 0) {
        public void actionPerformed(ActionEvent e) {
            app.doAmsCalc(AmsCalcType.BOOT, "bootams.py");
        }
    };

    public final Action bootAmsParam = new PuffinAction("Parametric bootstrap AMS",
            "Calculate parametric bootstrap statistics for AMS data of selected samples",
            null, false, 0) {
        public void actionPerformed(ActionEvent e) {
            app.doAmsCalc(AmsCalcType.PARA_BOOT, "bootams.py");
        }
    };

    public final Action hextAms = new PuffinAction("Calculate Hext on AMS",
            "Calculate Hext statistics for AMS data of selected samples",
            null, false, 0) {
        public void actionPerformed(ActionEvent e) {
            app.doAmsCalc(AmsCalcType.HEXT, "s_hext.py");
        }
    };

    public final Action rescaleMagSus = new PuffinAction("Rescale mag. sus. …",
            "Scale magnetic susceptibility by a constant factor (whole suite)",
            null, false, 0) {
        public void actionPerformed(ActionEvent e) {
            final String factorString = (String)JOptionPane.showInputDialog(
                    app.getMainWindow(),
                    "Please enter magnetic susceptibility scaling factor.");
            // my empirically determined value for the Bartington is 4.3e-5.
            try {
                final double factor = Double.parseDouble(factorString);
                app.getSuite().rescaleMagSus(factor);
            } catch (NumberFormatException exception) {
                app.errorDialog("Input error", "That didn't look like a number.");
            }
        }
    };

    public final Action exportPrefs = new AbstractAction("Export preferences…") {
        public void actionPerformed(ActionEvent arg0) {
            String pathname = getSavePath("Export preferences", ".xml",
                    "eXtensible Markup Language");
            if (pathname != null)
                app.getPrefs().exportToFile(new File(pathname));
        }
    };

    public final Action importPrefs = new AbstractAction("Import preferences…") {
        public void actionPerformed(ActionEvent arg0) {
            List<File> files = openFileDialog("Import preferences file");
            if (files != null && files.size() > 0) {
                app.getPrefs().importFromFile(files.get(0));
                app.getMainWindow().getGraphDisplay().recreatePlots();
                app.updateDisplay();
            }
        }
    };

    public final Action clearAmsCalcs = new AbstractAction("Clear AMS calculations") {
        public void actionPerformed(ActionEvent arg0) {
            app.getSuite().clearAmsCalculations();
            app.updateDisplay();
        }
    };
}
