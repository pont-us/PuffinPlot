package net.talvi.puffinplot.window;

import net.talvi.puffinplot.*;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import net.talvi.puffinplot.data.Sample;
import static net.talvi.puffinplot.PuffinActions.PuffinAction;

public class MainMenuBar extends JMenuBar {

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
                if (!puffinAction.excludeFromMenu())
                    menu.add(puffinAction);
            } else if (thing instanceof Action) {
                menu.add((Action) thing);
            } else if (thing instanceof JMenuItem) {
                menu.add((JMenuItem) thing);
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
                makeMenu("Export calculations", pa.exportCalcsSample,
                pa.exportCalcsSite, pa.exportCalcsSuite),
                pa.pageSetup, pa.print, pa.printFisher, new OldSquidItem(),
                pa.prefs, pa.quit));
        add(makeMenu("Edit", pa.selectAll, movePlotsItem, pa.resetLayout,
                pa.editCorrections, pa.copyPointSelection, pa.flipSample,
                pa.hideSelectedPoints, pa.unhideAllPoints,
                pa.useAsEmptySlot, pa.unsetEmptySlot,
                useEmptyItem));
        add(makeMenu("Calculations",
                pa.pcaOnSelection, anchorItem = new AnchorItem(),
                pa.fisher, pa.fisherBySite, pa.fisherOnSuite,
                pa.mdf, pa.clear));
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
                            openFiles(recent.getFilesAndReorder(index), true);
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
    
    private class OldSquidItem extends JCheckBoxMenuItem {

        public OldSquidItem() {
            super("Use old SQUID orientations");
            final JCheckBoxMenuItem item = this;
            addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    setValue(item.isSelected());
                }
            });
            setSelected(getValue());
        }

        @Override
        public boolean getState() {
            return getValue();
        }

        private void setValue(boolean b) {
            app.getPrefs().setUseOldSquidOrientations(b);
        }

        private boolean getValue() {
            return app.getPrefs().isUseOldSquidOrientations();
        }
    }
}