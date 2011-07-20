package net.talvi.puffinplot.plots;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.awt.Stroke;
import java.awt.Color;
import java.util.List;
import net.talvi.puffinplot.data.Suite;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Site;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.KentParams;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import static java.lang.Math.min;

public class AmsPlot extends EqAreaPlot {

    public AmsPlot(GraphDisplay parent, PlotParams params, Preferences prefs) {
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

    private void drawConfidenceEllipse(Graphics2D g, int radius, int xo, int yo,
            KentParams kp) {
        List<List<Vec3>> segments = Vec3.interpolateEquatorPoints(Vec3.ellipse(kp));
        for (List<Vec3> segment: segments) {
            drawLhLineSegments(g, xo, yo, radius, segment);
        }
    }

    private PlotPoint getPointForAxis(Point2D pos, double size, int axis) {
        assert(axis>=0 && axis < 3);
        return axis==0 ? new SquarePoint(this, null, pos, true, false, false, size)
                : axis==1 ? new TrianglePoint(this, null, pos, size)
                : new CirclePoint(this, null, pos, size);
    }

    protected void drawLhLineSegments(Graphics2D g,
            int xo, int yo, int radius, List<Vec3> vs) {
         // determine whether we're in upper hemisphere, ignoring
         // z co-ordinates very close to zero. Assumes all segments
         // in same hemisphere.
         boolean upperHemisph = true;
         for (Vec3 v: vs) { if (v.z > 1e-10) { upperHemisph = false; break; } }
         List<Vec3> vs2 = vs;
         if (upperHemisph) {
             vs2 = new ArrayList<Vec3>(vs.size());
             for (Vec3 v: vs) {
                 vs2.add(v.invert());
             }
         }
         g.draw(vectorsToPath(vs2, xo, yo, radius));
     }

    public void draw(Graphics2D g) {
        final Rectangle2D dims = getDimensions();
        final int radius = (int) (min(dims.getWidth(), dims.getHeight()) / 2);
        final int xo = (int) dims.getCenterX();
        final int yo = (int) dims.getCenterY();

        clearPoints();
        drawAxes(g, xo, yo, radius);
        final Sample sample = params.getSample();
        if (sample == null) return;

        g.setStroke(getStroke());
        final Set<Sample> samplesToPlot = new HashSet<Sample>();
        for (Sample s: PuffinApp.getInstance().getSelectedSamples()) {
            samplesToPlot.addAll(s.getSite().getSamples());
        }
        for (Sample s: samplesToPlot) {
            if (s.getAms() != null) {
                for (int i=0; i<3; i++) {
                    Vec3 v = s.getAmsAxis(i).normalize();
                    if (v.z < 0) v = v.invert(); // ensure lower hemisphere
                    final Point2D pos = project(v, xo, yo, radius);
                    g.setColor(Color.GRAY);
                    getPointForAxis(pos, PLOT_POINT_SIZE, i).draw(g);
                }
            }
        }

        Suite suite = sample.getSuite();
        List<KentParams> bootstrapParams = suite.getAmsBootstrapParams();
        List<KentParams> hextParams = suite.getAmsHextParams();
        if (bootstrapParams != null) {
            for (int i=0; i<3; i+=1) {
                final KentParams kp = bootstrapParams.get(i);
                final Point2D pos = project(kp.getMean(), xo, yo, radius);
                getPointForAxis(pos, PLOT_POINT_SIZE*3, i).draw(g);
                g.setStroke(getStroke());
                g.setColor(Color.BLACK);
                drawConfidenceEllipse(g, radius, xo, yo, kp);
            }
        }
        if (hextParams != null) {
            g.setStroke(getDashedStroke());
            for (KentParams kp: hextParams) {
                drawConfidenceEllipse(g, radius, xo, yo, kp);
            }
        }
    }
}
