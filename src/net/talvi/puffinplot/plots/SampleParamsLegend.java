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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

import net.talvi.puffinplot.Util;
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

    private final DecimalFormat angleFormat;
    
    /** Creates a new sample parameter legend with the supplied parameters.
     * 
     * @param parent the graph display containing the legend
     * @param params the parameters of the legend
     * @param prefs the preferences containing the legend configuration
     */
    public SampleParamsLegend(PlotParams params) {
        super(params);
        angleFormat = new DecimalFormat("##0.00", Util.getDecimalFormatSymbols());
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
    
    private String fmt(String format, double d0, double d1) {
        return String.format(Locale.ENGLISH, format,
                angleFormat.format(d0), angleFormat.format(d1));
    }
    
    private String fmt(String format, double d0) {
        return String.format(Locale.ENGLISH, format,
                angleFormat.format(d0));
    }
    
    @Override
    public void draw(Graphics2D g) {
        final Sample sample = params.getSample();
        if (sample==null) return;
       
        final List<String> strings = new ArrayList<>(10);
        
        final GreatCircle gc = sample.getGreatCircle();
        if (gc != null) {
            strings.addAll(Arrays.asList(
                fmt("GC  dec %s / inc %s",
                    gc.getPole().getDecDeg(), gc.getPole().getIncDeg()),
                fmt("GC  MAD1 %s", gc.getMad1())));
        }
        
        final PcaValues pca = sample.getPcaValues();
        if (pca != null) {
            strings.addAll(Arrays.asList(
                fmt("PCA  dec %s / inc %s",
                    pca.getDirection().getDecDeg(),
                    pca.getDirection().getIncDeg()),
                fmt("PCA  MAD1 %s / MAD3 %s",
                    pca.getMad1(), pca.getMad3()),
                /*
                 * This is a rather hacky method of getting the equation to
                 * use proper minus signs, but it works.
                 */
                pca.getEquation().replace("-", "−")));
        }
        
        final FisherParams fisher = sample.getFisherValues();
        if (fisher != null) {
            strings.addAll(Arrays.asList(
                    fmt("Fisher dec %s / inc %s",
                        fisher.getMeanDirection().getDecDeg(),
                        fisher.getMeanDirection().getIncDeg()),
                    fmt("Fisher α95 %s / k %s",
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
