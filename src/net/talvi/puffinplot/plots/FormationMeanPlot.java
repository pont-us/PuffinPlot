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
public class FormationMeanPlot extends EqAreaPlot {

    public FormationMeanPlot(GraphDisplay parent, PlotParams params,
            Preferences prefs) {
        super(parent, params, prefs);
    }

    @Override
    public String getName() {
        return "formation_mean";
    }
    
    @Override
    public String getNiceName() {
        return "Formation mean";
    }

    private void drawFisher(FisherValues fv,
                Graphics2D g, int xo, int yo, int radius) {
        final Vec3 mean = fv.getMeanDirection();
        drawLineSegments(mean.makeSmallCircle(fv.getA95()));
        PlotPoint meanPoint = 
                ShapePoint.build(this, project(mean)).
                circle().build();
        meanPoint.draw(g);
        System.out.println(fv.toString());
    }

    @Override
    public void draw(Graphics2D g) {
        List<Site> sites = params.getSample().getSuite().getSites();
        if (sites == null || sites.isEmpty()) {
            writeString(g, "No sites defined.", 100, 100);
            return;
        }
        updatePlotDimensions(g);
        clearPoints();
        drawAxes();
        List<Vec3> vs_n = new ArrayList<Vec3>();
        List<Vec3> vs_r = new ArrayList<Vec3>();
        for (Site site: sites) {
            Vec3 siteMean = null;
            GreatCircles circles = site.getGreatCircles();
            if (circles != null && circles.isValid())
                siteMean = circles.getDirection();
            if (siteMean == null) {
                FisherValues fv = site.getFisher();
                if (fv != null) siteMean = fv.getMeanDirection();
            }
            if (siteMean==null) continue;
            addPoint(null, project(siteMean), siteMean.z>0, false, false);
            (siteMean.z>0 ? vs_r : vs_n).add(siteMean);
        }
        if (vs_n.size()>1)
            drawFisher(FisherValues.calculate(vs_n), g, xo, yo, radius);
        if (vs_r.size()>1)
            drawFisher(FisherValues.calculate(vs_r), g, xo, yo, radius);
        //System.out.println(fv.toString());
        drawPoints(g);
    }
}
