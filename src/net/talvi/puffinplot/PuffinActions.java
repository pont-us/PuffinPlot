/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.itextpdf.text.DocumentException;
import net.talvi.puffinplot.data.AmsCalculationType;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.TreatmentStepField;
import net.talvi.puffinplot.window.AlignDeclinationsDialog;
import net.talvi.puffinplot.window.CiteWindow;
import net.talvi.puffinplot.window.EditSampleParametersWindow;
import net.talvi.puffinplot.window.RemoveByDepthRangeDialog;
import net.talvi.puffinplot.window.TreatmentWindow;

/**
 * A container class for individual instances of {@link PuffinAction}
 * which represent particular user actions in PuffinPlot.
 * 
 */
public class PuffinActions {

    private static final Logger LOGGER =
            Logger.getLogger("net.talvi.puffinplot");
 
    private final PuffinApp app;

    /**
     * Instantiates a new set of actions which will be performed on the
     * specified PuffinPlot application.
     * 
     * @param app the PuffinPlot instance to which to apply the actions
     * when they are performed
     */
    PuffinActions(PuffinApp app) {
        this.app = app;
    }
    
    /**
     * Opens the application's ‘About’ dialog box.
     */
    public final Action about = new PuffinAction("About PuffinPlot",
            "Show information about this program",
            null, false, KeyEvent.VK_A) {
        private static final long serialVersionUID = 1L;
        @Override
        public void actionPerformed(ActionEvent e) {
            app.about();
        }
    };

    /**
     * Opens a dialog box allowing the user to choose one or more files
     * to open as a new data suite.
     */
    public final Action open = new PuffinAction("Open…",
            "Open a data file in a new suite.", 'O', false,
            KeyEvent.VK_O) {
        private static final long serialVersionUID = 1L;
        @Override
        public void actionPerformed(ActionEvent e) {
            app.showOpenFilesDialog(true);
        }
    };
    
    /**
     * Opens a dialog box allowing the user to choose one or more files
     * to append to the current data suite.
     */
    public final Action appendFiles = new PuffinAction(
            "Append more palaeomagnetic data…",
            "Open a data file and append the data to the current suite.",
            'O', true,
            KeyEvent.VK_P) {
        private static final long serialVersionUID = 1L;
        @Override
        public void actionPerformed(ActionEvent e) {
            app.showOpenFilesDialog(false);
        }
    };

    /**
     * Opens a dialog box allowing the user to choose a folder
     * to open as a new data suite.
     */
    public final Action openFolder = new PuffinAction("Open folder…",
            "Open a folder of data files.", null, false,
            KeyEvent.VK_L) {
        private static final long serialVersionUID = 1L;
        @Override
        public void actionPerformed(ActionEvent e) {
            app.showMacOpenFolderDialog();
        }
    };

