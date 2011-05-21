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
            if (circles == null) continue;
            Vec3 v = circles.getDirection();
            addPoint(null, project(v, xo, yo, radius), v.z>0, false, false);
            vs.add(v);
        }

        FisherValues fv = FisherValues.calculate(vs);
        GeneralPath ellipse = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 72);
        boolean firstEllipsePoint = true;
        final Vec3 mean = fv.getMeanDirection();
        for (double dec = 0; dec < 360; dec += 5) {
            Vec3 circlePoint =
                    (Vec3.fromPolarDegrees(1, 90 - fv.getA95(), dec));
            Vec3 w = circlePoint.rotY(Math.PI / 2 - mean.getIncRad());
            w = w.rotZ(mean.getDecRad());
            Point2D.Double p = project(w, xo, yo, radius);
            if (firstEllipsePoint) {
                ellipse.moveTo((float) p.x, (float) p.y);
            } else {
                ellipse.lineTo((float) p.x, (float) p.y);
            }
            firstEllipsePoint = false;
        }
        ellipse.closePath();
        g.draw(ellipse);
        //addPoint(null, project(mean, xo, yo, radius), mean.z>0, true, false);
        PlotPoint meanPoint = new CirclePoint(this, null, project(mean, xo, yo, radius));
        meanPoint.draw(g);
        drawPoints(g);
    }
}
