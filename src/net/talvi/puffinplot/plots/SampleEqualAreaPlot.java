/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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
import java.util.List;

import net.talvi.puffinplot.data.GreatCircle;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.window.PlotParams;

/**
 * An equal-area plot showing sample data. This plot shows the magnetic
 * moment measurements for the treatment steps of a single sample.
 * It can also show a best-fit great circle, if one has been calculated.
 * 
 * @author pont
 */
public class SampleEqualAreaPlot extends EqualAreaPlot {

    /** Creates a sample equal-area plot with the supplied parameters.
     * 
     * @param params the parameters of the plot
     */
    public SampleEqualAreaPlot(PlotParams params) {
        super(params);
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
    public void draw(Graphics2D graphics) {
        updatePlotDimensions(graphics);
        clearPoints();
        final Sample sample = params.getSample();
        if (sample == null) {
            return;
        }
        
        drawAxes();
        boolean first = true;
        Vec3 prev = null;
        final List<TreatmentStep> visibleData =
                sample.getVisibleTreatmentSteps();
        boolean hasWellFormedData = false;
        for (TreatmentStep step: visibleData) {
            final Vec3 vector =
                    step.getMoment(params.getCorrection()).normalize();
            if (vector.isWellFormed()) {
                hasWellFormedData = true;
                addPoint(step, project(vector), vector.z>0, first, false);
                if (!first) {
                    drawGreatCircleSegment(prev, vector);
                }
                prev = vector;
                first = false;
            }
        }
        if (!hasWellFormedData) {
            return;
        }
        final GreatCircle gc = sample.getGreatCircle();
        if (gc != null) {
            final Vec3 pole = sample.getGreatCircle().getPole();
            drawGreatCircle(pole, true);
            ShapePoint.build(this, project(pole)).filled(pole.z>0).
                    triangle().build().draw(graphics);
        }
        if (sample.getPcaValues() != null) {
            final Vec3 dir = sample.getPcaValues().getDirection();
            ShapePoint.build(this, project(dir)).scale(1.5).
                    filled(dir.z>0).circle().build().draw(graphics);
        }
        if (sample.getFisherValues() != null) {
            final Vec3 dir = sample.getFisherValues().getMeanDirection();
            ShapePoint.build(this, project(dir)).scale(1.5).
                    filled(dir.z>0).diamond().build().draw(graphics);
        }
        if (sample.getImportedDirection() != null) {
            final Vec3 dir = sample.getImportedDirection();
            ShapePoint.build(this, project(dir)).scale(1.5).
                    filled(dir.z>0).triangle().build().draw(graphics);
        }

        drawPoints(graphics);
    }

    @Override
    public String getShortName() {
        return "Sample";
    }
}
