package net.talvi.puffinplot;

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
import net.talvi.puffinplot.data.TwoGeeField;

public class TableWindow extends JFrame {
    private JTable table;
    private DataTableModel tableModel;

    private static class DataTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private List<TableModelListener> listeners = new LinkedList<TableModelListener>();
        
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

        public int getColumnCount() {
            return TwoGeeField.values().length - 1;
        }
        
        @Override
        public String getColumnName(int c) {
            return TwoGeeField.values()[c].toString().toLowerCase();
        }
          
        public int getRowCount() {
            if (PuffinApp.getInstance() != null &&
                    PuffinApp.getInstance().getSample() != null)
                return PuffinApp.getInstance().getSample().getAllData().size();
            else return 0;
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return TwoGeeField.values()[c].getClass();
        }

        public Object getValueAt(int row, int col) {
            try {
                Datum d = PuffinApp.getInstance().getSample().getAllData().get(row);
                return d.getValue(TwoGeeField.values()[col]);
            } catch (NullPointerException e) {
                throw new RuntimeException("row " + row + " col " + col, e);
            }
        }
    }
    
    class TablePanel extends JPanel {

        public TablePanel() {
            super(new GridLayout(1, 0));
            table = new JTable(tableModel = new DataTableModel());
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.setPreferredScrollableViewportSize(new Dimension(500, 70));
            JScrollPane scrollPane = new JScrollPane(table);
            add(scrollPane);
        }
    }

    public void dataChanged() {
        tableModel.fireModelChangedEvent();
        repaint(100);
    }

    public TableWindow() {
        // JFrame.setDefaultLookAndFeelDecorated(true);
        TablePanel newContentPane = new TablePanel();
        newContentPane.setOpaque(true); //content panes must be opaque
        setContentPane(newContentPane);
        newContentPane.setVisible(true);
        pack();
    }
    
}
