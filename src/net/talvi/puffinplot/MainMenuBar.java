package net.talvi.puffinplot;

import java.awt.Toolkit;
import java.awt.event.InputEvent;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class MainMenuBar extends JMenuBar {

    private static final long serialVersionUID = 1L;

    // control or apple key as appropriate
    private static final int modifierKey =
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    
    public MainMenuBar() {
        JMenu fileMenu = new JMenu("File");

        PuffinActions actions = PuffinApp.app.actions;
        MenuItemDef[] fileItems = {
            new MenuItemDef(actions.open, 'O', 0, true),
            new MenuItemDef(actions.pageSetup, 'P', InputEvent.SHIFT_MASK, true),
            new MenuItemDef(actions.print, 'P', 0, true),
            new MenuItemDef(actions.prefs, ',', 0, false),
            new MenuItemDef(actions.quit, 'Q', 0, false),
        };
        
        for (MenuItemDef def: fileItems) def.addToMenu(fileMenu);
        
        JMenu plotMenu = new JMenu("Plot");
        JMenuItem doPcaItem = new JMenuItem(PuffinApp.app.actions.pca);
        doPcaItem.setAccelerator(KeyStroke.getKeyStroke('R', modifierKey, false));
        plotMenu.add(doPcaItem);
        final JCheckBoxMenuItem pcaItem = new JCheckBoxMenuItem("Anchor PCA")
        {
            @Override
            public boolean getState() {
                return PuffinApp.app.getPrefs().isPcaAnchored();
            }
        };
        plotMenu.add(pcaItem);
        pcaItem.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                PuffinApp.app.getPrefs().setPcaAnchored(!PuffinApp.app.getPrefs().isPcaAnchored());
            }
        });
        
        // plotMenu.add(new JCheckBoxMenuItem("Lock axis scale"));
        
        JMenu windowMenu = new JMenu("Window");
        final JCheckBoxMenuItem dt = new DataTableItem();
        windowMenu.add(dt);
        
        add(fileMenu);
        add(plotMenu);
        add(windowMenu);
    }
    
    private static class DataTableItem extends JCheckBoxMenuItem {
        public DataTableItem() {
            super("Data table");
            addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent arg0) {
                PuffinApp.app.getTableWindow().setVisible(DataTableItem.super.isSelected());
                }
            });
            PuffinApp.app.getTableWindow().addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    setSelected(false); }
            });
            }
            
            @Override
            public boolean isSelected() {
                return PuffinApp.app != null && PuffinApp.app.getTableWindow() != null
                        ? PuffinApp.app.getTableWindow().isVisible()
                        : false;
            }
        @Override
            public boolean getState() {
                return isSelected();
            }
        @Override
            public Object[] getSelectedObjects() {
                return isSelected() ? new Object[] { getText() } : null;
            }
    }
    
    private static class MenuItemDef {
        private Action action;
        private char shortcut;
        private boolean onMac;
        private int mask;
        
        MenuItemDef(Action action, char shortcut, int mask, boolean onMac) {
            super();
            this.action = action;
            this.shortcut = shortcut;
            this.mask = mask;
            this.onMac = onMac;
        }
        
        void addToMenu(JMenu menu) {
            if (onMac || !PuffinApp.MAC_OS_X) {
                JMenuItem item = new JMenuItem(action);
                item.setAccelerator(KeyStroke.getKeyStroke(shortcut,
                        modifierKey | mask, false));
                menu.add(item);
            }
        }
    }
}
