package net.talvi.puffinplot.plots;

import net.talvi.puffinplot.data.TreatType;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MDF;
import net.talvi.puffinplot.data.Sample;
import static net.talvi.puffinplot.plots.PlotAxis.AxisParameters;
import static net.talvi.puffinplot.plots.PlotAxis.Direction;

public class DemagPlot extends Plot {

    private final Preferences prefs;

    public DemagPlot(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
        this.prefs = prefs;
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
        if (data.isEmpty()) return;

        Rectangle2D dim = cropRectangle(getDimensions(), 270, 200, 50, 250);
        g.setColor(Color.BLACK);
        double maxDemag = Datum.maxTreatmentLevel(data);
        double maxIntens = Datum.maxIntensity(data);

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

        TreatType treatType = sample.getDatum(sample.getNumData() - 1).getTreatType();
        final String xAxisLabel;
        if (xBySequence) {
            xAxisLabel = "Measurement number";
        } else {
            String unit = treatType.getUnit();
            if (treatType == TreatType.DEGAUSS_XYZ) {
                unit = "m" + unit;
            }
            xAxisLabel = String.format("%s (%s)", treatType.getAxisLabel(), unit);
        }
        double demagRescale = 1;
        if (treatType == TreatType.DEGAUSS_XYZ) demagRescale = 1000;
        AxisParameters hAxisParams = new AxisParameters(xAxisLength * demagRescale, Direction.RIGHT).
                withLabel(xAxisLabel).withNumberEachTick();

        final MDF midpoint = sample.getMDF();
        if (midpoint != null && midpoint.isHalfIntReached()) {
            hAxisParams.markedPosition = midpoint.getDemagLevel() * demagRescale;
        }
        final PlotAxis hAxis = new PlotAxis(hAxisParams, this);
        final String vAxisLabel = prefs.get("plots."+getName()+".vAxisLabel",
                "Magnetization (A/m)");
        final PlotAxis vAxis =
                new PlotAxis(new AxisParameters(maxIntens, Direction.UP).
                withLabel(vAxisLabel).withNumberEachTick(), this);
        
        double hScale = dim.getWidth() / hAxis.getLength();
        final double vScale = dim.getHeight() / vAxis.getLength();
        
        vAxis.draw(g, vScale, (int)dim.getMinX(), (int)dim.getMaxY());
        hAxis.draw(g, hScale, (int)dim.getMinX(), (int)dim.getMaxY());
        addPoint(null, new Point2D.Double(dim.getMinX()-10, dim.getMaxY()),
                true, false, false);
        
        int i = 0;
        for (Datum d: data) {
            double demagLevel = d.getTreatmentLevel() * demagRescale;
            double xPos = dim.getMinX() +
                    (xBySequence ? (i + 1) : demagLevel) * hScale;
            addPoint(d, new Point2D.Double(xPos,
                    dim.getMaxY() - d.getIntensity() * vScale),
                    true, false, i>0);
            i++;
        }

        if (hAxisParams.markedPosition != null) {
            final double xPos = dim.getMinX() + hScale * hAxisParams.markedPosition;
            final double yPos = dim.getMaxY() - midpoint.getIntensity() * vScale;
            g.draw(new Line2D.Double(dim.getMinX(), yPos,
                    xPos, yPos));
            g.draw(new Line2D.Double(xPos, dim.getMaxY()-getFontSize()*1.5,
                    xPos, yPos));
        }

        if (sample.hasMsData()) {
            addPoint(null, new Point2D.Double(dim.getMaxX() + 10, dim.getMaxY()),
                    false, false, false);
            final AxisParameters msAxisParams =
                    new AxisParameters(Datum.maxMagSus(data),
                    Direction.UP).withNumberEachTick();
            msAxisParams.label = "Mag. sus. (S.I.)";
            msAxisParams.farSide = true;
            final PlotAxis msAxis = new PlotAxis(msAxisParams, this);
            final double msScale = dim.getHeight() / msAxis.getLength();
            msAxis.draw(g, msScale, (int)dim.getMaxX(), (int)dim.getMaxY());
            i = 0;
            boolean first = true;
            for (Datum d: data) {
            final double xPos = dim.getMinX() +
                    (xBySequence ? (i + 1) : d.getTreatmentLevel()) * hScale;
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
