package net.talvi.puffinplot.plots;

import java.util.ArrayList;
import java.awt.Color;
import java.util.List;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.PuffinApp;
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

    private List<KentParams> cachedBootstrapParams;
    private List<KentParams> cachedHextParams;
    private List<List<Vec3>> cachedBootstrapRegions = new ArrayList<List<Vec3>>();
    private List<List<Vec3>> cachedHextRegions = new ArrayList<List<Vec3>>();    
    
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

    private List<List<Vec3>> paramsToSegments(List<KentParams> kps) {
        List<List<Vec3>> result = new ArrayList<List<Vec3>>();
        if (kps != null) {
            for (KentParams kp: kps) {
                List<List<Vec3>> segments =
                        Vec3.interpolateEquatorPoints(Vec3.ellipse(kp));
                result.addAll(segments);
            }
        }
        return result;
    }
    
    private void cacheConfidenceRegionsIfRequired(Suite suite) {
        final List<KentParams> bootstrapParams = suite.getAmsBootstrapParams();     
        if (bootstrapParams != cachedBootstrapParams) {
            cachedBootstrapParams = bootstrapParams;
            cachedBootstrapRegions = paramsToSegments(bootstrapParams);
        }
        final List<KentParams> hextParams = suite.getAmsHextParams();
        if (hextParams != cachedHextParams) {
            cachedHextParams = hextParams;
            cachedHextRegions = paramsToSegments(hextParams);
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

        for (Sample s: PuffinApp.getInstance().getAllSamplesInSelectedSites()) {
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

        cacheConfidenceRegionsIfRequired(sample.getSuite());
        // Mean directions should be same for Hext and bootstrap
        final List<KentParams> meanDirections = 
                cachedBootstrapParams != null ?
                cachedBootstrapParams :
                cachedHextParams;
        if (meanDirections != null) {
            for (int i=0; i<3; i+=1) {
                final KentParams kp = meanDirections.get(i);
                final Point2D pos = project(kp.getMean(), xo, yo, radius);
                getPointForAxis(pos, PLOT_POINT_SIZE*3, i).draw(g);
            }
        }
        
        g.setStroke(getStroke());
        g.setColor(Color.BLACK);
        for (List<Vec3> segment: cachedBootstrapRegions) {
            drawLhLineSegments(g, xo, yo, radius, segment);
        }
        g.setStroke(getDashedStroke());
        for (List<Vec3> segment: cachedHextRegions) {
            drawLhLineSegments(g, xo, yo, radius, segment);
        }
    }
}
