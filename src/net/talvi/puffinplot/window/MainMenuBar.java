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
package net.talvi.puffinplot.window;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import net.talvi.puffinplot.PuffinAction;
import net.talvi.puffinplot.PuffinActions;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.RecentFileList;
import net.talvi.puffinplot.Util;
import net.talvi.puffinplot.data.Sample;

/**
 * The menu bar and attached hierarchy of menus for PuffinPlot's main window.
 * 
 * @author pont
 */
public final class MainMenuBar extends JMenuBar {

    private static final Logger LOGGER =
            Logger.getLogger("net.talvi.puffinplot");
    private static final long serialVersionUID = 1L;
    private static final JMenuItem NO_RECENT_FILES =
            new JMenuItem("No recent files");
    // control or apple key as appropriate
    private static final int MODIFIER_KEY =
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    private JMenu recentFilesMenu;
    private final JCheckBoxMenuItem anchorItem;
    private final PuffinApp app;
    private final int[] recentFileKeycodes;

    private static JMenu makeMenu(String name, int mnemonic, Object... things) {
        final JMenu menu = new JMenu(name);
        menu.setMnemonic(mnemonic);
        LOGGER.log(Level.FINE, "makeMenu {0}", name);
        for (Object thing: things) {
            if (thing == null) continue;
            if (thing instanceof PuffinAction) {
                final PuffinAction puffinAction = (PuffinAction) thing;
                if (!puffinAction.isExcludedFromMenu()) menu.add(puffinAction);
                LOGGER.log(Level.FINE,
                        puffinAction.getValue(Action.NAME).toString());
            } else if (thing instanceof Action) {
                final Action a = (Action) thing;
                menu.add(a);
                LOGGER.log(Level.FINE, a.getValue(Action.NAME).toString());
            } else if (thing instanceof JMenuItem) {
                /*
                 * JMenu is a subclass of JMenuItem so this case handles
                 * submenus as well as "raw" JMenuItems.
                 */
                final JMenuItem jmi = (JMenuItem) thing;
                menu.add(jmi);
                LOGGER.log(Level.FINE, jmi.getText());
            } else {
                LOGGER.log(Level.WARNING, "Unknown menu item {0}", thing);
            }
        }
        return menu;
    }

    /**
     * Creates a new menu bar and menu tree.
     * 
     * @param app the PuffinApp instance associated with this menu bar
     */
    public MainMenuBar(final PuffinApp app) {
        this.app = app;
        this.recentFileKeycodes = new int[] {KeyEvent.VK_1, KeyEvent.VK_2,
            KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6,
            KeyEvent.VK_7, KeyEvent.VK_8};
        final PuffinActions pa = app.getActions();
        recentFilesMenu = new JMenu("Open recent file");
        recentFilesMenu.setMnemonic(KeyEvent.VK_R);
        NO_RECENT_FILES.setEnabled(false);
        updateRecentFiles();

        final JCheckBoxMenuItem movePlotsItem =
                new JCheckBoxMenuItem("Edit layout") {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public boolean isSelected() {
                        final MainWindow w = app.getMainWindow();
                        return w != null ? w.getGraphDisplay().isDragPlotMode()
                                : false;
                    }
                };
        movePlotsItem.setMnemonic(KeyEvent.VK_E);
        movePlotsItem.addActionListener(event -> {
            final GraphDisplay gd = app.getMainWindow().getGraphDisplay();
            gd.setDragPlotMode(!gd.isDragPlotMode());
            gd.repaint();
        });

        add(makeMenu("File", KeyEvent.VK_F,
                pa.open,
                Util.runningOnOsX() ? pa.openFolder : null,
                recentFilesMenu, pa.save, pa.saveAs, pa.close,
                makeMenu("Export data", KeyEvent.VK_D,
                        pa.exportCalcsSample,
                        pa.exportCalcsSite, pa.exportCalcsSuite, 
                        pa.exportCalcsMultiSuite, pa.exportIrm,
                        pa.createBundle),
                makeMenu("Export graphics", KeyEvent.VK_G,
                        pa.exportSvgBatik, pa.exportSvgFreehep,
                        pa.exportPdfItext, pa.exportPdfFreehep),
                makeMenu("Import data", KeyEvent.VK_I,
                        pa.appendFiles,
                        pa.importLocations, pa.importAms),
                pa.pageSetup, pa.print, pa.printSuiteEqualArea, pa.printSiteEqualArea,
                pa.runJavascriptScript, pa.runPythonScript, pa.prefs, pa.quit));
        
        add(makeMenu("Edit",KeyEvent.VK_E,
                movePlotsItem, pa.resetLayout,
                makeMenu("Treatment steps", KeyEvent.VK_T,
                        pa.selectAll, pa.clearSelection,
                        pa.copyStepSelection, pa.pasteStepSelection,
                        pa.hideSelectedSteps, pa.unhideAllSteps,
                        pa.mergeDuplicateTreatmentSteps),
                makeMenu("Samples", KeyEvent.VK_A,
                        pa.showEditSampleParametersDialog,
                        pa.setTreatType,
                        makeMenu("Rotate/invert samples", KeyEvent.VK_R, 
                                pa.flipSampleX, pa.flipSampleY, pa.flipSampleZ,
                                pa.invertSamples, pa.alignSectionDeclinations),
                        pa.removeSamplesOutsideDepthRange,
                        pa.removeSamplesByTreatmentType,
                        pa.mergeDuplicateSamples,
                        pa.rescaleMagSus
                        ),
                makeMenu("Sites", KeyEvent.VK_I,
                        pa.setSiteName, pa.setSitesFromSampleNames,
                        pa.setSitesByDepth, pa.clearSites),
                makeMenu("Suite", KeyEvent.VK_U,
                        pa.showCustomFlagsWindow, pa.showCustomNotesWindow,
                        pa.convertDiscreteToContinuous)
        ));
        
        add(makeMenu("Calculations", KeyEvent.VK_C,
                pa.pcaOnSelection, anchorItem = new AnchorItem(),
                pa.fisherBySite, pa.fisherOnSample, pa.suiteMeans,
                pa.mdf, pa.clearSampleCalcs,
                pa.clearSamplePca, pa.clearSampleGreatCircle,
                pa.circleFit,
                pa.greatCircleAnalysis, pa.clearSiteCalcs,
                pa.multiSuiteMeans,
                makeMenu("AMS", KeyEvent.VK_A,
                        pa.bootAmsNaive, pa.bootAmsParam, pa.hextAms,
                        pa.clearAmsCalcs),
                pa.calculateRpi
        ));
        add(makeMenu("Window", KeyEvent.VK_W,
                new WindowMenuItem("Data table", KeyEvent.VK_D,
                        app::getTableWindow),
                new WindowMenuItem("Site equal-area plot", KeyEvent.VK_I,
                        app::getSiteEqAreaWindow),
                new WindowMenuItem("Suite equal-area plot", KeyEvent.VK_U,
                        app::getSuiteEqAreaWindow)
                    ));
        add(makeMenu("Help", KeyEvent.VK_H,
                pa.openPuffinWebsite, pa.showCiteDialog, pa.about));
    }

