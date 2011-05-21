package net.talvi.puffinplot.window;

import java.util.logging.Level;
import net.talvi.puffinplot.*;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import net.talvi.puffinplot.PuffinActions.PuffinAction;
import net.talvi.puffinplot.data.Sample;

public class MainMenuBar extends JMenuBar {

    private static final Logger logger = Logger.getLogger("MainMenuBar");
    private static final long serialVersionUID = 1L;
    final static private JMenuItem noRecentFiles =
            new JMenuItem("No recent files");
    // control or apple key as appropriate
    private static final int modifierKey =
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    private JMenu recentFilesMenu;
    private final JCheckBoxMenuItem anchorItem;
    private final PuffinApp app = PuffinApp.getInstance();

    private static JMenu makeMenu(String name, Object... things) {
        JMenu menu = new JMenu(name);
        for (Object thing: things) {
            if (thing instanceof PuffinAction) {
                PuffinAction puffinAction = (PuffinAction) thing;
                if (!puffinAction.excludeFromMenu()) menu.add(puffinAction);
            } else if (thing instanceof Action) {
                menu.add((Action) thing);
            } else if (thing instanceof JMenuItem) {
                // JMenu is a subclass of JMenuItem so this handles submenus
                // as well as `raw' JMenuItems
                menu.add((JMenuItem) thing);
            } else {
                logger.log(Level.WARNING, "Unknown menu item {0}", thing);
            }
        }
        return menu;
    }

    public MainMenuBar() {
        final PuffinActions pa = app.getActions();
        recentFilesMenu = new JMenu("Open recent file");
        noRecentFiles.setEnabled(false);
        updateRecentFiles();

        final JCheckBoxMenuItem movePlotsItem =
                new JCheckBoxMenuItem("Move plots") {
                    @Override
                    public boolean isSelected() {
                        MainWindow w = app.getMainWindow();
                        return w != null ? w.getGraphDisplay().isDragPlotMode()
                                : false;
                    }
                };
        movePlotsItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                GraphDisplay gd = app.getMainWindow().getGraphDisplay();
                gd.setDragPlotMode(!gd.isDragPlotMode());
                gd.repaint();
            }
        });

        final JCheckBoxMenuItem useEmptyItem =
                new JCheckBoxMenuItem("Apply empty correction") {
                    @Override
                    public boolean isSelected() {
                        return app.isEmptyCorrectionActive();
                    }
                };
        useEmptyItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                app.setEmptyCorrectionActive(!app.isEmptyCorrectionActive());
                app.updateDisplay();
            }
        });
        useEmptyItem.setAccelerator(KeyStroke.getKeyStroke('Y', modifierKey));
        
        add(makeMenu("File", pa.open, recentFilesMenu, pa.save, pa.saveAs,
                pa.close,
                makeMenu("Export data", pa.exportCalcsSample,
                pa.exportCalcsSite, pa.exportCalcsSuite, pa.exportIrm),
                pa.pageSetup, pa.print, pa.printFisher, pa.printGc,
                pa.importAms, pa.exportSvg, pa.exportPrefs, pa.importPrefs,
                pa.prefs, pa.quit));
        add(makeMenu("Edit", pa.selectAll, pa.clearSelection,
                movePlotsItem, pa.resetLayout,
                pa.editCorrections, pa.copyPointSelection, pa.flipSample,
                pa.hideSelectedPoints, pa.unhideAllPoints,
                pa.useAsEmptySlot, pa.unsetEmptySlot,
                useEmptyItem, pa.showCustomFlagsWindow, pa.showCustomNotesWindow,
                pa.fixBartington));
        add(makeMenu("Calculations",
                pa.pcaOnSelection, anchorItem = new AnchorItem(),
                pa.fisherBySample, pa.fisherBySite, pa.fisherOnSuite,
                pa.mdf, pa.clear, pa.circleFit, pa.greatCircleAnalysis,
                pa.clearGcFit,
                pa.reversalTest, pa.bootAmsNaive, pa.bootAmsParam, pa.hextAms,
                pa.clearAmsCalcs));
        add(makeMenu("Window",
                new WindowMenuItem("Data table")
                { JFrame window(PuffinApp a) {return a.getTableWindow();}},
                new WindowMenuItem("Fisher EA plot")
                { JFrame window(PuffinApp a) {return a.getFisherWindow();}}));
        add(makeMenu("Help", pa.about));
    }

    private class AnchorItem extends JCheckBoxMenuItem {
        AnchorItem() {
            super("Anchor PCA");
            addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                for (Sample s: app.getSelectedSamples()) {
                    s.setPcaAnchored(isSelected());
                    s.doPca();
                }
                app.updateDisplay();
            }});
            setAccelerator(KeyStroke.getKeyStroke('T', modifierKey));
        }

        @Override
        public boolean getState() {
                Sample s = app.getSample();
                return s != null ? s.isPcaAnchored() : false;
            }
    }

    public void sampleChanged() {
        Sample s = PuffinApp.getInstance().getSample();
        if (s != null) anchorItem.setSelected(s.isPcaAnchored());
    }
    
    public void updateRecentFiles() {
        recentFilesMenu.removeAll();
        final RecentFileList recent = PuffinApp.getInstance().getRecentFiles();
        final String[] recentFileNames = recent.getFilesetNames();
        if (recentFileNames.length == 0)
            recentFilesMenu.add(noRecentFiles);
        for (int i=0; i<recentFileNames.length; i++) {
            final int index  = i;
            recentFilesMenu.add(new AbstractAction() {
                public void actionPerformed(ActionEvent arg0) {
                    PuffinApp.getInstance().
                            openFiles(recent.getFilesAndReorder(index));
                }
                @Override
                public Object getValue(String key) {
                    return (NAME.equals(key)) ?
                        recent.getFilesetNames()[index] :
                        super.getValue(key);
                }
            });
        }
    }
    
}
