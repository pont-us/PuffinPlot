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
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.SuiteCalcs;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

/**
 * A title for the graph display, showing the sample identifier and
 * some other information.
 * 
 * @author pont
 */
public class PlotTitle extends Plot {

    /** Creates a plot title with the supplied parameters.
     * 
     * @param parent the graph display containing the plot title
     * @param params the parameters of the plot title
     * @param prefs the preferences containing the plot title configuration
     */
    public PlotTitle(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
    }
    
    @Override
    public int getMargin() {
        return 12;
    }

    @Override
    public String getName() {
        return "title";
    }

    @Override
    public String getNiceName() {
        return "Title";
    }

    @Override
    public void draw(Graphics2D g) {
        final Sample sample = params.getSample();
        if (sample==null) return;

        g.setFont(Font.getFont(getTextAttributes()));
        g.setColor(Color.BLACK);
        final Font oldFont = g.getFont();
        final Font biggerFont = oldFont.deriveFont(getFontSize() * 1.5f);
        g.setFont(biggerFont);
        final int minX = (int) getDimensions().getMinX(); 
        final int minY = (int) getDimensions().getMinY();
        final boolean discrete = sample.getMeasType().isDiscrete();
        g.drawString((discrete ? "Sample: " : "Depth: ")
                + sample.getNameOrDepth(),
                minX, minY + 16);
        g.setFont(oldFont);
        if (sample.getSite() != null) {
            g.drawString("Site: "+sample.getSite().toString(), minX,
                minY + 32);
        }
        final Suite suite = sample.getSuite();
        if (suite.getSuiteMeans() != null) {
            int yPos = 48;
            final SuiteCalcs means = suite.getSuiteMeans();
            final String bySampleString = writeSuiteMeans(means.getDirsBySample());
            if (bySampleString != null) {
                g.drawString("Sample mean: " + bySampleString, minX, minY + yPos);
                yPos += 16;
            }
            final String bySiteString = writeSuiteMeans(means.getDirsBySite());
            if (bySiteString != null) {
                g.drawString("Site mean: " + bySiteString, minX, minY + yPos);
            }
        }
    }
    
    private static String writeSuiteMeans(SuiteCalcs.Means means) {
        if (means == null || means.getAll() == null) {
            return null;
        }
        final FisherValues fv = means.getAll();
        final Vec3 meanDir = fv.getMeanDirection();
        return String.format("D %.2f / I %.2f / α95 %.2f / K %.2f",
                meanDir.getDecDeg(), meanDir.getIncDeg(),
                fv.getA95(), fv.getK());
    }
}
