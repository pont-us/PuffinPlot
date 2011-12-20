package net.talvi.puffinplot.plots;

import java.awt.Graphics2D;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.GreatCircle;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.data.Sample;

/**
 * An equal-area plot showing sample data. This plot shows the magnetic
 * moment measurements for the treatment steps of a single sample.
 * It can also show a best-fit great circle, if one has been calculated.
 * 
 * @author pont
 */
public class SampleEqAreaPlot extends EqAreaPlot {

      /** Creates a sample equal-area plot with the supplied parameters.
     * 
     * @param parent the graph display containing the plot
     * @param params the parameters of the plot
     * @param prefs the preferences containing the plot configuration
     */
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
        final GreatCircle gc = sample.getGreatCircle();
        if (gc != null) {
            final Vec3 pole = sample.getGreatCircle().getPole();
            drawGreatCircle(pole, true);
            ShapePoint.build(this, project(pole)).filled(pole.z>0).
                    triangle().build().draw(g);
        }

        drawPoints(g);
    }
}
