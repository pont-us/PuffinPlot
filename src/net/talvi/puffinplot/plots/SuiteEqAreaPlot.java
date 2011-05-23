package net.talvi.puffinplot.plots;

import java.awt.geom.Point2D;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.GreatCircles;
import net.talvi.puffinplot.data.Site;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import static java.lang.Math.min;

/**
 * Displays directions for great-circle fits for whole suite.
 * 
 */
public class SuiteEqAreaPlot extends EqAreaPlot {

    public SuiteEqAreaPlot(GraphDisplay parent, PlotParams params,
            Preferences prefs) {
        super(parent, params, prefs);
    }

    @Override
    public String getName() {
        return "suite_directions";
    }

    @Override
    public void draw(Graphics2D g) {
        List<Site> sites = params.getSample().getSuite().getSites();
        if (sites == null || sites.isEmpty()) {
            writeString(g, "No sites defined.", 100, 100);
            return;
        }
        final Rectangle2D dims = getDimensions();
        final int radius = (int) (min(dims.getWidth(), dims.getHeight()) / 2);
        final int xo = (int) dims.getCenterX();
        final int yo = (int) dims.getCenterY();
        clearPoints();
        drawAxes(g, xo, yo, radius);
        List<Vec3> vs = new ArrayList<Vec3>();
        for (Site site: sites) {
            GreatCircles circles = site.getGreatCircles();
            if (circles == null || !circles.isValid()) continue;
            Vec3 v = circles.getDirection();
            addPoint(null, project(v, xo, yo, radius), v.z>0, false, true);
            vs.add(v);
        }

        FisherValues fv = FisherValues.calculate(vs);
        final Vec3 mean = fv.getMeanDirection();
        drawLineSegments(g, xo, yo, radius, mean.makeSmallCircle(fv.getA95()));
        PlotPoint meanPoint = new CirclePoint(this, null, project(mean, xo, yo, radius));
        meanPoint.draw(g);
        drawPoints(g);
    }
}
