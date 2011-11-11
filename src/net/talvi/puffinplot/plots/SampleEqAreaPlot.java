package net.talvi.puffinplot.plots;

import static java.lang.Math.min;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.data.Sample;

public class SampleEqAreaPlot extends EqAreaPlot {

    public SampleEqAreaPlot(GraphDisplay parent, PlotParams params,
            Preferences prefs) {
        super(parent, params, prefs);
    }

    @Override
    public String getName() {
        return "equarea";
    }

    @Override
    public String getNiceName() {
        return "Equal-area";
    }

    public void draw(Graphics2D g) {
        final Rectangle2D dims = getDimensions();
        final int radius = (int) (min(dims.getWidth(), dims.getHeight()) / 2);
        final int xo = (int) dims.getCenterX();
        final int yo = (int) dims.getCenterY();
        
        updatePlotDimensions(g);
        clearPoints();
        final Sample sample = params.getSample();
        if (sample == null) return;
        
        drawAxes();
        boolean first = true;
        Vec3 prev = null;
        for (Datum d: sample.getVisibleData()) {
            final Vec3 p = d.getMoment(params.getCorrection()).normalize();
            addPoint(d, project(p), p.z>0, first, false);
            if (!first) {
                drawGreatCircleSegment(prev, p);
            }
            prev = p;
            first = false;
        }
        if (sample.greatCircle != null) {
            drawGreatCircle(g, xo, yo, radius, sample.greatCircle, true);
        }

        /* Some code to show where North gets projected to.
         * Probably not very useful in general.
        Datum anyOldDatum = sample.getVisibleData().get(0);
        Vec3 north = anyOldDatum.correctVector(Vec3.NORTH, params.getCorrection());
        points.add(new TrianglePoint(this, null,
                project(north, xo, yo, radius)));
        */

        drawPoints(g);
    }
}
