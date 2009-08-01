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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.print.PrintService;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;

public class PuffinActions {

    private PuffinApp app;
    private static final boolean useSwingChooserForOpen = true;
    private static final boolean useSwingChooserForSave = !PuffinApp.MAC_OS_X;
    // control or apple key as appropriate
    private static final int modifierKey =
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    PuffinActions(PuffinApp app) {
        this.app = app;
    }

    static abstract class PuffinAction extends AbstractAction {
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
    
    public final Action open = new PuffinAction("Open…",
            "Open a 2G, PPL, or ZPlot data file.", 'O', false,
            KeyEvent.VK_O) {

        public void actionPerformed(ActionEvent e) {

            List<File> files = null;
            if (useSwingChooserForOpen) {
                JFileChooser chooser = new JFileChooser();
                chooser.setMultiSelectionEnabled(true);
                chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                int choice = chooser.showOpenDialog(app.getMainWindow());
                if (choice == JFileChooser.APPROVE_OPTION)
                    files = Arrays.asList(chooser.getSelectedFiles());
            } else {
                FileDialog fd = new FileDialog(app.getMainWindow(), "Open file(s)",
                        FileDialog.LOAD);
                fd.setVisible(true);
                String filename = fd.getFile();
                if (filename != null) {
                    File file = new File(fd.getDirectory(), fd.getFile());
                    files = Collections.singletonList(file);
                }
            }
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
            chooser.setFileFilter(new FileFilter() {

                @Override
                public boolean accept(File f) {
                    return f.getName().toLowerCase().endsWith(extension);
                }

                @Override
                public String getDescription() {
                    return type;
                }
            });
            int choice = chooser.showSaveDialog(app.getMainWindow());
            if (choice == JFileChooser.APPROVE_OPTION)
                pathname = chooser.getSelectedFile().getPath();
        } else {
            FileDialog fd = new FileDialog(app.getMainWindow(), title,
                    FileDialog.SAVE);
            fd.setFilenameFilter(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(extension);
                }
            });
            fd.setVisible(true);
            if (fd.getFile() == null) { // "cancel" selected
                pathname = null;
            } else { // "save" selected
               pathname = new File(fd.getDirectory(), fd.getFile()).getPath();
            }
        }
        if (pathname != null && !pathname.toLowerCase().endsWith(extension))
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
                app.getSuite().saveCalcsSample(new File(pathname));
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
            for (Sample sample: app.getSelectedSamples())
                if (sample.getSelectedPoints().size()>1)
                    sample.doPca();
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

    public final Action fisher = new PuffinAction("Fisher on sample",
            "Calculate Fisher statistics for selected points", null, false,
            KeyEvent.VK_A) {
        public void actionPerformed(ActionEvent e) {
            Sample s = app.getSample();
            if (s == null) {
                app.errorDialog("Fisher on sample", "No sample selected.");
            } else {
                app.getSample().calculateFisher();
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

    public final Action mdf = new PuffinAction("MDF",
            "Calculate median destructive field (or temperature) on selected samples",
            'M', false, KeyEvent.VK_M) {
        public void actionPerformed(ActionEvent e) {
            for (Sample s: app.getSelectedSamples()) s.calculateMdf(app.isEmptyCorrectionActive());
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
    
    public final Action clear = new PuffinAction("Clear",
            "Clear selection and calculations for selected samples",
            'Z', false, KeyEvent.VK_C) {
        public void actionPerformed(ActionEvent e) {
            for (Sample s: app.getSelectedSamples()) s.clear();
            app.getMainWindow().repaint();
        }
    };
    
    public final Action selectAll = new PuffinAction("Select all",
            "Select all points in selected samples", 'D', false, KeyEvent.VK_A) {
        public void actionPerformed(ActionEvent e) {
            for (Sample s: app.getSelectedSamples()) s.selectAll();
                app.getMainWindow().repaint();
        }
    };

    public final Action copyPointSelection = new PuffinAction("Copy point selection",
            "Select these points for every selected sample", null, false, KeyEvent.VK_P) {
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
            JOptionPane.showMessageDialog(app.getMainWindow(), "This is the preferences dialog.");
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
}
