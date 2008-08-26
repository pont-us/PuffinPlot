package net.talvi.puffinplot;

import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.TwoGeeField;

/**
 *
 * @author pont
 */
public class TableWindow extends JFrame {

    private static class DataTableModel extends AbstractTableModel {
          private static final long serialVersionUID = 1L;
          
        public int getColumnCount() {
            return TwoGeeField.values().length - 1;
        }
        
        @Override
        public String getColumnName(int c) {
            return TwoGeeField.values()[c].toString().toLowerCase();
        }
          
        public int getRowCount() {
            if (PuffinApp.app != null &&
                    PuffinApp.app.getCurrentSample() != null)
                return PuffinApp.app.getCurrentSample().getNumData();
            else return 0;
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return TwoGeeField.values()[c].getClass();
        }

        public Object getValueAt(int row, int col) {
            try {
                Datum d = PuffinApp.app.getCurrentSample().getDatum(row);
                return d.getValue(TwoGeeField.values()[col]);
            } catch (NullPointerException e) {
                throw new RuntimeException("row " + row + " col " + col, e);
            }
        }
    }
    
    static class TablePanel extends JPanel {
        
    public TablePanel() {
        super(new GridLayout(1, 0));

        final JTable table = new JTable(new DataTableModel());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setPreferredScrollableViewportSize(new Dimension(500, 70));

        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        //Add the scroll pane to this panel.
        add(scrollPane);
        }
    }

    public TableWindow() {
        // JFrame.setDefaultLookAndFeelDecorated(true);
        TablePanel newContentPane = new TablePanel();
        newContentPane.setOpaque(true); //content panes must be opaque
        setContentPane(newContentPane);
        pack();
        // setVisible(true);
    }
    
}
