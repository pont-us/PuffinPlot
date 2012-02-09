/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
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
