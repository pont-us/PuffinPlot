/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2015 Pontus Lurcock.
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
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.GreatCircle;
import net.talvi.puffinplot.data.PcaValues;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

/**
 * A textual display of a set of PCA and great circle parameters
 * for a single sample.
 * 
 * @author pont
 */
public class SampleParamsLegend extends Plot {

    /** Creates a new PCA table with the supplied parameters.
     * 
     * @param parent the graph display containing the table
     * @param params the parameters of the table
     * @param prefs the preferences containing the table configuration
     */
    public SampleParamsLegend(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
    }

    @Override
    public String getName() {
        return "pcatable";
    }

    @Override
    public String getNiceName() {
        return "Sample parameters";
    }

    @Override
    public int getMargin() {
        return 12;
    }
    
    @Override
    public void draw(Graphics2D g) {
        Sample sample = params.getSample();
        if (sample==null) return;
 
        final PcaValues pca = sample.getPcaValues();
        final GreatCircle gc = sample.getGreatCircle();
        
        List<String> strings = new ArrayList<>(10);
                
        if (gc != null) {
            strings.addAll(Arrays.asList(
                String.format(Locale.ENGLISH,
                        "GC  dec %.2f / inc %.2f", gc.getPole().getDecDeg(),
                        gc.getPole().getIncDeg()),
                String.format(Locale.ENGLISH,
                        "GC  MAD1 %.2f", gc.getMad1())));
        }
        
        if (pca != null) {
            strings.addAll(Arrays.asList(
                String.format(Locale.ENGLISH,
                        "PCA  dec %.2f / inc %.2f",
                        pca.getDirection().getDecDeg(),
                        pca.getDirection().getIncDeg()),
                String.format(Locale.ENGLISH,
                        "PCA  MAD1 %.2f / MAD3 %.2f",
                    pca.getMad1(), pca.getMad3()),
                pca.getEquation()));
        }
        
        if (!strings.isEmpty()) {
            g.setColor(Color.BLACK);
            for (int i=0; i<strings.size(); i++) {
              writeString(g, strings.get(i),
                      (int) getDimensions().getMinX(),
                      (int) (getDimensions().getMinY() + (i+1) * getFontSize() * 1.2));
            }
        }
    }
}