    private class AnchorItem extends JCheckBoxMenuItem {
        private static final long serialVersionUID = 1L;
        AnchorItem() {
            super("PCA anchored");
            setToolTipText("If this item is checked, "
                    + "PCA analyses will be anchored");
            /*
             * If an ItemListener is used here instead of an ActionListener, it
             * will also be called when the item is set programmatically (e.g.
             * to reflect a state taken from a new current sample). An
             * ActionListener will only be activated when the state is changed
             * by the user.
             */
            addActionListener(event -> {
                for (Sample sample: app.getSelectedSamples()) {
                    sample.setPcaAnchored(isSelected());
                    sample.doPca(app.getCorrection());
                }
                app.updateDisplay();
            });
            setAccelerator(KeyStroke.getKeyStroke('T', MODIFIER_KEY));
            setMnemonic(KeyEvent.VK_N);
        }

        @Override
        public boolean getState() {
                final Sample sample = app.getCurrentSample();
                return sample != null ? sample.isPcaAnchored() : false;
            }
    }

    /**
     * Alerts the menu bar that the current sample has changed.
     * This allows any stateful menu items to be changed.
     */
    public void sampleChanged() {
        final Sample sample = app.getCurrentSample();
        if (sample != null) {
            anchorItem.setSelected(sample.isPcaAnchored());
        }
    }
    
    /**
     * Updates the submenu containing the list of recently used files.
     */
    public void updateRecentFiles() {
        recentFilesMenu.removeAll();
        final RecentFileList recent = app.getRecentFiles();
        final String[] recentFileNames = recent.getFilesetNames();
        final List<String> toolTips = recent.getFilesetLongNames();
        if (recentFileNames.length == 0)
            recentFilesMenu.add(NO_RECENT_FILES);
        for (int i=0; i<recentFileNames.length; i++) {
            final int index = i;
            final JMenuItem item = recentFilesMenu.add(new AbstractAction() {
                private static final long serialVersionUID = 1L;
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    app.openFiles(recent.getFilesAndReorder(index), true);
                }
                @Override
                public Object getValue(String key) {
                    return (NAME.equals(key)) ?
                        recent.getFilesetNames()[index] :
                        super.getValue(key);
                }
            });
            item.setToolTipText(toolTips.get(i));
            item.setMnemonic(recentFileKeycodes[i]);
        }
    }
}
