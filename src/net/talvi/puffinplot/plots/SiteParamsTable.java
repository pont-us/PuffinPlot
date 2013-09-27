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
import net.talvi.puffinplot.data.*;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

/**
 * A table showing site mean directions for the whole suite.
 * The table shows the site name, the calculation method
 * (anchored or unanchored PCA or great circle), declination,
 * and inclination. For PCA the principal direction is shown;
 * for great circles, the direction of the pole is shown.
 */
public class SiteParamsTable extends Plot {

    private final double us = getUnitSize();
    private final List<Double> xSpacing =
            Arrays.asList(500*us, 250*us, 270*us, 250*us, 400*us, 400*us,
            400*us);
    private final int ySpacing = (int) (120 * getUnitSize());
    private final List<String> headers = 
            Arrays.asList(new String[] {"Site", "n", "PCA", "GC", "dec.",
                "inc.", "type"});
    
    /** Creates a sample parameter table with the supplied parameters.
     * 
     * @param parent the graph display containing the plot
     * @param params the parameters of the plot
     * @param prefs the preferences containing the plot configuration
     */
    public SiteParamsTable(GraphDisplay parent, PlotParams params,
            Preferences prefs) {
        super(parent, params, prefs);
    }
    
    @Override
    public String getName() {
        return "site_params_table";
    }
    
    @Override
    public String getNiceName() {
        return "Site parameter table";
    }

    @Override
    public void draw(Graphics2D g) {
        clearPoints();
        final Sample sample = params.getSample();
        if (sample==null) return;
        final Suite suite = sample.getSuite();
        if (suite==null) return;
        List<Site> sites = suite.getSites();
        
        
        points.add(new TextLinePoint(this, g, 10, null, null, headers, xSpacing));

        final int columns = headers.size();
        int sequence = 1;
        float yPos = 2 * ySpacing;
        for (Site site: sites) {
            if (yPos > getDimensions().getHeight()) break;
            final List<String> values = new ArrayList<String>(columns);
            values.addAll(Collections.nCopies(columns, "--"));
            values.set(0, site.toString());
            values.set(1, format("%d", site.getSamples().size()));
            if (site.getFisherValues() != null) {
                final FisherValues fvs = site.getFisherValues();
                values.set(2, format("%d", fvs.getNDirs()));
                values.set(3, "0");
                final Vec3 direction = fvs.getMeanDirection();
                values.set(4, format("%.1f", direction.getDecDeg()));
                values.set(5, format("%.1f", direction.getIncDeg()));
                values.set(6, "Fisher");
            } else if (site.getGreatCircles() != null) {
                final GreatCircles gcs = site.getGreatCircles();
                values.set(2, format("%d", gcs.getM()));
                values.set(3, format("%d", gcs.getN()));
                final Vec3 direction = gcs.getMeanDirection();
                values.set(4, format("%.1f", direction.getDecDeg()));
                values.set(5, format("%.1f", direction.getIncDeg()));
                values.set(6, "GC " + (gcs.isValid() ? "(v)" : "(i)"));
            }
            
            Sample firstSample = null;
            List<Sample> siteSamples = site.getSamples();
            if (!siteSamples.isEmpty()) firstSample = siteSamples.get(0);
            points.add(new TextLinePoint(this, g, yPos, null, firstSample,
                    values, xSpacing));
            yPos += ySpacing;
            sequence++;
        }
        g.setColor(Color.BLACK);
        drawPoints(g);
    }
    
}
