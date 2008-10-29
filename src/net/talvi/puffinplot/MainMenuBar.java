package net.talvi.puffinplot;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import net.talvi.puffinplot.PuffinApp.Prefs;

public class MainMenuBar extends JMenuBar {

    private static final long serialVersionUID = 1L;

    // control or apple key as appropriate
    private static final int modifierKey =
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    
    private void addSimpleItem(JMenu menu, Action action, char key) {
        JMenuItem item = new JMenuItem(action);
        menu.add(item);
        item.setAccelerator(KeyStroke.getKeyStroke(key, modifierKey, false));
    }
    
    public MainMenuBar() {
        final JMenu fileMenu = new JMenu("File");
        final PuffinActions actions = PuffinApp.getApp().actions;

        MenuItemDef[] fileItems = {
            new MenuItemDef(actions.open, 'O', 0, true),
            new MenuItemDef(actions.pageSetup, 'P', InputEvent.SHIFT_MASK, true),
            new MenuItemDef(actions.print, 'P', 0, true),
            new MenuItemDef(actions.prefs, ',', 0, false),
            new MenuItemDef(actions.quit, 'Q', 0, false),
        };
        
        for (MenuItemDef def: fileItems) def.addToMenu(fileMenu);
        
        JMenu editMenu = new JMenu("Edit");
        addSimpleItem(editMenu, actions.selectAll, 'S');
        // Can't use ctrl-A, it's already "select all samples in this suite"
        final JCheckBoxMenuItem movePlotsItem = new JCheckBoxMenuItem("Move plots")
        {
            @Override
            public boolean isSelected() {
                try {
                    return PuffinApp.getApp().getMainWindow().graphDisplay.isDragPlotMode();
                } catch (NullPointerException e) {
                    return false;
                }
            }
        };
        editMenu.add(movePlotsItem);
        movePlotsItem.addActionListener(
                new ActionListener() {
      public void actionPerformed(ActionEvent event) {

                GraphDisplay gd = PuffinApp.getApp().getMainWindow().graphDisplay;
                gd.setDragPlotMode(!gd.isDragPlotMode());
                gd.repaint();

      } }
        );
        
        JMenu calcMenu = new JMenu("Calculate");
        addSimpleItem(calcMenu, actions.pca, 'R');
        
        final JCheckBoxMenuItem anchorItem = new JCheckBoxMenuItem("Anchor PCA")
        {
            @Override
            public boolean getState() {
                return PuffinApp.getApp().getPrefs().isPcaAnchored();
            }
        };
        calcMenu.add(anchorItem);
        anchorItem.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                Prefs prefs = PuffinApp.getApp().getPrefs();
                prefs.setPcaAnchored(!prefs.isPcaAnchored());
            }
        });
        
        addSimpleItem(calcMenu, actions.fisher, 'F');
        addSimpleItem(calcMenu, actions.clear, 'Z');
        
        // calcMenu.add(new JCheckBoxMenuItem("Lock axis scale"));
        
        JMenu windowMenu = new JMenu("Window");
        final JCheckBoxMenuItem dt = new DataTableItem();
        windowMenu.add(dt);
        
        add(fileMenu);
        add(editMenu);
        add(calcMenu);
        add(windowMenu);
    }
    
    private static class DataTableItem extends JCheckBoxMenuItem {
        public DataTableItem() {
            super("Data table");
            addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent arg0) {
                    PuffinApp.getApp().getTableWindow().setVisible(DataTableItem.super.isSelected());
                }
            });
            PuffinApp.getApp().getTableWindow().addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    setSelected(false); }
            });
            }
            
            @Override
            public boolean isSelected() {
                return PuffinApp.getApp() != null && PuffinApp.getApp().getTableWindow() != null
                        ? PuffinApp.getApp().getTableWindow().isVisible()
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
