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
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.PrintService;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import net.talvi.puffinplot.data.DatumField;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.Suite.AmsCalcType;

/**
 * A container class for individual instances of {@link PuffinAction}
 * which represent particular user actions in PuffinPlot.
 * 
 * @author pont
 */
public class PuffinActions {

    private final PuffinApp app;
    private final boolean useSwingChooserForSave;
    // control or apple key as appropriate

    /**
     * Instantiates a new set of actions which will be performed on the
     * specified PuffinPlot application.
     * 
     * @param app the PuffinPlot instance to which to apply the actions
     * when they are performed
     */
    PuffinActions(PuffinApp app) {
        this.app = app;
        useSwingChooserForSave = !app.isOnOsX();
    }
    
    /**
     * Opens the application's ‘About’ dialog box.
     */
    public final Action about = new PuffinAction("About PuffinPlot",
            "Show information about this program",
            null, false, KeyEvent.VK_A) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.about();
        }
    };

    /**
     * Opens a dialog box allowing the user to choose one or more files
     * to open as a new data suite.
     */
    public final Action open = new PuffinAction("Open…",
            "Open a 2G, PPL, or ZPlot data file.", 'O', false,
            KeyEvent.VK_O) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.openFilesWithDialog();
        }
    };

    /**
     * Closes the current data suite.
     */
    public final Action close = new PuffinAction("Close",
            "Close this suite of data", 'W', false, KeyEvent.VK_C) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.closeCurrentSuite();
        }
    };

    String getSavePath(final String title, final String extension,
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

    /**
     * Opens a ‘Save’ dialog box; sample calculations are saved to 
     * the chosen file (if any).
     */
    public final Action exportCalcsSample =
            new AbstractAction("Export sample calculations…") {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent event) {
            if (app.getSuite() == null) {
                app.errorDialog("Error saving calculations", "No file loaded.");
                            return;
            }
            final String pathname = getSavePath("Export sample calculations",
                    ".csv", "Comma Separated Values");
            if (pathname != null) {
                try {
                    app.getSuite().saveCalcsSample(new File(pathname));
                } catch (PuffinUserException ex) {
                    app.errorDialog("Error saving calculations", ex);
                }
            }
        }
    };

    /**
     * Opens a ‘Save’ dialog box; site calculations are saved to 
     * the chosen file (if any).
     */
    public final Action exportCalcsSite = new AbstractAction("Export site calculations…") {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent arg0) {
            if (app.getSuite() == null) {
                app.errorDialog("Error saving calculation", "No file loaded.");
                            return;
            }
            String pathname = getSavePath("Export site calculations", ".csv",
                    "Comma Separated Values");

            if (pathname != null) {
                try {
                    app.getSuite().saveCalcsSite(new File(pathname));
                } catch (PuffinUserException ex) {
                    app.errorDialog("Error saving calculations", ex);
                }
            }
        }
    };

    /**
     * Opens a ‘Save’ dialog box; suite calculations are saved to 
     * the chosen file (if any).
     */
    public final Action exportCalcsSuite = new AbstractAction("Export suite calculations…") {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent arg0) {
            if (app.getSuite() == null) {
                app.errorDialog("Error saving calculation", "No file loaded.");
                return;
            }
            String pathname = getSavePath("Export suite calculations", ".csv",
                    "Comma Separated Values");

            if (pathname != null) {
                try {
                    app.getSuite().saveCalcsSuite(new File(pathname));
                } catch (PuffinUserException ex) {
                    app.errorDialog("Error saving calculations",
                            ex.getLocalizedMessage());
                }
            }
        }
    };

        /**
     * Opens a ‘Save’ dialog box; suite calculations are saved to 
     * the chosen file (if any).
     */
    public final Action exportCalcsMultiSuite = new AbstractAction("Export multi-suite calculations…") {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent arg0) {
            app.exportCalcsMultiSuite();
        }
    };
    
    private void doSaveAs(Suite suite) {
        String pathname = getSavePath("Save data", ".ppl", "PuffinPlot data");
        if (pathname != null) try {
            final File file = new File(pathname);
            suite.saveAs(file);
            app.getRecentFiles().add(Collections.singletonList(file));
            app.getMainWindow().getMainMenuBar().updateRecentFiles();
        } catch (PuffinUserException ex) {
            app.errorDialog("Error saving file", ex);
        }
    }

    /**
     * If a PuffinPlot file is associated with the current suite,
     * the suite is saved to that file. If not, this action is 
     * equivalent to {@link #saveAs}.
     */
    public final Action save = new PuffinAction("Save",
            "Re-save the current file", 'S', false, KeyEvent.VK_A) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            Suite suite = app.getSuite();
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
    };

    /**
     * Opens a ‘save’ dialog box; the current suite is saved to the 
     * selected file in PuffinPlot format.
     */
    public final Action saveAs = new PuffinAction("Save as…",
            "Save this suite of data in a new file.", 'S', true, KeyEvent.VK_S) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent arg0) {
            Suite suite = app.getSuite();
            if (suite != null) doSaveAs(suite);
        }
    };
    
    /**
     * Opens a dialog box allowing the user to change the page setup 
     * for printing.
     */
    public final Action pageSetup = new PuffinAction("Page Setup…",
            "Edit the page setup for printing", 'P', true, KeyEvent.VK_G) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent arg0) {
            app.showPageSetupDialog();
        }
    };
    
    /**
     * Rotates the currently selected samples 180° about the X axis.
     */
    public final Action flipSampleX = new PuffinAction("Flip samples around X axis",
            "Rotate selected samples 180° about the X axis") {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent arg0) {
            app.flipSelectedSamples(MeasurementAxis.X);
        }
    };
    
    /**
     * Rotates the currently selected samples 180° about the Y axis.
     */
    public final Action flipSampleY = new PuffinAction("Flip samples around Y axis",
            "Rotate selected samples 180° about the Y axis") {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent arg0) {
            app.flipSelectedSamples(MeasurementAxis.Y);
        }
    };
    
    /**
     * Rotates the currently selected samples 180° about the Z axis.
     */
    public final Action flipSampleZ = new PuffinAction("Flip samples around Z axis",
            "Rotate selected samples 180° about the Z axis") {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent arg0) {
            app.flipSelectedSamples(MeasurementAxis.Z);
        }
    };
    
    /**
     * Performs principal component analysis on the selected points of the
     * selected samples.
     */
    public final Action pcaOnSelection = new PuffinAction("PCA",
            "Perform principal component analysis on selected points", 'R', false, KeyEvent.VK_P) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.doPcaOnSelection();
        }
    };

    /**
     * Flags the current sample as an empty slot to be used as a control
     * for machine noise (not currently implemented).
     */
    public final Action useAsEmptySlot = new PuffinAction("Use as empty slot",
            "Use this sample as a control for machine noise", null, false,
            KeyEvent.VK_E) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            for (Sample s : app.getSuite().getSamples()) s.setEmptySlot(false);
            app.getSample().setEmptySlot(true);
            app.updateDisplay();
        }
    };
   
    /**
     * Unflags any samples previously flagged as empty slots.
     * 
     * @see #useAsEmptySlot
     */
    public final Action unsetEmptySlot = new AbstractAction("Unset empty slot") {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            for (Sample s : app.getSuite().getSamples()) s.setEmptySlot(false);
            app.updateDisplay();
        }
    };

    /**
     * Calculates Fisherian statistics on PCA directions from the current site.
     */
    public final Action fisherBySite = new PuffinAction("Fisher by site",
            "Fisher statistics on PCA directions grouped by site", 'F', false,
            KeyEvent.VK_I) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            final Suite suite = app.getSuite();
            if (suite == null) {
                app.errorDialog("Fisher by site", "No suite loaded.");
            } else if (!suite.getMeasType().isDiscrete()) {
                app.errorDialog("Fisher by site", "Only discrete suites can have sites.");
            } else {
                suite.calculateSiteFishers(app.getCorrection());
                app.getSuiteEqAreaWindow().getPlot().setGroupedBySite(true);
                // app.getFisherWindow().setVisible(true);
            }
        }
    };
    
    /**
     * Calculates Fisher statistics on sample PCA directions for all
     * selected samples, and on site means for all selected sites.
     */
    public final Action suiteMeans = new PuffinAction("Suite means",
            "Calculate mean directions using all selected sites and samples",
            'F', true, KeyEvent.VK_U) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            Suite suite = app.getSuite();
            if (suite == null) {
                app.errorDialog("Calculate suite means", "No suite loaded.");
            } else {
                suite.calculateSuiteMeans();
                app.getSuiteEqAreaWindow().getPlot().setGroupedBySite(false);
                //app.getFisherWindow().setVisible(true);
            }
        }
    };

    /**
     * For each selected sample, fits a great circle to the selected points.
     */
    public final Action circleFit = new PuffinAction("Fit great circle",
            "Fit great circle to selected points", 'L', false, KeyEvent.VK_C) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.fitGreatCirclesToSelection();
            app.calculateGreatCirclesDirections(false);
            app.updateDisplay();
        }
    };

    /**
     * Determines a site mean through McFadden and McElhinny great-circle
     * intersection.
     */
    public final Action greatCircleAnalysis = new PuffinAction("Great circles",
            "Great circle analysis for site", 'I', false, KeyEvent.VK_L) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
        app.calculateGreatCirclesDirections(false);
        }
    };

    /**
     * Clears any previously calculated Fisherian or great-circle site directions.
     */
    public final Action clearSiteCalcs = new PuffinAction("Clear site calculations",
            "Clear site Fisher and Great Circle calculations", 'I', true, 0) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.clearSiteCalculations();
        }
    };

    /**
     * Calculates the median destructive field or temperature of the selected
     * samples.
     */
    public final Action mdf = new PuffinAction("MDF",
            "Calculate median destructive field (or temperature) on selected samples",
            'M', false, KeyEvent.VK_M) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            for (Sample s: app.getSelectedSamples()) s.calculateMdf();
            app.getMainWindow().repaint();
        }
    };

    /**
     * Opens a window allowing the user to edit the sample and formation
     * orientations and the local magnetic declination.
     */
    public final Action editCorrections = new PuffinAction("Corrections…",
            "Edit sample and formation orientations, and local geomagnetic field declination",
            null, false, KeyEvent.VK_R) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.getCorrectionWindow().setVisible(true);
        }
    };
    
    /**
     * For each selected sample, clears all calculations and deselects all points.
     */
    public final Action clearSampleCalcs = new PuffinAction("Clear sample calculations",
            "Clear point selections and calculations for selected samples",
            'Z', false, KeyEvent.VK_C) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            for (Sample s: app.getSelectedSamples()) s.clearCalculations();
            app.getMainWindow().repaint();
        }
    };
    
    /**
     * Within each selected sample, selects all the points.
     */
    public final Action selectAll = new PuffinAction("Select all",
            "Select all visible points in selected samples", 'D', false, KeyEvent.VK_A) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            for (Sample s: app.getSelectedSamples()) s.selectVisible();
                app.getMainWindow().repaint();
        }
    };

    /**
     * For each selected sample, deselects all the points.
     */
    public final Action clearSelection = new PuffinAction("Clear selection",
            "Unselect all points in selected samples", 'D', true, KeyEvent.VK_L) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            for (Sample s: app.getSelectedSamples()) s.selectNone();
                app.getMainWindow().repaint();
        }
    };

    /**
     * Copies the range of selected points from the current sample onto
     * a clipboard.
     */
    public final Action copyPointSelection =
            new PuffinAction("Copy point selection",
            "Copy the point selection to the clipboard",
                    'J', false, KeyEvent.VK_C) {
                private static final long serialVersionUID = 1L;
                public void actionPerformed(ActionEvent e) {
                    app.copyPointSelection(); }};
    
    /**
     * For each selected sample, sets the selected points using the 
     * range currently copied to the clipboard.
     */
    public final Action pastePointSelection =
            new PuffinAction("Paste point selection",
            "Select the points corresponding to those copied to the clipboard",
                    'K', false, KeyEvent.VK_P) {
        private static final long serialVersionUID = 1L;
                public void actionPerformed(ActionEvent e) {
                    app.pastePointSelection(); }};

    /**
     * For each selected sample, makes the selected points invisible.
     */
    // we can't use ctrl-H because Apples use it already.
    public final Action hideSelectedPoints = new PuffinAction("Hide points",
            "Hide the selected points", 'G', false, KeyEvent.VK_H) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
           for (Sample s: app.getSelectedSamples())  s.hideSelectedPoints();
            app.getMainWindow().repaint();
        }
    };

    /**
     * For each selected sample, makes all the points visible.
     */
    public final Action unhideAllPoints = new PuffinAction("Show all points",
            "Make hidden points visible again for all selected samples", 'G',
            true, KeyEvent.VK_O) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            for (Sample s : app.getSelectedSamples()) s.unhideAllPoints();
            app.getMainWindow().repaint();
        }
    };

    /**
     * Opens the preferences window.
     */
    public final Action prefs = new PuffinAction("Preferences…",
            "Show the preferences window", ',', false, KeyEvent.VK_R, true,
            PuffinAction.modifierKey) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.showPreferences();
        }
    };
    
    /**
     * Opens a printing dialog box allowing printing of the main window
     * data display.
     */
    public final Action print = new PuffinAction("Print…",
            "Print the selected samples", 'P', false, KeyEvent.VK_E) {
        private static final long serialVersionUID = 1L;
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
                     * dialog rather than the default native one. */
                    if (job.printDialog()) job.print();
                } catch (PrinterException pe) {
                    System.err.println(pe);
                }
            }
        }
    };
    
    /**
     * Opens a print dialog for the site equal-area plot window.
     */
    public final Action printGc = new PuffinAction("Print site EA window…",
            "Print the contents of the suite equal-area plot window",
            null, false, KeyEvent.VK_N) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable((Printable) app.getSiteEqAreaWindow().getContentPane(),
                    app.getCurrentPageFormat());
            PrintService[] services = PrinterJob.lookupPrintServices();
            if (services.length > 0) {
                try {
                    job.setPrintService(services[0]);
                    /* Note: if we pass an attribute set to printDialog(),
                     * it forces the use of a cross-platform Swing print
                     * dialog rather than the default native one. */
                    if (job.printDialog()) job.print();
                } catch (PrinterException pe) {
                    System.err.println(pe);
                }
            }
        }
    };
    
    /**
     * Opens a printing dialog box allowing printing of the suite equal-area 
     * data display.
     */
    public final Action printSuiteEqArea = new PuffinAction("Print suite EA window…",
            "Print the contents of the suite equal-area plot window",
            null, false, KeyEvent.VK_F) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable((Printable) app.getSuiteEqAreaWindow().getContentPane(),
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
    
    /**
     * Terminates the application immediately.
     */
    public final Action quit = new PuffinAction("Quit",
            null, 'Q', false, KeyEvent.VK_Q, true, PuffinAction.modifierKey) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.quit();
        }
    };

    /**
     * Resets the layout of the plots to the default.
     */
    public final Action resetLayout = new PuffinAction("Reset layout",
            "Move plots back to their original positions", null, false,
            KeyEvent.VK_L) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.getMainWindow().getGraphDisplay().resetLayout();
        }
    };

    /**
     * Opens a file dialog allowing the user to choose an Agico ASC file
     * from which to import AMS data.
     */
    public final Action importAms = new PuffinAction("Import AMS…",
            "Import AMS data from Agico ASC file", null, false, KeyEvent.VK_L) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.importAmsWithDialog();
        }
    };

    /**
     * Calculate mean directions for data in all the currently open data suites.
     */
    public final Action multiSuiteMeans = new PuffinAction("Multi-suite means",
            "Calculate means for data in all open suites", null, false,
            KeyEvent.VK_V) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.calculateMultiSuiteMeans();
        }
    };

    /**
     * Opens a save dialog allowing the export of the current suite's
     * IRM data as a tab-delimited text file.
     */
    public final Action exportIrm = new PuffinAction("Export IRM data…",
            "Export IRM field/remanence for this suite", null, false, 0) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            if (app.getSuite() == null) return;
            String pathname = getSavePath("Export IRM data", null,
                    null);
            app.getSuite().exportToFiles(new File(pathname),
                    Arrays.asList(new DatumField[] {DatumField.IRM_FIELD,
                    DatumField.VIRT_MAGNETIZATION}));
        }
    };

    /**
     * Opens a window which allows the user to edit the list of user-defined
     * flags for the current suite.
     */
    public final Action showCustomFlagsWindow = new PuffinAction("Edit custom flags…",
            "Edit user-defined flags for samples",
            null, false, 0) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
           app.showCustomFlagsWindow();
        }
    };

    /**
     * Opens a window which allows the user to edit the list of user-defined
     * note headings for the current suite.
     */
    public final Action showCustomNotesWindow = new PuffinAction("Edit custom notes…",
            "Edit user-defined notes for samples",
            null, false, 0) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
           app.showCustomNotesWindow();
        }
    };

    /**
     * Opens a save dialog allowing the current main display to be saved
     * as an SVG file using the Batik library.
     */
    public final Action exportSvgBatik = new PuffinAction("Export SVG (Batik)…",
            "Save current display to an SVG file, using the Batik graphics library",
            '6', false, 0) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            String pathname = getSavePath("Export to SVG (Batik)", ".svg",
                    "Scalable Vector Graphics");
            if (pathname != null)
                app.getMainWindow().getGraphDisplay().saveToSvgBatik(pathname);
        }
    };
    
    /**
     * Opens a save dialog allowing the current main display to be saved
     * as an SVG file using the FreeHEP library.
     */
    public final Action exportSvgFreehep = new PuffinAction("Export SVG (FreeHEP)…",
            "Save current display to an SVG file, using the FreeHEP graphics library",
            '7', false, 0) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            String pathname = getSavePath("Export to SVG (FreeHEP)", ".svg",
                    "Scalable Vector Graphics");
            if (pathname != null) {
                try {
                app.getMainWindow().getGraphDisplay().saveToSvgFreehep(pathname);
                } catch (IOException ex) {
                    app.errorDialog("Error exporting SVG", ex.getLocalizedMessage());
                }
            }
        }
    };

    /**
     * Opens a save dialog allowing the current main display to be saved
     * as a PDF file.
     */
    public final Action exportPdfItext = new PuffinAction("Export PDF (iText)…",
            "Print data for selected samples to a PDF file, using the iText graphics library",
            '8', false, 0) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            String pathname = getSavePath("Export to PDF", ".pdf",
                    "Portable Document Format");
            if (pathname != null)
                try {
                app.exportPdfItext(new File(pathname));
            } catch (DocumentException ex) {
                Logger.getLogger(PuffinActions.class.getName()).log(Level.SEVERE, null, ex);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(PuffinActions.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    };
    
    /**
     * Opens a save dialog allowing the current main display to be saved
     * as a PDF file.
     */
    public final Action exportPdfFreehep = new PuffinAction("Export PDF (FreeHEP)…",
            "Print data for selected samples to a PDF file, using the FreeHEP graphics library",
            '9', false, 0) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            String pathname = getSavePath("Export to PDF", ".pdf",
                    "Portable Document Format");
            if (pathname != null)
                try {
                app.exportPdfFreehep(new File(pathname));
            } catch (IOException ex) {
                Logger.getLogger(PuffinActions.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    };
    
    /**
     * Calculates bootstrap AMS statistics on the selected samples.
     */
    public final Action bootAmsNaive = new PuffinAction("Calculate bootstrap AMS",
            "Calculate bootstrap statistics for AMS data of selected samples",
            null, false, 0) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.doAmsCalc(AmsCalcType.BOOT, "bootams.py");
        }
    };

    /**
     * Calculates parametric bootstrap AMS statistics on the selected samples.
     */
    public final Action bootAmsParam = new PuffinAction("Parametric bootstrap AMS",
            "Calculate parametric bootstrap statistics for AMS data of selected samples",
            null, false, 0) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.doAmsCalc(AmsCalcType.PARA_BOOT, "bootams.py");
        }
    };

    /**
     * Calculates Hext AMS statistics on the selected samples.
     */
    public final Action hextAms = new PuffinAction("Calculate Hext on AMS",
            "Calculate Hext statistics for AMS data of selected samples",
            null, false, 0) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.doAmsCalc(AmsCalcType.HEXT, "s_hext.py");
        }
    };

    /**
     * Scales all magnetic susceptibility values in the current suite by
     * a user-specified factor.
     */
    public final Action rescaleMagSus = new PuffinAction("Rescale mag. sus. …",
            "Scale magnetic susceptibility by a constant factor (whole suite)",
            null, false, 0) {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.rescaleMagSus();
        }
    };

    /**
     * Exports the current user preferences to an XML file.
     */
    public final Action exportPrefs = new AbstractAction("Export preferences…") {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent arg0) {
            String pathname = getSavePath("Export preferences", ".xml",
                    "eXtensible Markup Language");
            if (pathname != null)
                app.getPrefs().exportToFile(new File(pathname));
        }
    };

    /**
     * Imports user preferences from an XML file.
     */
    public final Action importPrefs = new AbstractAction("Import preferences…") {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent arg0) {
            app.importPreferencesWithDialog();
        }
    };

    /**
     * Clears AMS calculations for the current suite.
     */
    public final Action clearAmsCalcs = new AbstractAction("Clear AMS calculations") {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent arg0) {
            app.clearAmsCalcs();
        }
    };
    
    /**
     * Opens a dialog box allowing the user to specify a site name
     * for the selected samples.
     */
    public final Action setSiteName = new AbstractAction("Set site name…") {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            final String name = JOptionPane.showInputDialog("Site name");
            if (name==null || "".equals(name)) return;
            app.getSuite().setNamedSiteForSamples(app.getSelectedSamples(), name);
        }
    };
    
    /**
     * Opens a dialog box allowing the user to specify which characters of
     * the currently selected samples should be used to determine the site
     * name.
     */
    public final Action setSitesFromSampleNames = new AbstractAction("Set sites from sample names…") {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            final String maskSpec = JOptionPane.showInputDialog("Character positions to use");
            if (maskSpec==null || "".equals(maskSpec)) return;
            app.getSuite().setSiteNamesBySubstring(app.getSelectedSamples(),
                    Util.numberRangeStringToBitSet(maskSpec, 256));
        }
    };
    
    /**
     * Opens a dialog box allowing the user to specify a site thickness, which
     * is then used to divide a long core suite into sites based on sample depths.
     */
    public final Action setSitesByDepth = new AbstractAction("Set sites by depth…") {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            final String thicknessString = JOptionPane.showInputDialog("Thickness of depth slices");
            if (thicknessString==null || "".equals(thicknessString)) return;
            float thickness = Float.NaN;
            try {
                thickness = Float.parseFloat(thicknessString);
            } catch (NumberFormatException ex) {
                app.errorDialog("Invalid number", thicknessString+" is not a number.");
            }
            if (Float.isNaN(thickness)) return;
            app.getSuite().setSiteNamesByDepth(app.getSelectedSamples(),
                    thickness);
        }
    };
    
    /**
     * Clears the current user preferences, resetting them to their default values.
     */
    public final Action clearPreferences = new AbstractAction("Clear preferences") {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.clearPreferences();
        }
    };

    /**
     * Runs a Python script using the Jython interpreter.
     */
    public final Action runScript = new AbstractAction("Run Python script…") {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.runPythonScriptWithDialog();
        }
    };
    
    /**
     * Imports data from a tabular file.
     */
    public final Action importTabularData = new AbstractAction("Import data…") {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            app.showTabularImportDialog();
        }
    };
}
