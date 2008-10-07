package net.talvi.puffinplot.plots;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BoxLayout;
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
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.PcaValues;
import net.talvi.puffinplot.data.Sample;

public class DataDisplay extends Plot {

    private static final long serialVersionUID = 1L;
    private JLabel summaryLine;
    private JLabel pcaLine;
    
    
    class MyRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        public MyRenderer() {
            super();
            setBorder(new EmptyBorder(0,0,0,0));
            setHorizontalAlignment(SwingConstants.CENTER);
        }

    }
    
    public DataDisplay(final PlotParams params) {
        super(params);
        setOpaque(false);
        setMaximumSize(new Dimension(1200, 800));
        setPreferredSize(new Dimension(1200, 800));

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        TableModel dataModel = new AbstractTableModel() {
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
                }
                return new Integer(row*col);
                }
        };
        JTable table = new JTable(dataModel);

        setBorder(new EmptyBorder(10,10,10,10));
        table.setVisible(true);
        table.setBorder(new EmptyBorder(0,0,0,0));
        // table.setDefaultRenderer(Object.class, new MyRenderer());
        table.setShowGrid(false);
        
        for(int i = 0; i < 4; i++)
        {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setHeaderRenderer(new MyRenderer());
            column.setHeaderValue(i==0 ? "temp." : i==1 ? "dec." : i==2 ? "inc." : "int. e-7");
        }
        
        summaryLine = new JLabel();
        pcaLine = new JLabel();
        add(summaryLine);
        add(pcaLine);
        JPanel p = new JPanel();
        add(p);
        p.setLayout(new BorderLayout());
        JTableHeader h = table.getTableHeader();
        h.setBackground(Color.WHITE);
        h.setBorder(new EmptyBorder(0,0,0,0));
        table.setGridColor(Color.WHITE);
        table.setIntercellSpacing(new Dimension(0,0));
        p.add(h, BorderLayout.PAGE_START);
        p.add(table, BorderLayout.CENTER);
        setVisible(true);
    }
    
    @Override
    public void paint(Graphics g1) {
        super.paint(g1);
        Graphics2D g = (Graphics2D) g1;
        g.setRenderingHints(Plot.renderingHints);
        Sample sample = params.getSample();
        if (sample==null) return;
        
        String line = null;
        switch (params.getMeasType()) {
        case DISCRETE: line = "Sample " + sample.getName();
            break;
        case CONTINUOUS: line = "Depth " + sample.getDepth();
            break;
        }
        line = line + " / correction " + params.getCorrection();
        summaryLine.setText(line);
        
        PcaValues pca = sample.getPca();
        if (pca != null) pcaLine.setText("PCA: "+pca.getDecInc()+" "+pca.getMads());
        else pcaLine.setText("");

    }    
}
