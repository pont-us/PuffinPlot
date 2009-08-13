package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import java.util.prefs.Preferences;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MDF;
import net.talvi.puffinplot.data.Sample;
import static net.talvi.puffinplot.plots.PlotAxis.AxisParameters;
import static net.talvi.puffinplot.plots.PlotAxis.Direction;

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
        double maxDemag = Datum.maximumDemag(data);
        double maxIntens = Datum.maximumIntensity(data, useEmptyCorr);

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

        AxisParameters hAxisParams = new AxisParameters(xAxisLength, Direction.RIGHT).
                withLabel(xAxisLabel);

        final MDF midpoint = sample.getMidpoint();
        if (midpoint != null) {
            hAxisParams.markedPosition = midpoint.getDemagLevel();
        }
        final PlotAxis hAxis = new PlotAxis(hAxisParams, this);
        final PlotAxis vAxis =
                new PlotAxis(new AxisParameters(maxIntens, Direction.UP).
                withLabel("Intensity (Gauss)"), this);
        
        final double hScale = dim.getWidth() / hAxis.getLength();
        final double vScale = dim.getHeight() / vAxis.getLength();
        
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

        if (midpoint != null) {
            final double xPos = dim.getMinX() + midpoint.getDemagLevel() * hScale;
            final double yPos = dim.getMaxY() - midpoint.getIntensity() * vScale;
            g.draw(new Line2D.Double(dim.getMinX(), yPos,
                    xPos, yPos));
            g.draw(new Line2D.Double(xPos, dim.getMaxY(),
                    xPos, yPos));
        }

        if (sample.hasMsData()) {
            final AxisParameters msAxisParams =
                    new AxisParameters(Datum.maximumMagSus(data),
                    Direction.UP);
            msAxisParams.label = "Magnetic susceptibility (S.I.)";
            msAxisParams.farSide = true;
            final PlotAxis msAxis = new PlotAxis(msAxisParams, this);
            final double msScale = dim.getHeight() / msAxis.getLength();
            msAxis.draw(g, msScale, (int)dim.getMaxX(), (int)dim.getMaxY());
            i = 0;
            boolean first = true;
            for (Datum d: data) {
            final double xPos = dim.getMinX() +
                    (xBySequence ? (i + 1) : d.getDemagLevel()) * hScale;
            double magSus = d.getMagSus();
            if (magSus < 0) magSus = 0;
            if (d.hasMagSus()) {
                addPoint(d, new Point2D.Double(xPos,
                    dim.getMaxY() - magSus * msScale),
                    false, false, !first);
                first = false;
            }
            i++;
            }
        }

        drawPoints(g);
    }
}
