/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
 *
 * PuffinPlot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PuffinPlot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.plots;

import java.awt.Graphics2D;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.GreatCircle;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

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
        return "Equal-area (sample)";
    }
    
    @Override
    public boolean areTreatmentStepsLabelled() {
        if (prefs==null) return false;
        else return prefs.getBoolean("plots.labelTreatmentSteps", false);
    }

    @Override
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

    @Override
    public String getShortName() {
        return "Sample";
    }
}
