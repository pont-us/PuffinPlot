package net.talvi.puffinplot;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.talvi.puffinplot.data.Sample;

public class MainMenuBar extends JMenuBar {

    private static final long serialVersionUID = 1L;

    final static private JMenuItem noRecentFiles = new JMenuItem("No recent files");
    // control or apple key as appropriate
    private static final int modifierKey =
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    private JMenu recentFilesMenu;
    final private JCheckBoxMenuItem anchorItem;
        
    public MainMenuBar() {
        final JMenu fileMenu = new JMenu("File");
        final PuffinApp app = PuffinApp.getInstance();
        final PuffinActions actions = app.getActions();

        recentFilesMenu = new JMenu("Open recent file");

        noRecentFiles.setEnabled(false);
        updateRecentFiles();
        
        MenuItemDef[] fileItems = {
            new ActionItemDef(actions.open, 'O', 0, true),
            new SubmenuDef(recentFilesMenu),
            new ActionItemDef(actions.save, 'S', InputEvent.SHIFT_DOWN_MASK, true),
            new ActionItemDef(actions.saveCalcsSample, true),
            new ActionItemDef(actions.saveCalcsSite, true),
            new ActionItemDef(actions.saveCalcsSuite, true),
            new ActionItemDef(actions.pageSetup, 'P', InputEvent.SHIFT_DOWN_MASK, true),
            new ActionItemDef(actions.print, 'P', 0, true),
            new ActionItemDef(actions.printFisher, 'I', 0, true),
            new ActionItemDef(actions.prefs, ',', 0, false),
            new ActionItemDef(actions.quit, 'Q', 0, false)
        };
        
        for (MenuItemDef def: fileItems) def.addToMenu(fileMenu);
        
        JMenu editMenu = new JMenu("Edit");
        addSimpleItem(editMenu, actions.selectAll, 'D');
        // Can't use ctrl-A, it's already "select all samples in this suite"
        final JCheckBoxMenuItem movePlotsItem = new JCheckBoxMenuItem("Move plots")
        {
            @Override
            public boolean isSelected() {
                MainWindow w = app.getMainWindow();
                return w==null ? false : w.getGraphDisplay().isDragPlotMode();
            }
        };
        movePlotsItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                GraphDisplay gd = app.getMainWindow().getGraphDisplay();
                gd.setDragPlotMode(!gd.isDragPlotMode());
                gd.repaint();
            }
        });
        editMenu.add(movePlotsItem);
        addSimpleItem(editMenu, actions.resetLayout, '\u0000');
        addSimpleItem(editMenu, actions.editCorrections, '\u0000');
        addSimpleItem(editMenu, actions.flipSample, '\u0000');
        addSimpleItem(editMenu, actions.useAsEmptySlot, '\u0000');
        addSimpleItem(editMenu, actions.unsetEmptySlot, '\u0000');
        final JCheckBoxMenuItem useEmptyItem = new JCheckBoxMenuItem("Apply empty correction")
        {
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
        editMenu.add(useEmptyItem);

        JMenu calcMenu = new JMenu("Calculate");
        addSimpleItem(calcMenu, actions.pcaOnSelection, 'R');
        
        anchorItem = new JCheckBoxMenuItem("Anchor PCA")
        {
            @Override
            public boolean getState() {
                return app.getSample().isPcaAnchored();
            }
        };
        calcMenu.add(anchorItem);
        anchorItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                for (Sample s: app.getSelectedSamples()) {
                    s.setPcaAnchored(anchorItem.isSelected());
                    s.doPca();
                }
                app.updateDisplay();
            }
        });
        anchorItem.setAccelerator(KeyStroke.getKeyStroke('T', modifierKey));
        
        addSimpleItem(calcMenu, actions.fisher, 'F');
        addSimpleItem(calcMenu, actions.fisherBySite, 'G');
        addSimpleItem(calcMenu, actions.fisherBySample, 'H');
        addSimpleItem(calcMenu, actions.clear, 'Z');
        
        // calcMenu.add(new JCheckBoxMenuItem("Lock axis scale"));
        
        JMenu windowMenu = new JMenu("Window");
        windowMenu.add(new WindowMenuItem("Data table") 
        { JFrame window(PuffinApp app) {return app.getTableWindow();}});
        windowMenu.add(new WindowMenuItem("Fisher EA plot") 
        { JFrame window(PuffinApp app) {return app.getFisherWindow();}});
        
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(app.getActions().about);
        
        add(fileMenu);
        add(editMenu);
        add(calcMenu);
        add(windowMenu);
        add(helpMenu);
    }

    void sampleChanged() {
        anchorItem.setSelected(PuffinApp.getInstance().getSample().isPcaAnchored());
    }
    
    private void addSimpleItem(JMenu menu, Action action, char key) {
        JMenuItem item = new JMenuItem(action);
        menu.add(item);
        if (key != '\u0000') item.setAccelerator(KeyStroke.getKeyStroke(key, modifierKey, false));
    }
  
    void updateRecentFiles() {
        recentFilesMenu.removeAll();
        final RecentFileList recent = PuffinApp.getInstance().getRecentFiles();
        final String[] recentFileNames = recent.getFilesetNames();
        if (recentFileNames.length == 0)
            recentFilesMenu.add(noRecentFiles);
        for (int i=0; i<recentFileNames.length; i++) {
            String fileName = recentFileNames[i];
            final int index  = i;
            recentFilesMenu.add(new AbstractAction() {
                public void actionPerformed(ActionEvent arg0) {
                    PuffinApp.getInstance().openFiles(recent.getFilesAndReorder(index), true);
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
    
    private static interface MenuItemDef {
        void addToMenu(JMenu menu);
    }
    
    private static class ActionItemDef implements MenuItemDef {
        private Action action;
        private char shortcut;
        private boolean onMac;
        private int mask;
        private boolean hasShortcut;
        
        ActionItemDef(Action action, char shortcut, int mask, boolean onMac) {
            super();
            this.action = action;
            this.shortcut = shortcut;
            this.mask = mask;
            this.onMac = onMac;
            this.hasShortcut = true;
        }

        ActionItemDef(Action action, boolean onMac) {
            super();
            this.action = action;
            this.onMac = onMac;
            this.hasShortcut = false;
        }
        
        public void addToMenu(JMenu menu) {
            if (onMac || !PuffinApp.MAC_OS_X) {
                JMenuItem item = new JMenuItem(action);
                if (hasShortcut)
                    item.setAccelerator(KeyStroke.getKeyStroke(shortcut,
                            modifierKey | mask, false));
                menu.add(item);
            }
        }
    }
    
    private static class SubmenuDef implements MenuItemDef {
        private JMenu submenu;

        SubmenuDef(JMenu submenu) {
            this.submenu = submenu;
        }
        
        public void addToMenu(JMenu menu) {
            menu.add(submenu);
        }
        
    }
    }
