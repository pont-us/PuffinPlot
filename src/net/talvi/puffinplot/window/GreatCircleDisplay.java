package net.talvi.puffinplot.window;

import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.plots.GreatCirclePlot;
import net.talvi.puffinplot.plots.Plot;

public class GreatCircleDisplay extends GraphDisplay implements Printable {

    public GreatCircleDisplay() {
        super();
        zoomTransform = AffineTransform.getScaleInstance(1.0, 1.0);

        PlotParams params = new PlotParams() {
            public Sample getSample() {
                return PuffinApp.getInstance().getSample(); }
            public Correction getCorrection() {
                throw new UnsupportedOperationException(); }
            public MeasurementAxis getAxis() {
                throw new UnsupportedOperationException(); }
        };

        Plot plot = new GreatCirclePlot(this, params,
                new Rectangle2D.Double(50, 50, 450, 450));
        plot.setVisible(true);
        plots.put(plot.getName(), plot);
    }

    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
            throws PrinterException {
        if (pageIndex > 0) return NO_SUCH_PAGE;
        else {
            printPlots(pageFormat, graphics);
            return PAGE_EXISTS;
        }
    }
}
