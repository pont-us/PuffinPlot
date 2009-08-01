package net.talvi.puffinplot.window;

import net.talvi.puffinplot.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;

abstract class WindowMenuItem extends JCheckBoxMenuItem {

    public WindowMenuItem(String name) {
        super(name);
        addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent arg0) {
                window().setVisible(WindowMenuItem.super.isSelected());
                window().invalidate();
                window().repaint(100);
            }
        });
        window().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setSelected(false);
            }
        });
    }

    private JFrame window() {
        final PuffinApp app = PuffinApp.getInstance();
        return (app==null) ? null : window(app);
    }
    
    abstract JFrame window(PuffinApp app);
    
    @Override
    public boolean isSelected() {
        return window() != null ? window().isVisible() : false;
    }

    @Override
    public boolean getState() {
        return isSelected();
    }

    @Override
    public Object[] getSelectedObjects() {
        return isSelected() ? new Object[]{getText()} : null;
    }
}
