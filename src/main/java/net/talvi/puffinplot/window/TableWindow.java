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

import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.plots.PlotParams;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.TreatmentParameter;

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
     * 
     * @param params plot parameters (to control the currently displayed data)
     */
    public TableWindow(PuffinApp app) {
        final TablePanel newContentPane = new TablePanel(app);
        newContentPane.setOpaque(true); //content panes must be opaque
        setContentPane(newContentPane);
        newContentPane.setVisible(true);
        pack();
    }
        
    private class TablePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        public TablePanel(PuffinApp app) {
            super(new GridLayout(1, 0));
            final PlotParams params = app.getPlotParams();
            table = new JTable(tableModel = new DataTableModel(params));
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.setPreferredScrollableViewportSize(new Dimension(500, 70));
            table.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (event.getButton() == MouseEvent.BUTTON3) {
                        final int row = table.rowAtPoint(event.getPoint());
                        params.getSample().getTreatmentSteps().get(row).
                                toggleSelected();
                        table.repaint();
                        app.updateDisplay();
                    }
                }
            });
            table.setDefaultRenderer(
                    Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(
                        JTable table, Object value, boolean isSelected,
                        boolean hasFocus, int row, int column) {
                    final Component component =
                            super.getTableCellRendererComponent(
                                    table, value, isSelected, hasFocus,
                                    row, column);
                    final boolean selectedForAnalysis =
                            params.getSample().getTreatmentSteps().
                                    get(row).isSelected();
                    // component.setForeground(selectedForAnalysis ? Color.RED : Color.BLACK);
                    if (selectedForAnalysis) {
                        component.setFont(component.getFont().deriveFont(Font.BOLD));
                    }
                    return component;
                }
            });
            final JScrollPane scrollPane = new JScrollPane(table);
            add(scrollPane);
        }
    }
    
    private static class DataTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final List<TableModelListener> listeners = new LinkedList<>();
        private final PlotParams params;

        public DataTableModel(PlotParams params) {
            this.params = params;
        }

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
            return TreatmentParameter.values().length - 1;
        }
        
        @Override
        public String getColumnName(int c) {
            return TreatmentParameter.values()[c].toString().toLowerCase();
        }
          
        @Override
        public int getRowCount() {
            if (params != null && params.getSample() != null) {
                return params.getSample().getTreatmentSteps().size();
            } else {
                return 0;
            }
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return TreatmentParameter.values()[c].getClass();
        }

        @Override
        public Object getValueAt(int row, int col) {
            try {
                final TreatmentStep step =
                        params.getSample().getTreatmentSteps().get(row);
                return step.getValue(TreatmentParameter.values()[col]);
            } catch (NullPointerException e) {
                throw new RuntimeException("row " + row + " col " + col, e);
            }
        }

    }

    /**
     * Forces the data table to update its display. This allows it to be redrawn
     * when the current sample changes.
     */
    public void dataChanged() {
        tableModel.fireModelChangedEvent();
        repaint(100);
    }
}
