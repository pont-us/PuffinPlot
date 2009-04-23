package net.talvi.puffinplot.plots;

import static java.lang.Math.min;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
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
        
        clearPoints();
        final Sample sample = params.getSample();
        if (sample == null) return;
        
        drawAxes(g, xo, yo, radius);
        boolean first = true;
        Vec3 prev = null;
        for (Datum d: sample.getData()) {
            final Vec3 p = d.getPoint(params.getCorrection(),
                    params.isEmptyCorrectionActive()).normalize();
            addPoint(d, project(p, xo, yo, radius), p.z>0, first, false);
            if (!first) {
                drawGreatCircleSegment(g, xo, yo, radius, prev, p);
            }
            prev = p;
            first = false;
        }
        drawPoints(g);
    }
}
