/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2018 Pontus Lurcock.
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
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
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

    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");
    private static final long serialVersionUID = 1L;
    final static private JMenuItem noRecentFiles =
            new JMenuItem("No recent files");
    // control or apple key as appropriate
    private static final int modifierKey =
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    private JMenu recentFilesMenu;
    private final JCheckBoxMenuItem anchorItem;
    private final PuffinApp app;
    private final int[] recentFileKeycodes;

    private static JMenu makeMenu(String name, int mnemonic, Object... things) {
        JMenu menu = new JMenu(name);
        menu.setMnemonic(mnemonic);
        logger.log(Level.FINE, "makeMenu {0}", name);
        for (Object thing: things) {
            if (thing == null) continue;
            if (thing instanceof PuffinAction) {
                PuffinAction puffinAction = (PuffinAction) thing;
                if (!puffinAction.isExcludedFromMenu()) menu.add(puffinAction);
                logger.log(Level.FINE,
                        puffinAction.getValue(Action.NAME).toString());
            } else if (thing instanceof Action) {
                Action a = (Action) thing;
                menu.add(a);
                logger.log(Level.FINE, a.getValue(Action.NAME).toString());
            } else if (thing instanceof JMenuItem) {
                // JMenu is a subclass of JMenuItem so this handles submenus
                // as well as `raw' JMenuItems
                final JMenuItem jmi = (JMenuItem) thing;
                menu.add(jmi);
                logger.log(Level.FINE, jmi.getText());
            } else {
                logger.log(Level.WARNING, "Unknown menu item {0}", thing);
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
        noRecentFiles.setEnabled(false);
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
        movePlotsItem.setMnemonic(KeyEvent.VK_Y);
        movePlotsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                final GraphDisplay gd = app.getMainWindow().getGraphDisplay();
                gd.setDragPlotMode(!gd.isDragPlotMode());
                gd.repaint();
            }
        });

        final JCheckBoxMenuItem useEmptyItem =
                new JCheckBoxMenuItem("Apply empty correction") {
            private static final long serialVersionUID = 1L;
                    @Override
                    public boolean isSelected() {
                        return MainMenuBar.this.app.isEmptyCorrectionActive();
                    }
                };
        useEmptyItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                app.setEmptyCorrectionActive(!app.isEmptyCorrectionActive());
                app.updateDisplay();
            }
        });
        useEmptyItem.setAccelerator(KeyStroke.getKeyStroke('Y', modifierKey));
        
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
                pa.pageSetup, pa.print, pa.printSuiteEqArea, pa.printGc,
                pa.runJavascriptScript, pa.runPythonScript, pa.prefs, pa.quit));
        
        add(makeMenu("Edit",KeyEvent.VK_E,
                pa.selectAll, pa.clearSelection,
                movePlotsItem, pa.resetLayout, pa.editSampleParameters,
                pa.setTreatType,
                pa.copyStepSelection, pa.pasteStepSelection,
                makeMenu("Rotate/invert samples", KeyEvent.VK_F,
                        pa.flipSampleX, pa.flipSampleY, pa.flipSampleZ,
                        pa.invertSamples),
                makeMenu("Edit sites", KeyEvent.VK_I,
                        pa.setSiteName, pa.setSitesFromSampleNames,
                        pa.setSitesByDepth, pa.clearSites),
                pa.hideSelectedSteps, pa.unhideAllSteps,
                pa.showCustomFlagsWindow, pa.showCustomNotesWindow,
                pa.rescaleMagSus, pa.convertDiscreteToContinuous,
                pa.alignSectionDeclinations));
        
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
                        pa.clearAmsCalcs)
                , pa.calculateRpi
        ));
        add(makeMenu("Window", KeyEvent.VK_W,
                new WindowMenuItem("Data table", KeyEvent.VK_D) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    JFrame window(PuffinApp a) {return a.getTableWindow();}},
                new WindowMenuItem("Site equal-area plot", KeyEvent.VK_I) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    JFrame window(PuffinApp a) {return a.getSiteEqAreaWindow();}},
                new WindowMenuItem("Suite equal-area plot", KeyEvent.VK_U) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    JFrame window(PuffinApp a) {return a.getSuiteEqAreaWindow();}}));
        add(makeMenu("Help", KeyEvent.VK_H,
                pa.openPuffinWebsite, pa.openCiteWindow, pa.about));
    }

    private class AnchorItem extends JCheckBoxMenuItem {
        private static final long serialVersionUID = 1L;
        AnchorItem() {
            super("PCA anchored");
            setToolTipText("If this item is checked, "
                    + "subsequent PCA analyses will be anchored");
            /*
             * If an ItemListener is used here instead of an 
             * ActionListener, it will also be called when the item
             * is set programmatically (e.g. to reflect a state 
             * taken from a new current sample). An ActionListener
             * will only be activated when the state is changed by
             * the user.
             */
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    for (Sample s: app.getSelectedSamples()) {
                        s.setPcaAnchored(isSelected());
                        s.doPca(app.getCorrection());
                    }
                    app.updateDisplay();
                }
            });
            setAccelerator(KeyStroke.getKeyStroke('T', modifierKey));
            setMnemonic(KeyEvent.VK_N);
        }

        @Override
        public boolean getState() {
                Sample s = app.getSample();
                return s != null ? s.isPcaAnchored() : false;
            }
    }

    /**
     * Alerts the menu bar that the current sample has changed.
     * This allows any stateful menu items to be changed.
     */
    public void sampleChanged() {
        Sample s = app.getSample();
        if (s != null) anchorItem.setSelected(s.isPcaAnchored());
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
            recentFilesMenu.add(noRecentFiles);
        for (int i=0; i<recentFileNames.length; i++) {
            final int index = i;
            JMenuItem item = recentFilesMenu.add(new AbstractAction() {
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
