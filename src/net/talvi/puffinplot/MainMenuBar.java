package net.talvi.puffinplot;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
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
import net.talvi.puffinplot.PuffinPrefs;
import net.talvi.puffinplot.PuffinPrefs.RecentFile;

public class MainMenuBar extends JMenuBar {

    private static final long serialVersionUID = 1L;

    final static private JMenuItem noRecentFiles = new JMenuItem("No recent files");
    // control or apple key as appropriate
    private static final int modifierKey =
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    private JMenu recentFilesMenu;
        
    public MainMenuBar() {
        final JMenu fileMenu = new JMenu("File");
        final PuffinApp app = PuffinApp.getApp();
        final PuffinActions actions = app.getActions();

        recentFilesMenu = new JMenu("Open recent file");

        noRecentFiles.setEnabled(false);
        updateRecentFiles();
        
        MenuItemDef[] fileItems = {
            new ActionItemDef(actions.open, 'O', 0, true),
            new SubmenuDef(recentFilesMenu),
            new ActionItemDef(actions.saveCalcs, 'S', 0, true),
            new ActionItemDef(actions.save, 'S', InputEvent.SHIFT_DOWN_MASK, true),
            new ActionItemDef(actions.pageSetup, 'P', InputEvent.SHIFT_DOWN_MASK, true),
            new ActionItemDef(actions.print, 'P', 0, true),
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
                try {
                    return app.getMainWindow().getGraphDisplay().isDragPlotMode();
                } catch (NullPointerException e) {
                    return false;
                }
            }
        };
        editMenu.add(movePlotsItem);
        movePlotsItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        GraphDisplay gd = app.getMainWindow().getGraphDisplay();
                        gd.setDragPlotMode(!gd.isDragPlotMode());
                        gd.repaint();
      } }
        
        );
        
        JMenu calcMenu = new JMenu("Calculate");
        addSimpleItem(calcMenu, actions.pca, 'R');
        addSimpleItem(calcMenu, actions.pcaOnSelection, 'T');
        
        final JCheckBoxMenuItem anchorItem = new JCheckBoxMenuItem("Anchor PCA")
        {
            @Override
            public boolean getState() {
                return app.getPrefs().isPcaAnchored();
            }
        };
        calcMenu.add(anchorItem);
        anchorItem.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                PuffinPrefs prefs = app.getPrefs();
                prefs.setPcaAnchored(!prefs.isPcaAnchored());
            }
        });
        
        addSimpleItem(calcMenu, actions.fisher, 'F');
        addSimpleItem(calcMenu, actions.fisherOnPca, 'G');
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
    
    private void addSimpleItem(JMenu menu, Action action, char key) {
        JMenuItem item = new JMenuItem(action);
        menu.add(item);
        item.setAccelerator(KeyStroke.getKeyStroke(key, modifierKey, false));
    }
  
    void updateRecentFiles() {
        recentFilesMenu.removeAll();
        final List<RecentFile> recentFiles =
                PuffinApp.getApp().getPrefs().getRecentFiles();
        if (recentFiles.size() == 0)
            recentFilesMenu.add(noRecentFiles);
        for (final PuffinPrefs.RecentFile recent : recentFiles) {
            recentFilesMenu.add(new AbstractAction() {
                public void actionPerformed(ActionEvent arg0) {
                    PuffinApp.getApp().openFiles(recent.getFiles());
                }
                @Override
                public Object getValue(String key) {
                    return (NAME.equals(key)) ?
                        recent.getName() :
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
        
        ActionItemDef(Action action, char shortcut, int mask, boolean onMac) {
            super();
            this.action = action;
            this.shortcut = shortcut;
            this.mask = mask;
            this.onMac = onMac;
        }
        
        public void addToMenu(JMenu menu) {
            if (onMac || !PuffinApp.MAC_OS_X) {
                JMenuItem item = new JMenuItem(action);
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
