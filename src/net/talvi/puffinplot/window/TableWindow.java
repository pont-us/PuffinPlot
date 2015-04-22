/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2015 Pontus Lurcock.
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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.DatumField;

/**
 * A window which shows all the data for a sample in tabular form.
 * 
 * @author pont
 */
public class TableWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    private JTable table;
    private DataTableModel tableModel;
    
    /**
     * Creates a new table window.
     */
    public TableWindow() {
        // JFrame.setDefaultLookAndFeelDecorated(true);
        TablePanel newContentPane = new TablePanel();
        newContentPane.setOpaque(true); //content panes must be opaque
        setContentPane(newContentPane);
        newContentPane.setVisible(true);
        pack();
    }
    
    private static class DataTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final List<TableModelListener> listeners = new LinkedList<>();
        
        @Override
        public void addTableModelListener(TableModelListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeTableModelListener(TableModelListener listener) {
            listeners.remove(listener);
        }

        public void fireModelChangedEvent() {
            for (TableModelListener listener: listeners) {
                listener.tableChanged(new TableModelEvent(this));
            }
        }

        @Override
        public int getColumnCount() {
            return DatumField.values().length - 1;
        }
        
        @Override
        public String getColumnName(int c) {
            return DatumField.values()[c].toString().toLowerCase();
        }
          
        @Override
        public int getRowCount() {
            if (PuffinApp.getInstance() != null &&
                    PuffinApp.getInstance().getSample() != null)
                return PuffinApp.getInstance().getSample().getData().size();
            else return 0;
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return DatumField.values()[c].getClass();
        }

        @Override
        public Object getValueAt(int row, int col) {
            try {
                Datum d = PuffinApp.getInstance().getSample().getData().get(row);
                return d.getValue(DatumField.values()[col]);
            } catch (NullPointerException e) {
                throw new RuntimeException("row " + row + " col " + col, e);
            }
        }
    }
    
    private class TablePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        public TablePanel() {
            super(new GridLayout(1, 0));
            table = new JTable(tableModel = new DataTableModel());
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.setPreferredScrollableViewportSize(new Dimension(500, 70));
            JScrollPane scrollPane = new JScrollPane(table);
            add(scrollPane);
        }
    }

    /**
     * Forces the data table to update its display.
     * This allows it to be redrawn when the current sample changes.
     * 
     */
    public void dataChanged() {
        tableModel.fireModelChangedEvent();
        repaint(100);
    }
}
