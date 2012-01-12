package net.talvi.puffinplot.plots;

import java.util.ArrayList;
import java.awt.Graphics2D;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.GreatCircles;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Site;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

/**
 * An equal-area plot data for an entire suite.
 * This plot displays site means calculated using Fisher statistics
 * or great-circle intersections, and overall Fisher means of the
 * site means themselves. If there are site means in both hemispheres,
 * a separate mean is shown for each hemisphere.
 */
public class SuiteEqAreaPlot extends EqAreaPlot {

    /** Creates a suite equal area plot with the supplied parameters.
     * 
     * @param parent the graph display containing the plot
     * @param params the parameters of the plot
     * @param prefs the preferences containing the plot configuration
     */
    public SuiteEqAreaPlot(GraphDisplay parent, PlotParams params,
            Preferences prefs) {
        super(parent, params, prefs);
    }

    @Override
    public String getName() {
        return "equarea_suite";
    }
    
    @Override
    public String getNiceName() {
        return "Suite directions";
    }

    private void drawFisher(FisherValues fv) {
        final Vec3 mean = fv.getMeanDirection();
        drawLineSegments(mean.makeSmallCircle(fv.getA95()));
        PlotPoint meanPoint = 
                ShapePoint.build(this, project(mean)).
                circle().build();
        meanPoint.draw(g);
    }

    @Override
    public void draw(Graphics2D g) {
        updatePlotDimensions(g);
        clearPoints();
        drawAxes();
        final Sample sample = params.getSample();
        if (sample==null) return;
        final Suite suite = sample.getSuite();
        if (suite==null) return;
        List<Site> sites = suite.getSites();
        if (sites == null || sites.isEmpty()) {
            writeString(g, "No sites defined.", xo-40, yo-20);
            return;
        }
        List<Vec3> vs_n = new ArrayList<Vec3>();
        List<Vec3> vs_r = new ArrayList<Vec3>();
        for (Site site: sites) {
            Vec3 siteMean = null;
            GreatCircles circles = site.getGreatCircles();
            if (circles != null && circles.isValid())
                siteMean = circles.getMeanDirection();
            if (siteMean == null) {
                FisherValues fv = site.getFisher();
                if (fv != null) siteMean = fv.getMeanDirection();
            }
            if (siteMean==null) continue;
            addPoint(null, project(siteMean), siteMean.z>0, false, false);
            (siteMean.z>0 ? vs_r : vs_n).add(siteMean);
        }
        if (vs_n.size()>1)
            drawFisher(FisherValues.calculate(vs_n));
        if (vs_r.size()>1)
            drawFisher(FisherValues.calculate(vs_r));
        drawPoints(g);
    }
}
