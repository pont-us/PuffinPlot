package net.talvi.puffinplot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.plots.DataDisplay;
import net.talvi.puffinplot.plots.DemagPlot;
import net.talvi.puffinplot.plots.EqAreaPlot;
import net.talvi.puffinplot.plots.ZPlot;

public class GraphDisplay extends JPanel implements Printable {

    private static final long serialVersionUID = -5730958004698337302L;
    JPanel leftPanel;
    private PlotParams params;
    private Sample[] samples = null;
    private int printPageIndex = -1;

    GraphDisplay() {
        
        params = new PlotParams() {

            public Sample getSample() {
                return samples==null
                       ? PuffinApp.app.getCurrentSample()
                        : samples[printPageIndex];
            }

            public Correction getCorrection() {
                return PuffinApp.app.currentCorrection();
            }

            public MeasurementAxis getAxis() {
                return PuffinApp.app.mainWindow.controlPanel.getAxis();
            }

            public MeasType getMeasType() {
                return PuffinApp.app.getCurrentSuite().getMeasType();
            }
            
        };
        
        setPreferredSize(new Dimension(1200, 800));
        setMaximumSize(new Dimension(1200, 800));
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
//        leftPanel = new JPanel();
//        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
//        leftPanel.add(new DemagPlot());
//        leftPanel.add(new DataDisplay());        
//        leftPanel.setOpaque(true);
//        leftPanel.setBackground(Color.WHITE);
//        leftPanel.setVisible(true);
        leftPanel = new LeftPanel();
        add(leftPanel);
        EqAreaPlot eq = new EqAreaPlot(params);

        add(eq);
        add(new ZPlot(params));
        setOpaque(true);
        setBackground(Color.WHITE);
        setVisible(true);
    }

    public int print(Graphics graphics, PageFormat pf, int pageIndex) throws PrinterException {
        if (samples == null) samples = PuffinApp.app.getSelectedSamples();
        if (pageIndex >= samples.length) {
            samples = null; // we've finished printing
            return NO_SUCH_PAGE;
        }
        printPageIndex = pageIndex;
        Graphics2D g = (Graphics2D) graphics;
        g.translate(pf.getImageableX(), pf.getImageableY());
        double xScale = pf.getImageableWidth() / getWidth();
        double yScale = pf.getImageableHeight() / getHeight();
        double scale = Math.min(xScale, yScale);
        g.scale(scale, scale);
        leftPanel.setDoubleBuffered(false);
        printChildren(g);
        leftPanel.setDoubleBuffered(true);
        return PAGE_EXISTS;
    }
    
    private class LeftPanel extends JPanel {
        
        private static final long serialVersionUID = 1L;

        LeftPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setMaximumSize(new Dimension(400,800));
            setPreferredSize(new Dimension(400,800));
            add(new DemagPlot(params));
            add(new DataDisplay(params));       
            setOpaque(true);
            setBackground(Color.WHITE);
            setVisible(true);
        }
    }
}