    /**
     * Closes the current data suite.
     */
    public final Action close = new PuffinAction("Close",
            "Close this suite of data", 'W', false, KeyEvent.VK_C) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.closeCurrentSuite();
        }
    };


    /**
     * Opens a ‘Save’ dialog box; sample calculations are saved to 
     * the chosen file (if any).
     */
    public final Action exportCalcsSample =
            new PuffinAction("Export sample calculations…",
            "Write sample calculations to a CSV file.",
            null, false, KeyEvent.VK_A) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent event) {
            if (app.getSuite() == null) {
                app.errorDialog("Error saving calculations", "No file loaded.");
                            return;
            }
            final String pathname = app.getSavePath(
                    "Export sample calculations",
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
    public final Action exportCalcsSite = 
            new PuffinAction("Export site calculations…",
            "Write site calculations to a CSV file.",
            null, false, KeyEvent.VK_I) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent arg0) {
            if (app.getSuite() == null) {
                app.errorDialog("Error saving calculation", "No file loaded.");
                            return;
            }
            final String pathname = app.getSavePath("Export site calculations",
                    ".csv", "Comma Separated Values");

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
    public final Action exportCalcsSuite = 
            new PuffinAction("Export suite calculations…",
            "Write suite calculations to a CSV file.",
            null, false, KeyEvent.VK_U) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent arg0) {
            if (app.getSuite() == null) {
                app.errorDialog("Error saving calculation", "No file loaded.");
                return;
            }
            String pathname = app.getSavePath("Export suite calculations",
                    ".csv", "Comma Separated Values");

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
    public final Action exportCalcsMultiSuite = 
            new PuffinAction("Export multi-suite calculations…",
            "Write multi-suite calculations to a CSV file.",
            null, false, KeyEvent.VK_M) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent arg0) {
            app.exportCalcsMultiSuite();
        }
    };

    /**
     * If a PuffinPlot file is associated with the current suite,
     * the suite is saved to that file. If not, this action is 
     * equivalent to {@link #saveAs}.
     */
    public final Action save = new PuffinAction("Save",
            "Re-save the current file.", 'S', false, KeyEvent.VK_S) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.save();
        }
    };

    /**
     * Opens a ‘save’ dialog box; the current suite is saved to the 
     * selected file in PuffinPlot format.
     */
    public final Action saveAs = new PuffinAction("Save as…",
            "Save this suite of data in a new file.", 'S', true,
            KeyEvent.VK_A) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent arg0) {
            if (app.getSuite() != null) {
                app.showSaveAsDialog(app.getSuite());
            }
        }
    };
    
    /**
     * Opens a dialog box allowing the user to change the page setup 
     * for printing.
     */
    public final Action pageSetup = new PuffinAction("Page Setup…",
            "Edit the page setup for printing.", 'P', true, KeyEvent.VK_E) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent arg0) {
            app.showPageSetupDialog();
        }
    };
    
    /**
     * Rotates the currently selected samples 180° about the X axis.
     */
    public final Action flipSampleX = new PuffinAction(
            "Flip samples around X axis",
            "Rotate selected samples 180° about the X axis.", null, false,
            KeyEvent.VK_X) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent arg0) {
            app.flipSelectedSamples(MeasurementAxis.X);
        }
    };
    
    /**
     * Rotates the currently selected samples 180° about the Y axis.
     */
    public final Action flipSampleY = new PuffinAction(
            "Flip samples around Y axis",
            "Rotate selected samples 180° about the Y axis.", null, false,
            KeyEvent.VK_Y) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent arg0) {
            app.flipSelectedSamples(MeasurementAxis.Y);
        }
    };
    
    /**
     * Rotates the currently selected samples 180° about the Z axis.
     */
    public final Action flipSampleZ = new PuffinAction(
            "Flip samples around Z axis",
            "Rotate selected samples 180° about the Z axis.", null, false,
            KeyEvent.VK_Z) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent arg0) {
            app.flipSelectedSamples(MeasurementAxis.Z);
        }
    };
    
    /**
     * Inverts every vector in the currently selected samples.
     */
    public final Action invertSamples = new PuffinAction(
            "Invert sample directions",
            "Invert all magnetization directions for selected samples.",
            null, false, KeyEvent.VK_I) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent arg0) {
            app.invertSelectedSamples();
        }
    };
    
    /**
     * Performs principal component analysis on the selected points of the
     * selected samples.
     */
    public final Action pcaOnSelection = new PuffinAction("Calculate PCA",
            "Perform principal component analysis (anchored or unanchored) "
            + "on selected points", 'R', false, KeyEvent.VK_P) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.doPcaOnSelection();
        }
    };

    /**
     * Calculates Fisherian statistics on selected demagnetization steps
     * of the selected samples.
     */
    public final Action fisherOnSample = new PuffinAction("Fisher on sample",
            "Fisher statistics on selected demagnetization steps of selected samples",
            null, false,
            KeyEvent.VK_S) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            final Suite suite = app.getSuite();
            if (suite == null) {
                app.errorDialog("Fisher on sample", "No suite loaded.");
            } else {
                for (Sample s: app.getSelectedSamples()) {
                    s.clearPca();
                    s.calculateFisher(app.getCorrection());
                }
                suite.calculateSiteFishers(app.getCorrection());
                app.getSuiteEqAreaWindow().getPlot().setGroupedBySite(true);
                app.updateDisplay();
            }
        }
    };
    
    /**
     * Calculates Fisherian statistics on PCA directions from the current site.
     */
    public final Action fisherBySite = new PuffinAction("Fisher by site",
            "Fisher statistics on PCA directions grouped by site.", 'F', false,
            KeyEvent.VK_I) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            final Suite suite = app.getSuite();
            if (suite == null) {
                app.errorDialog("Fisher by site", "No suite loaded.");
            } else if (suite.getSites().isEmpty()) {
                app.errorDialog("Fisher by site", "No sites have been defined "
                        + "for this suite.");
            } else {
                suite.calculateSiteFishers(app.getCorrection());
                app.getSuiteEqAreaWindow().getPlot().setGroupedBySite(true);
                app.updateDisplay();
            }
        }
    };
    
    /**
     * Calculates Fisher statistics on sample PCA directions for all
     * selected samples, and on site means for all selected sites.
     */
    public final Action suiteMeans = new PuffinAction("Suite means",
            "Calculate mean directions using all selected sites and samples.",
            'F', true, KeyEvent.VK_U) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            final Suite suite = app.getSuite();
            if (suite == null) {
                app.errorDialog("Calculate suite means", "No suite loaded.");
            } else {
                suite.calculateSuiteMeans(app.getSelectedSamples(),
                        app.getSelectedSites());
                app.getSuiteEqAreaWindow().getPlot().setGroupedBySite(false);
                app.updateDisplay();
            }
        }
    };

    /**
     * For each selected sample, fits a great circle to the selected points.
     */
    public final Action circleFit = new PuffinAction("Fit great circle",
            "Fit great circle to selected points.", 'L', false, KeyEvent.VK_G) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.fitGreatCirclesToSelection();
        }
    };

    /**
     * Determines a site mean through McFadden and McElhinny great-circle
     * intersection.
     */
    public final Action greatCircleAnalysis = new PuffinAction("Great circles",
            "Great circle analysis for site.", 'I', false, KeyEvent.VK_L) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.calculateGreatCirclesDirections();
        }
    };

    /**
     * Clears any previously calculated Fisherian or great-circle site
     * directions.
     */
    public final Action clearSiteCalcs = new PuffinAction(
            "Clear site calculations",
            "Clear site Fisher and Great Circle calculations.", 'I', true,
            KeyEvent.VK_E) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.clearSiteCalculations();
        }
    };

    /**
     * Calculates the median destructive field or temperature of the selected
     * samples.
     */
    public final Action mdf = new PuffinAction("MDF",
            "Calculate median destructive field (or temperature) on selected samples.",
            'M', false, KeyEvent.VK_M) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            for (Sample s: app.getSelectedSamples()) s.calculateMdf();
            app.updateDisplay();
        }
    };

    /**
     * Opens a window allowing the user to edit the sample and formation
     * orientations and the local magnetic declination.
     */
    public final Action showEditSampleParametersDialog = new PuffinAction(
            "Edit sample parameters…",
            "Edit sample volume, sample and formation orientations, and local geomagnetic field declination.",
            null, false, KeyEvent.VK_P) {
        private static final long serialVersionUID = 1L;
        private EditSampleParametersWindow espDialog;
        @Override public void actionPerformed(ActionEvent e) {
            if (espDialog == null) {
                espDialog = new EditSampleParametersWindow(app);
            }
            espDialog.setLocationRelativeTo(app.getMainWindow());
            espDialog.setVisible(true);
        }
    };
    
    /**
     * Shows a window allowing the user to set the treatment type for
     * the selected samples.
     */
    public final Action setTreatType = new PuffinAction("Set treatment type…",
            "Set the treatment type for the selected samples.",
            null, false, KeyEvent.VK_T) {
        private static final long serialVersionUID = 1L;
        private TreatmentWindow treatmentWindow;
        @Override public void actionPerformed(ActionEvent e) {
            if (treatmentWindow == null) {
                treatmentWindow = new TreatmentWindow(app);
            }
            treatmentWindow.setLocationRelativeTo(app.getMainWindow());
            treatmentWindow.setVisible(true);
        }
    };
    
    /**
     * For each selected sample, clears PCA calculation.
     */
    public final Action clearSamplePca = new PuffinAction("Clear sample PCAs",
            "Clear PCA calculations for selected samples",
            null, false, null) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.modifySelectedSamples(Sample::clearPca);
        }
    };
    
    /**
     * For each selected sample, clears great circle fit.
     */
    public final Action clearSampleGreatCircle = new PuffinAction(
            "Clear sample GC fits",
            "Clear great-circle fits for selected samples.",
            null, false, null) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.modifySelectedSamples(Sample::clearGreatCircle);
        }
    };
    
    /**
     * For each selected sample, clears all calculations and deselects all
     * points.
     */
    public final Action clearSampleCalcs = new PuffinAction(
            "Clear sample calculations",
            "Clear all calculations and selected steps for selected samples.",
            'Z', false, KeyEvent.VK_C) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.modifySelectedSamples(Sample::clearCalculations);
        }
    };
    
    /**
     * Within each selected sample, selects all the points.
     */
    public final Action selectAll = new PuffinAction("Select all steps",
            "Select all visible treatment steps in selected samples", 'D',
            false, KeyEvent.VK_A) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            for (Sample s: app.getSelectedSamples()) {
                s.selectVisible();
            }
            app.updateDisplay();
        }
    };

    /**
     * For each selected sample, deselects all the points.
     */
    public final Action clearSelection = new PuffinAction(
            "Clear step selection",
            "Deselect all treatment steps in selected samples.", 'D', true,
            KeyEvent.VK_E) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            for (Sample s: app.getSelectedSamples()) s.selectNone();
                app.updateDisplay();
        }
    };

    /**
     * Copies the range of selected points from the current sample onto
     * a clipboard.
     */
    public final Action copyStepSelection =
            new PuffinAction("Copy step selection",
            "Copy the treatment step selection to the clipboard.",
                    'J', false, KeyEvent.VK_C) {
                private static final long serialVersionUID = 1L;
                @Override public void actionPerformed(ActionEvent e) {
                    app.copyStepSelection(); }};
    
    /**
     * For each selected sample, sets the selected points using the 
     * range currently copied to the clipboard.
     */
    public final Action pasteStepSelection = new PuffinAction(
            "Paste step selection",
            "Select the treatment steps corresponding to those copied to the clipboard.",
            'K', false, KeyEvent.VK_P) {
        private static final long serialVersionUID = 1L;
                @Override public void actionPerformed(ActionEvent e) {
                    app.pasteStepSelection(); }};

    /**
     * For each selected sample, makes the selected points invisible.
     */
    // we can't use ctrl-H because Apples use it already.
    public final Action hideSelectedSteps = new PuffinAction("Hide steps",
            "Hide the selected treatment steps", 'G', false, KeyEvent.VK_H) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.getSelectedSamples().forEach((s) -> {
                s.hideAndDeselectSelectedPoints();
            });
           app.updateDisplay();
        }
    };

    /**
     * For each selected sample, makes all the points visible.
     */
    public final Action unhideAllSteps = new PuffinAction("Show all steps",
            "Make hidden treatment steps visible again for all selected samples",
            'G', true, KeyEvent.VK_S) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            for (Sample s : app.getSelectedSamples()) s.unhideAllPoints();
            app.updateDisplay();
        }
    };

    /**
     * Opens the preferences window.
     */
    public final Action prefs = new PuffinAction("Preferences…",
            "Show the preferences window", ',', false, KeyEvent.VK_F, true,
            PuffinAction.modifierKey) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.showPreferences();
        }
    };
    
    /**
     * Opens a printing dialog box allowing printing of the main window
     * data display.
     */
    public final Action print = new PuffinAction("Print…",
            "Print the selected samples", 'P', false, KeyEvent.VK_P) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.showPrintDialog("MAIN");
        }
    };
    
    /**
     * Opens a print dialog for the site equal-area plot window.
     */
    public final Action printSiteEqualArea = new PuffinAction(
            "Print site EA window…",
            "Print the contents of the site equal-area plot window",
            null, false, KeyEvent.VK_N) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.showPrintDialog("SITE");
        }
    };
    
    /**
     * Opens a printing dialog box allowing printing of the suite equal-area 
     * data display.
     */
    public final Action printSuiteEqualArea = new PuffinAction(
            "Print suite EA window…",
            "Print the contents of the suite equal-area plot window",
            null, false, KeyEvent.VK_U) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.showPrintDialog("SUITE");
        }
    };
    
    /**
     * Terminates the application immediately.
     */
    public final Action quit = new PuffinAction("Quit",
            null, 'Q', false, KeyEvent.VK_Q, true, PuffinAction.modifierKey) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.quit();
        }
    };

    /**
     * Resets the layout of the plots to the default.
     */
    public final Action resetLayout = new PuffinAction("Reset layout",
            "Move plots back to their original positions", null, false,
            KeyEvent.VK_R) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.getMainWindow().getGraphDisplay().resetLayout();
        }
    };

    /**
     * Opens a file dialog allowing the user to choose an Agico ASC file
     * from which to import AMS data.
     */
    public final Action importAms = new PuffinAction("Import AMS…",
            "Import AMS data from Agico ASC file", null, false, KeyEvent.VK_A) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.showImportAmsDialog();
        }
    };

    /**
     * Calculate mean directions for data in all the currently open data suites.
     */
    public final Action multiSuiteMeans = new PuffinAction("Multi-suite means",
            "Calculate means for data in all open suites", null, false,
            KeyEvent.VK_T) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.calculateMultiSuiteMeans();
        }
    };

    /**
     * Opens a save dialog allowing the export of the current suite's
     * IRM data as a tab-delimited text file.
     */
    public final Action exportIrm = new PuffinAction("Export IRM data…",
            "Export IRM field/remanence for this suite", null, false,
            KeyEvent.VK_I) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            if (app.getSuite() == null) return;
            String pathname = app.getSavePath("Export IRM data", null,
                    null);
            app.getSuite().exportToFiles(new File(pathname),
                    Arrays.asList(new TreatmentStepField[] {TreatmentStepField.IRM_FIELD,
                    TreatmentStepField.VIRT_MAGNETIZATION}));
        }
    };

    /**
     * Opens a window which allows the user to edit the list of user-defined
     * flags for the current suite.
     */
    public final Action showCustomFlagsWindow = new PuffinAction(
            "Edit custom flags…",
            "Edit user-defined flags for samples",
            null, false, KeyEvent.VK_F) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
           app.showCustomFlagsWindow();
        }
    };

    /**
     * Opens a window which allows the user to edit the list of user-defined
     * note headings for the current suite.
     */
    public final Action showCustomNotesWindow = new PuffinAction(
            "Edit custom notes…",
            "Edit user-defined notes for samples",
            null, false, KeyEvent.VK_N) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
           app.showCustomNotesWindow();
        }
    };

    /**
     * Opens a save dialog allowing the current main display to be saved
     * as an SVG file using the Batik library.
     */
    public final Action exportSvgBatik = new PuffinAction("Export SVG (Batik)…",
            "Save current display to an SVG file, using the Batik graphics library",
            '6', false, KeyEvent.VK_S) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            String pathname = app.getSavePath("Export to SVG (Batik)", ".svg",
                    "Scalable Vector Graphics");
            if (pathname != null)
                app.getMainWindow().getGraphDisplay().saveToSvgBatik(pathname);
        }
    };
    
    /**
     * Opens a save dialog allowing the current main display to be saved
     * as an SVG file using the FreeHEP library.
     */
    public final Action exportSvgFreehep = new PuffinAction(
            "Export SVG (FreeHEP)…",
            "Save current display to an SVG file, using the FreeHEP graphics library",
            '7', false, KeyEvent.VK_V) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            String pathname = app.getSavePath("Export to SVG (FreeHEP)", ".svg",
                    "Scalable Vector Graphics");
            if (pathname != null) {
                try {
                app.getMainWindow().getGraphDisplay().
                        saveToSvgFreehep(pathname);
                } catch (IOException ex) {
                    app.errorDialog("Error exporting SVG",
                            ex.getLocalizedMessage());
                }
            }
        }
    };

    /**
     * Opens a save dialog allowing the current main display to be saved
     * as a PDF file.
     */
    public final Action exportPdfItext = new PuffinAction("Export PDF (iText)…",
            "Print data for selected samples to a PDF file, "
                    + "using the iText graphics library",
            '8', false, KeyEvent.VK_P) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            String pathname = app.getSavePath("Export to PDF", ".pdf",
                    "Portable Document Format");
            if (pathname != null)
                try {
                app.exportPdfItext(new File(pathname));
            } catch (DocumentException | FileNotFoundException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    };
    
    /**
     * Opens a save dialog allowing the current main display to be saved
     * as a PDF file.
     */
    public final Action exportPdfFreehep = new PuffinAction(
            "Export PDF (FreeHEP)…",
            "Print data for selected samples to a PDF file, "
                    + "using the FreeHEP graphics library",
            '9', false, KeyEvent.VK_D) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            String pathname = app.getSavePath("Export to PDF", ".pdf",
                    "Portable Document Format");
            if (pathname != null)
                try {
                app.exportPdfFreehep(new File(pathname));
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    };
    
    /**
     * Calculates bootstrap AMS statistics on the selected samples.
     */
    public final Action bootAmsNaive = new PuffinAction(
            "Calculate bootstrap AMS",
            "Calculate bootstrap statistics for AMS data of selected samples",
            null, false, KeyEvent.VK_B) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.doAmsCalc(AmsCalculationType.BOOT, "bootams.py");
        }
    };

    /**
     * Calculates parametric bootstrap AMS statistics on the selected samples.
     */
    public final Action bootAmsParam = new PuffinAction(
            "Parametric bootstrap AMS",
            "Calculate parametric bootstrap statistics "
                    + "for AMS data of selected samples",
            null, false, KeyEvent.VK_P) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.doAmsCalc(AmsCalculationType.PARA_BOOT, "bootams.py");
        }
    };

    /**
     * Calculates Hext AMS statistics on the selected samples.
     */
    public final Action hextAms = new PuffinAction("Calculate Hext on AMS",
            "Calculate Hext statistics for AMS data of selected samples",
            null, false, KeyEvent.VK_H) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.doAmsCalc(AmsCalculationType.HEXT, "s_hext.py");
        }
    };

    /**
     * Scales all magnetic susceptibility values in the current suite by
     * a user-specified factor.
     */
    public final Action rescaleMagSus = new PuffinAction(
            "Rescale susceptibility…",
            "Scale magnetic susceptibility by a constant factor (whole suite)",
            null, false, KeyEvent.VK_S) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.showRescaleMagSusDialog();
        }
    };
        
    /**
     * Clears the current user preferences, resetting them to their default
     * values.
     */
    public final Action clearPreferences = new PuffinAction("Clear preferences",
            "Reset all preferences to the default values.",
            null, false, KeyEvent.VK_L) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.clearPreferences();
        }
    };
    
    /**
     * Exports the current user preferences to an XML file.
     */
    
    public final Action exportPrefs = new PuffinAction("Export preferences…",
            "Save the current preferences to a file, "
                    + "allowing them to be restored later.",
            null, false, KeyEvent.VK_X) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent arg0) {
            String pathname = app.getSavePath("Export preferences", ".xml",
                    "eXtensible Markup Language");
            if (pathname != null)
                app.getPrefs().exportToFile(new File(pathname));
        }
    };

    /**
     * Imports user preferences from an XML file.
     */
    public final Action importPrefs = new PuffinAction("Import preferences…",
            "Read preferences from a file, "
                    + "discarding your current preferences.",
            null, false, KeyEvent.VK_T) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent arg0) {
            app.showImportPreferencesDialog();
        }
    };

    /**
     * Clears AMS calculations for the current suite.
     */
    public final Action clearAmsCalcs =
        new PuffinAction("Clear AMS calculations",
            "Clear all calculations on AMS data",
            null, false, KeyEvent.VK_C) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent arg0) {
            app.clearAmsCalcs();
        }
    };
    
    /**
     * Clears site definitions for selected samples.
     */
    public final Action clearSites = new PuffinAction("Clear sites",
            "Remove site names for all selected samples.", null, false,
            KeyEvent.VK_C) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.getSuite().clearSites(app.getSelectedSamples());
            app.updateDisplay();
        }
    };
    
    /**
     * Opens a dialog box allowing the user to specify a site name
     * for the selected samples.
     */
    public final Action setSiteName = 
            new PuffinAction("Set site name…",
            "Define a single site name for all selected samples.",
                    null, false, KeyEvent.VK_N) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            final String name = JOptionPane.showInputDialog("Site name");
            if (name==null || "".equals(name)) return;
            app.getSuite().setNamedSiteForSamples(app.getSelectedSamples(),
                    name);
            app.updateDisplay();
        }
    };
    
    /**
     * Opens a dialog box allowing the user to specify which characters of
     * the currently selected samples should be used to determine the site
     * name.
     */
    public final Action setSitesFromSampleNames = 
        new PuffinAction("Set sites from sample names…",
            "Set site names for selected samples using parts of the sample names.",
                null, false, KeyEvent.VK_S) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            final String maskSpec =
                    JOptionPane.showInputDialog("Character positions to use");
            if (maskSpec==null || "".equals(maskSpec)) return;
            app.getSuite().setSiteNamesBySubstring(app.getSelectedSamples(),
                    Util.numberRangeStringToBitSet(maskSpec, 256));
            app.updateDisplay();
        }
    };
    
    /**
     * Opens a dialog box allowing the user to specify a site thickness, which
     * is then used to divide a long core suite into sites based on sample
     * depths.
     */
    public final Action setSitesByDepth = 
        new PuffinAction("Set sites by depth…",
            "Set site names for the selected samples using sample depth.",
            null, false, KeyEvent.VK_D) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            final String thicknessString =
                    JOptionPane.showInputDialog("Thickness of depth slices");
            if (thicknessString==null || "".equals(thicknessString)) return;
            float thickness = Float.NaN;
            try {
                thickness = Float.parseFloat(thicknessString);
            } catch (NumberFormatException ex) {
                app.errorDialog("Invalid number",
                        thicknessString + " is not a number.");
            }
            if (Float.isNaN(thickness)) return;
            app.getSuite().setSiteNamesByDepth(app.getSelectedSamples(),
                    thickness);
            app.updateDisplay();
        }
    };

    /**
     * Runs a Python script using the Jython interpreter.
     */
    public final Action runPythonScript = new PuffinAction("Run Python script…",
            "Use a script written in Python to perform functions in PuffinPlot.",
            null, false, KeyEvent.VK_Y) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.showRunPythonScriptDialog();
        }
    };

    /**
     * Runs a Python script using the Jython interpreter.
     */
    public final Action runJavascriptScript = new PuffinAction(
            "Run Javascript script…",
            "Use a script written in Javascript to perform functions in PuffinPlot.",
            null, false, KeyEvent.VK_J) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.showRunJavascriptScriptDialog();
        }
    };

    /**
     * Opens the PuffinPlot website.
     */
    public final Action openPuffinWebsite = new PuffinAction(
            "PuffinPlot website",
            "Visit the PuffinPlot website", null, false, KeyEvent.VK_W) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.openWebPage("http://talvi.net/puffinplot");
        }
    };
    
    /**
     * Opens the Citation window.
     */
    public final Action showCiteDialog = new PuffinAction("Cite PuffinPlot…",
            "Show information on citing PuffinPlot.", null, false,
            KeyEvent.VK_C) {
        private static final long serialVersionUID = 1L;
        private CiteWindow citeWindow;
        @Override public void actionPerformed(ActionEvent e) {
            if (citeWindow == null) {
                citeWindow = new CiteWindow(app);
            }
            citeWindow.setLocationRelativeTo(app.getMainWindow());
            citeWindow.setVisible(true);
        }
    };

    /**
     * Performs an RPI calculation.
     */
    public final Action calculateRpi = new PuffinAction("Calculate RPI…",
            "Calculate relative palaeomagnetic intensity.",
            null, false, KeyEvent.VK_R) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.calculateRpi();
        }
    };
    
    /**
     * Imports site location data.
     */
    public final Action importLocations =
            new PuffinAction("Import site locations…",
            "Import site co-ordinates from a CSV file.",
            null, false, KeyEvent.VK_L) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.showImportLocationsDialog();
        }
    };
        
    /**
     * Converts a discrete suite into a continuous one.
     */
    public final Action convertDiscreteToContinuous =
            new PuffinAction("Discrete to continuous…",
            "Converts a discrete suite into a continuous one.",
            null, false, KeyEvent.VK_D) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.showDiscreteToContinuousDialog();
        }
    };
    
    /**
     * Creates a bundle.
     */
    public final Action createBundle =
            new PuffinAction("Create bundle…",
            "Create a self-contained runnable bundle for the current analyses.",
            null, false, KeyEvent.VK_B) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.showCreateBundleDialog();
        }
    };

    /**
     * Automatically aligns declinations between core sections.
     */
    public final Action alignSectionDeclinations =
            new PuffinAction("Align core section declinations…",
            "Automatically align declinations between core sections.",
            null, false, KeyEvent.VK_A) {
        private static final long serialVersionUID = 1L;
        private AlignDeclinationsDialog alignDeclinationsDialog;
        @Override public void actionPerformed(ActionEvent e) {
            if (alignDeclinationsDialog == null) {
                alignDeclinationsDialog = new AlignDeclinationsDialog(app);
            }
            alignDeclinationsDialog.setLocationRelativeTo(app.getMainWindow());
            alignDeclinationsDialog.setVisible(true);
        }
    };
    
    /**
     * Removes samples whose depth falls outside a specified range.
     */
    public final Action removeSamplesOutsideDepthRange =
            new PuffinAction("Remove samples by depth…",
            "Remove samples whose depth lies outside a specified range.",
            null, false, KeyEvent.VK_D) {
        private static final long serialVersionUID = 1L;
        private RemoveByDepthRangeDialog removeByDepthRangeDialog;
        @Override public void actionPerformed(ActionEvent e) {
            if (removeByDepthRangeDialog == null) {
                removeByDepthRangeDialog =
                        new RemoveByDepthRangeDialog(app.getMainWindow(), true,
                                app);
            }
            removeByDepthRangeDialog.setLocationRelativeTo(app.getMainWindow());
            removeByDepthRangeDialog.setVisible(true);
        }
    };

    /**
     * Removes samples with a particular treatment type.
     */
    public final Action removeSamplesByTreatmentType =
            new PuffinAction("Remove samples by treatment type…",
            "Remove samples with a particular treatment type.",
            null, false, KeyEvent.VK_Y) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            final JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            final JLabel label =
                    new JLabel("Remove selected samples with treatment type:");
            label.setAlignmentX(0.5f);
            panel.add(label);
            panel.add(Box.createVerticalStrut(8));
            final JComboBox comboBox = 
                    new JComboBox(Arrays.stream(TreatmentType.values()).
                    map(tt -> tt.getNiceName()).toArray());
            panel.add(comboBox);
            final int option = JOptionPane.showConfirmDialog(
                    app.getMainWindow(), panel,
                    "Remove samples by treatment type",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (option == JOptionPane.OK_OPTION) {
                app.getSuite().removeSamplesByTreatmentType(
                        app.getSelectedSamples(),
                        TreatmentType.values()[comboBox.getSelectedIndex()]
                );
                app.getMainWindow().suitesChanged();
            }
        }
    };
    
    /**
     * Merges duplicate treatment steps within the selected samples.
     */
    public final Action mergeDuplicateTreatmentSteps =
            new PuffinAction("Merge duplicate steps",
            "Merge any treatment steps with the same type and level.",
            null, false, KeyEvent.VK_M) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.getSuite().mergeDuplicateTreatmentSteps(
                    app.getSelectedSamples());
            app.updateDisplay();
        }
    };
    
    /**
     * Merges samples with the same name or depth, and treatment steps
     * with the same treatment type and level within samples.
     */
    public final Action mergeDuplicateSamples =
            new PuffinAction("Merge duplicate samples",
            "Merge any samples with the same name or depth.",
            null, false, KeyEvent.VK_M) {
        private static final long serialVersionUID = 1L;
        @Override public void actionPerformed(ActionEvent e) {
            app.getSuite().mergeDuplicateSamples(app.getSelectedSamples());
            app.updateDisplay();
        }
    };
    
}
