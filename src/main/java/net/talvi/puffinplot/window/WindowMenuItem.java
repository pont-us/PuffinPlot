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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;

class WindowMenuItem extends JCheckBoxMenuItem {

    private final Supplier<JFrame> windowSupplier;

    /**
     * The constructor takes a window *supplier* rather than a window, since
     * this is safer e.g. if the window itself is created after this menu
     * item, or if it's recreated later on.
     * 
     * @param name menu item name
     * @param mnemonic mnemonic (short-cut)
     * @param windowSupplier function supplying the window to open/close
     */
    public WindowMenuItem(String name, int mnemonic,
            Supplier<JFrame> windowSupplier) {
        super(name);
        Objects.requireNonNull(windowSupplier);
        this.windowSupplier = windowSupplier;
        setMnemonic(mnemonic);
        addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent arg0) {
                getWindow().setVisible(WindowMenuItem.super.isSelected());
                getWindow().invalidate();
                getWindow().repaint(100);
            }
        });
        getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setSelected(false);
            }
        });
    }

    /**
     * We need a null check here because this gets called (via super(name))
     * before the constructor can set the windowSupplier field.
     * 
     * @return the window returned by the window supplier, or null
     * if no window supplier is set
     */
    private JFrame getWindow() {
        return windowSupplier == null ? null : windowSupplier.get();
    }
    
    @Override
    public boolean isSelected() {
        return getWindow() != null ? getWindow().isVisible() : false;
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
