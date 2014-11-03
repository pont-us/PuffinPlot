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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.*;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

/**
 * A table which displays site location and VGP data.
 */
public class VgpTable extends Plot {

    private final double us = getUnitSize();
    private final List<Double> xSpacing =
            Arrays.asList(500*us, 400*us, 400*us, 400*us, 400*us, 400*us,
            400*us);
    private final int ySpacing = (int) (120 * getUnitSize());
    private final List<String> headers = 
            Arrays.asList(new String[] {"Site", "φ", "λ", "VGP φ", "VGP λ",
                "dp", "dm"});
    private final Preferences prefs;
    
    /** Creates a sample parameter table with the supplied parameters.
     * 
     * @param parent the graph display containing the plot
     * @param params the parameters of the plot
     * @param prefs the preferences containing the plot configuration
     */
    public VgpTable(GraphDisplay parent, PlotParams params,
            Preferences prefs) {
        super(parent, params, prefs);
        this.prefs = prefs;
    }
    
    @Override
    public String getName() {
        return "vgp_table";
    }
    
    @Override
    public String getNiceName() {
        return "VGP table";
    }
        
    private static String fmt(String format, Object... args) {
        return String.format(Locale.ENGLISH, format, args);
    }

    @Override
    public void draw(Graphics2D g) {
        clearPoints();
        final Sample sample = params.getSample();
        if (sample==null) return;
        final Suite suite = sample.getSuite();
        if (suite==null) return;
        final List<Site> sites = suite.getSites();
        
        points.add(new TextLinePoint(this, g, 10, null, null, headers, xSpacing, Color.BLACK));

        final Color highlightColour = (prefs != null &&
                prefs.getBoolean("plots.highlightCurrentSample", false)) ?
                Color.RED : Color.BLACK;
        final int columns = headers.size();
        float yPos = 2 * ySpacing;
        for (Site site: sites) {
            if (yPos > getDimensions().getHeight()) break;
            final List<String> values = new ArrayList<>(columns);
            values.addAll(Collections.nCopies(columns, "--"));
            values.set(0, site.toString());
            if (site.getLocation() != null) {
                values.set(1, fmt("%.1f", site.getLocation().getLatDeg()));
                values.set(2, fmt("%.1f", site.getLocation().getLongDeg()));
            }
            if (site.getVgp() != null) {
                final VGP vgp = site.getVgp();
                values.set(3, fmt("%.1f", vgp.getLocation().getLatDeg()));
                values.set(4, fmt("%.1f", vgp.getLocation().getLongDeg()));
                values.set(5, fmt("%.1f", vgp.getDp()));
                values.set(6, fmt("%.1f", vgp.getDm()));
            }
            
            Sample firstSample = null;
            List<Sample> siteSamples = site.getSamples();
            if (!siteSamples.isEmpty()) firstSample = siteSamples.get(0);
            points.add(new TextLinePoint(this, g, yPos, null, firstSample,
                    values, xSpacing, site == params.getSample().getSite()?
                    highlightColour : Color.BLACK));
            yPos += ySpacing;
        }
        g.setColor(Color.BLACK);
        drawPoints(g);
    }
    
}