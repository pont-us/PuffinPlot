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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.plots.PlotAxis.AxisParameters;

/**
 * A histogram of the intensities of natural remanent magnetizations of
 * a collection of samples.
 * 
 * @author pont
 */
public class NrmHistogram extends Plot {

    /**
     * Creates an NRM histogram with the supplied parameters
     *
     * @param params the parameters of the plot
     */
    public NrmHistogram(PlotParams params) {
        super(params);
    }
    
    /**
     * Returns this plot's internal name.
     *
     * @return this plot's internal name
     */
    @Override
    public String getName() {
        return "nrm_histogram";
    }

    /**
     * Returns this plot's user-friendly name.
     *
     * @return this plot's user-friendly name
     */
    @Override
    public String getNiceName() {
        return "NRM Histogram";
    }
    
    /**
     * Draws this plot.
     *
     * @param graphics the graphics object to which to draw the plot
     */
    @Override
    public void draw(Graphics2D graphics) {
        clearPoints();
        final Sample selectedSample = params.getSample();
        if (selectedSample==null) {
            return;
        }
        final Suite suite = selectedSample.getSuite();
        final List<Double> nrms = new ArrayList<>(suite.getSamples().size());
        for (Sample sample: suite.getSamples()) {
            if (sample.hasTreatmentSteps()) {
                nrms.add(sample.getNrm());
            }
        }
        if (nrms.isEmpty()) {
            return;
        }
        final double minimum = 0; //Collections.min(nrms);
        final double maximum = Collections.max(nrms);
        final double range = maximum-minimum;
        final int nBuckets = 50;
        final Rectangle2D dim =
                cropRectangle(getDimensions(), 270, 200, 50, 250);
        final List<Integer> buckets =
                new ArrayList<>(Collections.nCopies(nBuckets, 0));
        for (double nrm : nrms) {
            int bucket = (int) Math.round(nBuckets * (nrm-minimum)/range);
            if (bucket == nBuckets) {
                bucket--;  // rightmost edge of rightmost bucket
            }
            buckets.set(bucket, 1+buckets.get(bucket));
        }

        AxisParameters hAxisParams = new AxisParameters(range, Direction.RIGHT).
                withLabel("NRM (A/m)").withNumberEachTick().
                withStartValue(minimum);
        final PlotAxis hAxis = new PlotAxis(hAxisParams, this);
        final PlotAxis vAxis = new PlotAxis(
                new AxisParameters(Collections.max(buckets), Direction.UP).
                withLabel("Number of samples").withNumberEachTick(), this);
        
        final double hScale = dim.getWidth() / hAxis.getLength();
        final double vScale = dim.getHeight() / vAxis.getLength();
        
        vAxis.draw(graphics, vScale, (int)dim.getMinX(), (int)dim.getMaxY());
        hAxis.draw(graphics, hScale, (int)dim.getMinX(), (int)dim.getMaxY());
        
        for (int i = 0; i < nBuckets; i++) {
            double xPos = dim.getMinX() + 
                    (((double) i) / (double) nBuckets) * range * hScale;
            Rectangle2D r = new Rectangle2D.Double(xPos,
                    dim.getMaxY() - buckets.get(i) * vScale,
                    range * hScale/(double)nBuckets,
                    buckets.get(i) * vScale);
            graphics.setColor(Color.gray);
            graphics.fill(r);
        }
    }
}
