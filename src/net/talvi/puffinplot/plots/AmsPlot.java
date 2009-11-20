package net.talvi.puffinplot.plots;

import Jama.Matrix;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import static java.lang.Math.min;

public class AmsPlot extends EqAreaPlot {

    public AmsPlot(GraphDisplay parent, PlotParams params,
            Preferences prefs) {
        super(parent, params, prefs);
    }

    @Override
    public String getName() {
        return "ams";
    }

    @Override
    public String getNiceName() {
        return "AMS";
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
        g.setStroke(getStroke());
        for (Sample s: PuffinApp.getInstance().getSuite().getSamples()) {
            if (s.getAms() != null &&
                    sample.getNameOrDepth().regionMatches(0, s.getNameOrDepth(), 0, 3)) {
                    Point2D prevp = null;
                for (int i=0; i<3; i++) {
                    Vec3 v = s.getAmsAxis(i, params.getCorrection()).normalize();
                    if (v.z > 0) v = v.invert();
                    Point2D pos = project(v, xo, yo, radius);
                    PlotPoint pp = null;

                    switch (i) {
                        case 0: pp = new SquarePoint(this, null, pos, true, false, false);
                        break;
                        case 1: pp = new TrianglePoint(this, null, pos);
                        prevp = pos;
                        break;
                        case 2: pp = new CirclePoint(this, null, pos);
                        // g.draw(new Line2D.Double(prevp, pos));
                        break;
                    }
                    pp.draw(g);
                    //addPoint(null, project(v, xo, yo, radius), v.z > 0, false, false);
                }
            }
        }
        //drawPoints(g);
    }
}
