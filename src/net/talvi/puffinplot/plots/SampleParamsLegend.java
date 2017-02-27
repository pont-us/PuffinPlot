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
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.FisherParams;
import net.talvi.puffinplot.data.GreatCircle;
import net.talvi.puffinplot.data.PcaValues;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

/**
 * A textual display of a set of parameters for a single sample.
 * 
 * This legend displays sample directions and associated data from 
 * PCA calculations, great circle fits, and Fisher statistics on individual 
 * treatment steps.
 * 
 * @author pont
 */
public class SampleParamsLegend extends Plot {

    /** Creates a new sample parameter legend with the supplied parameters.
     * 
     * @param parent the graph display containing the legend
     * @param params the parameters of the legend
     * @param prefs the preferences containing the legend configuration
     */
    public SampleParamsLegend(GraphDisplay parent, PlotParams params,
            Preferences prefs) {
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
    
    private static String fmt(String format, Object... arguments) {
        return String.format(Locale.ENGLISH, format, arguments);
    }
    
    @Override
    public void draw(Graphics2D g) {
        final Sample sample = params.getSample();
        if (sample==null) return;
       
        final List<String> strings = new ArrayList<>(10);
        
        final GreatCircle gc = sample.getGreatCircle();
        if (gc != null) {
            strings.addAll(Arrays.asList(
                fmt("GC  dec %.2f / inc %.2f",
                    gc.getPole().getDecDeg(), gc.getPole().getIncDeg()),
                fmt("GC  MAD1 %.2f", gc.getMad1())));
        }
        
        final PcaValues pca = sample.getPcaValues();
        if (pca != null) {
            strings.addAll(Arrays.asList(
                fmt("PCA  dec %.2f / inc %.2f",
                    pca.getDirection().getDecDeg(),
                    pca.getDirection().getIncDeg()),
                fmt("PCA  MAD1 %.2f / MAD3 %.2f",
                    pca.getMad1(), pca.getMad3()),
                pca.getEquation()));
        }
        
        final FisherParams fisher = sample.getFisherValues();
        if (fisher != null) {
            strings.addAll(Arrays.asList(
                    fmt("Fisher dec %.2f / inc %.2f",
                        fisher.getMeanDirection().getDecDeg(),
                        fisher.getMeanDirection().getIncDeg()),
                    fmt("Fisher α95 %.2f / k %.2f",
                        fisher.getA95(), fisher.getK())));
        }
        
        if (!strings.isEmpty()) {
            g.setColor(Color.BLACK);
            final Rectangle2D dims = getDimensions();
            for (int i=0; i<strings.size(); i++) {
              writeString(g, strings.get(i),
                      (int) dims.getMinX(),
                      (int) (dims.getMinY() + (i+1) * getFontSize() * 1.2));
            }
        }
    }
}
