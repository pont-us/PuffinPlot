package net.talvi.puffinplot.plots;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.PcaValues;
import net.talvi.puffinplot.data.Sample;

public class DataDisplay extends JTable {

    private static final long serialVersionUID = 1L;
    
    private static class MyRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        public MyRenderer() {
            super();
            setBorder(new EmptyBorder(0,0,0,0));
            setHorizontalAlignment(SwingConstants.CENTER);
        }

    }
    
    public DataDisplay(final PlotParams params) {
        TableModel myDataModel = new AbstractTableModel() {
          private static final long serialVersionUID = 1L;
          public int getColumnCount() { return 4; }
            public int getRowCount() {
                if (params.getSample()==null) return 0;
                return params.getSample().getNumData();
                }
            @Override
            public Class<?> getColumnClass(int c) {
                return getValueAt(0, c).getClass();
            }
            public Object getValueAt(int row, int col) {
                Datum d = params.getSample().getDatum(row);
                Correction corr = params.getCorrection();
                switch (col) {
                case 0: return d.getDemagLevel();
                case 1: return d.getPoint(corr).decDegrees();
                case 2: return d.getPoint(corr).incDegrees();
                case 3: return d.getIntensity() * 1e7;
                default: return -1;
                }
                }
        };
        setModel(myDataModel);
        setVisible(true);
        setBorder(new EmptyBorder(0,0,0,0));
        // table.setDefaultRenderer(Object.class, new MyRenderer());
        setShowGrid(false);
        
        for(int i = 0; i < 4; i++)
        {
            TableColumn column = getColumnModel().getColumn(i);
            column.setHeaderRenderer(new MyRenderer());
            column.setHeaderValue(i==0 ? "demag." : i==1 ? "dec." : i==2 ? "inc." : "int. e-7");
        }
        
        JTableHeader h = getTableHeader();
        h.setBackground(Color.WHITE);
        h.setBorder(new EmptyBorder(0,0,0,0));
        setGridColor(Color.WHITE);
        setIntercellSpacing(new Dimension(0,0));
        setVisible(true);
    }
    
       @Override
        public void update(Graphics g) {
            AffineTransform savedTransform = null;
            Graphics2D g2 = (Graphics2D) getGraphics();
            if (!PuffinApp.getApp().mainWindow.graphDisplay.alreadyTransformed) {
                savedTransform = g2.getTransform();
                g2.transform(PuffinApp.getApp().mainWindow.graphDisplay.zoomTransform);
            }
            super.update(g2);
            if (savedTransform != null) {
                g2.setTransform(savedTransform);
            }
        }
        
        @Override
        protected void processMouseEvent(MouseEvent e) {
            super.processMouseEvent(e);
        }
    
        public void processMouseEventPublic(MouseEvent e) {
            Point2D newPoint =
                    PuffinApp.getApp().mainWindow.graphDisplay.
                    getAntiZoom().transform(e.getPoint(), null);
            e.translatePoint((int) newPoint.getX() - e.getX(),
                    (int) newPoint.getY() - e.getY());
            e.translatePoint((int) -DataDisplay.this.getBounds().getMinX(), (int) -DataDisplay.this.getBounds().getMinY());
            processMouseEvent(e);
    }
    
    }
