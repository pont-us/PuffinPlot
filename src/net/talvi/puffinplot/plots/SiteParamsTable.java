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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.GreatCircles;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Site;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.Vec3;
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
            400*us, 500*us, 350*us);
    private final int ySpacing = (int) (120 * getUnitSize());
    private final List<String> headers = 
            Arrays.asList(new String[] {"Site", "n", "PCA", "GC", "dec.",
                "inc.", "a95", "R", "type"});
    private final Preferences prefs;
    
    /**
     * Creates a site parameter table with the supplied parameters.
     *
     * @param params the parameters of the plot
     */
    public SiteParamsTable(PlotParams params) {
        super(params);
        this.prefs = params.getPreferences();
    }
    
    @Override
    public String getName() {
        return "site_params_table";
    }
    
    @Override
    public String getNiceName() {
        return "Site parameter table";
    }
        
    private static String fmt(String format, Object... args) {
        return String.format(Locale.ENGLISH, format, args);
    }

    @Override
    public void draw(Graphics2D graphics) {
        clearPoints();
        final Sample sample = params.getSample();
        if (sample==null) {
            return;
        }
        final Suite suite = sample.getSuite();
        if (suite==null) {
            return;
        }
        final List<Site> sites = suite.getSites();
        
        points.add(new TextLinePoint(this, graphics, 10, null, null,
                headers, xSpacing, Color.BLACK));

        final Color highlightColour = (prefs != null &&
                prefs.getBoolean("plots.highlightCurrentSample", false)) ?
                Color.RED : Color.BLACK;
        final int columns = headers.size();
        float yPos = 2 * ySpacing;
        for (Site site: sites) {
            if (yPos > getDimensions().getHeight()) {
                break;
            }
            final List<String> values = new ArrayList<>(columns);
            values.addAll(Collections.nCopies(columns, "--"));
            values.set(0, site.toString());
            values.set(1, fmt("%d", site.getSamples().size()));
            if (site.getGreatCircles() != null &&
                    site.getGreatCircles().isValid()) {
                final GreatCircles gcs = site.getGreatCircles();
                values.set(2, fmt("%d", gcs.getM()));
                values.set(3, fmt("%d", gcs.getN()));
                final Vec3 direction = gcs.getMeanDirection();
                values.set(4, fmt("%.1f", direction.getDecDeg()));
                values.set(5, fmt("%.1f", direction.getIncDeg()));
                values.set(6, fmt("%.1f", gcs.getA95()));
                values.set(7, fmt("%.4f", gcs.getR()));
                values.set(8, "GC:" + (gcs.isValid() ? "v" : "i"));
            } else if (site.getFisherValues() != null) {
                final FisherValues fvs = site.getFisherValues();
                values.set(2, fmt("%d", fvs.getN()));
                values.set(3, "0");
                final Vec3 direction = fvs.getMeanDirection();
                values.set(4, fmt("%.1f", direction.getDecDeg()));
                values.set(5, fmt("%.1f", direction.getIncDeg()));
                values.set(6, fmt("%.1f", fvs.getA95()));
                values.set(7, fmt("%.4f", fvs.getR()));
                values.set(8, "Fshr");
            } 
            
            Sample firstSample = null;
            final List<Sample> siteSamples = site.getSamples();
            if (!siteSamples.isEmpty()) {
                firstSample = siteSamples.get(0);
            }
            points.add(new TextLinePoint(this, graphics, yPos, null,
                    firstSample, values, xSpacing,
                    site == params.getSample().getSite() ?
                            highlightColour : Color.BLACK));
            yPos += ySpacing;
        }
        graphics.setColor(Color.BLACK);
        drawPoints(graphics);
    }
    
}
