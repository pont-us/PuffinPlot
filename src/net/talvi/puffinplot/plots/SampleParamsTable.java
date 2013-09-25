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

import java.awt.Color;
import java.awt.Graphics2D;
import static java.lang.String.format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.GreatCircle;
import net.talvi.puffinplot.data.PcaValues;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Site;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

/**
 * A table showing sample directions for the current site.
 * The table shows the sample name, the calculation method
 * (anchored or unanchored PCA or great circle), declination,
 * and inclination. For PCA the principal direction is shown;
 * for great circles, the direction of the pole is shown.
 */
public class SampleParamsTable extends Plot {

    private final double us = getUnitSize();
    private final List<Double> xSpacing =
            Arrays.asList(420*us, 420*us, 420*us, 420*us);
    private final int ySpacing = (int) (120 * getUnitSize());
    private final List<String> headers = 
            Arrays.asList(new String[] {"Sample", "type", "dec.", "inc."});
    
    /** Creates a sample parameter table with the supplied parameters.
     * 
     * @param parent the graph display containing the plot
     * @param params the parameters of the plot
     * @param prefs the preferences containing the plot configuration
     */
    public SampleParamsTable(GraphDisplay parent, PlotParams params,
            Preferences prefs) {
        super(parent, params, prefs);
    }
    
    @Override
    public String getName() {
        return "sample_params_table";
    }
    
    @Override
    public String getNiceName() {
        return "Sample parameter table";
    }

    @Override
    public void draw(Graphics2D g) {
        clearPoints();
        final Sample selectedSample = params.getSample();
        if (selectedSample==null) return;
        final Site site = selectedSample.getSite();
        if (site==null) return;
        
        points.add(new TextLinePoint(this, g, 10, null, null, headers, xSpacing));

        int sequence = 1;
        float yPos = 2 * ySpacing;
        for (Sample sample: site.getSamples()) {
            if (yPos > getDimensions().getHeight()) break;
            final List<String> values = new ArrayList<String>(4);
            values.addAll(Collections.nCopies(4, "--"));
            values.set(0, sample.getNameOrDepth());
            if (sample.getGreatCircle() != null) {
                final GreatCircle gc = sample.getGreatCircle();
                values.set(1, "GC");
                values.set(2, format("%.1f", gc.getPole().getDecDeg()));
                values.set(3, format("%.1f", gc.getPole().getIncDeg()));
            } else if (sample.getPcaValues() != null) {
                final PcaValues pca = sample.getPcaValues();
                values.set(1, pca.isAnchored() ? "PCAa" : "PCAu");
                values.set(2, format("%.1f", pca.getDirection().getDecDeg()));
                values.set(3, format("%.1f", pca.getDirection().getIncDeg()));
            }
            
            points.add(new TextLinePoint(this, g, yPos, null, sample, values, xSpacing));
            yPos += ySpacing;
            sequence++;
        }
        g.setColor(Color.BLACK);
        drawPoints(g);
    }
    
}
