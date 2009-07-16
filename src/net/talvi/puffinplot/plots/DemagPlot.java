package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import java.util.prefs.Preferences;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.Sample;

public class DemagPlot extends Plot {

    public DemagPlot(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
    }

    public String getName() {
        return "demag";
    }

    @Override
    public String getNiceName() {
        return "Demag.";
    }

    public void draw(Graphics2D g) {
        clearPoints();
        Sample sample = params.getSample();
        if (sample==null) return;
        List<Datum> data = sample.getVisibleData();
        if (data.size() == 0) return;

        Rectangle2D dim = cropRectangle(getDimensions(), 270, 200, 50, 230);
        boolean useEmptyCorr = params.isEmptyCorrectionActive();

        g.setColor(Color.BLACK);
        double maxIntens = 0, maxDemag = 0;
        for (Datum d: data) {
            if (d.getDemagLevel() > maxDemag) maxDemag = d.getDemagLevel();
            if (d.getIntensity(useEmptyCorr) > maxIntens)
                maxIntens = d.getIntensity(useEmptyCorr);
        }

        // If all the measurements have the same demag level, we'll
        // just plot them in sequence to avoid giving them all the same
        // X co-ordinate.
        boolean xBySequence = false;

        double xAxisLength;
        if (maxDemag == 0) {
            xAxisLength = data.size() > 1 ? data.size() : 1;
            xBySequence = true;
        } else {
            xAxisLength = maxDemag;
        }
        if (maxIntens == 0) maxIntens = 1;

        final String xAxisLabel = xBySequence
                ? "Measurement number"
                : sample.getDatum(sample.getNumData() - 1).getTreatType().getAxisLabel();

        PlotAxis vAxis = new PlotAxis(maxIntens, PlotAxis.Direction.UP,
                "Intensity", null, this);
        PlotAxis hAxis = new PlotAxis(xAxisLength, PlotAxis.Direction.RIGHT,
                xAxisLabel, null, this);
        
        double hScale = dim.getWidth() / hAxis.getLength();
        double vScale = dim.getHeight() / vAxis.getLength();
        
        vAxis.draw(g, vScale, (int)dim.getMinX(), (int)dim.getMaxY());
        hAxis.draw(g, hScale, (int)dim.getMinX(), (int)dim.getMaxY());
        
        int i = 0;
        for (Datum d: data) {
            double xPos = dim.getMinX() +
                    (xBySequence ? (i + 1) : d.getDemagLevel()) * hScale;
            addPoint(d, new Point2D.Double(xPos,
                    dim.getMaxY() - d.getIntensity(useEmptyCorr) * vScale),
                    true, false, i>0);
            i++;
        }
        drawPoints(g);
    }
}
