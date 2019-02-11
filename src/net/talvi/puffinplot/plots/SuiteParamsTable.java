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

import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.SuiteCalcs;

/**
 * A table which displays site location and VGP data.
 */
public class SuiteParamsTable extends Plot {

    private final double us = getUnitSize();
    private final List<Double> xSpacing =
            Arrays.asList(600*us, 400*us, 400*us, 400*us,
                    400*us, 350*us, 550*us);
    private final int ySpacing = (int) (120 * getUnitSize());
    private final List<String> headers = 
            Arrays.asList(new String[] {"Param", "dec/φ", "inc/λ", "α95",
                "k", "N", "R"});
    
    /** Creates a suite parameter table with the supplied parameters.
     * 
     * @param params the parameters of the plot
     */
    public SuiteParamsTable(PlotParams params) {
        super(params);
    }
    
    @Override
    public String getName() {
        return "suite_table";
    }
    
    @Override
    public String getNiceName() {
        return "Suite table";
    }
        
    private static String fmt(double value) {
        return String.format(Locale.ENGLISH, "%.1f", value);
    }
    
    private static String fmt(double value, int dp) {
        final String formatString =
                String.format(Locale.ENGLISH, "%%.%df", dp);
        return String.format(Locale.ENGLISH, formatString, value);
    }
            
    private static String fmt(int value) {
        return String.format(Locale.ENGLISH, "%d", value);
    }

    @Override
    public void draw(Graphics2D g) {
        clearPoints();
        final Sample sample = params.getSample();
        if (sample==null) return;
        final Suite suite = sample.getSuite();
        if (suite==null) return;
        final SuiteCalcs suiteCalcs = suite.getSuiteMeans();
        if (suiteCalcs==null) return;
        
        points.add(new TextLinePoint(this, g, 10, null, null, headers,
                xSpacing, Color.BLACK));

        final int columns = headers.size();
        float yPos = 2 * ySpacing;

        for (int type=0; type<2; type++) {
            for (int grouping=0; grouping<2; grouping++) {
                final SuiteCalcs.Means means;
                if (type==0 /* direction */) {
                    means = grouping==0 ?
                            suiteCalcs.getDirsBySite() :
                            suiteCalcs.getDirsBySample();
                } else /* VGP */ {
                    means = grouping==0 ?
                            suiteCalcs.getVgpsBySite() :
                            suiteCalcs.getVgpsBySample();
                }
                final List<String> values = new ArrayList<>(columns);
                values.addAll(Collections.nCopies(columns, "--"));
                values.set(0, (grouping==0 ? "Site " : "Sample ") +
                        (type==0 ? "dir" : "VGP"));
                
                if (means != null && means.getAll() != null &&
                        means.getAll().getMeanDirection() != null) {
                    final FisherValues all = means.getAll();
                    values.set(1, fmt(all.getMeanDirection().getDecDeg()));
                    values.set(2, fmt(all.getMeanDirection().getIncDeg()));
                    values.set(3, fmt(all.getA95()));
                    values.set(4, fmt(all.getK()));
                    values.set(5, fmt(all.getN()));
                    values.set(6, fmt(all.getR(), 4));
                }
                points.add(new TextLinePoint(this, g, yPos, null, null,
                        values, xSpacing, Color.BLACK));
                yPos += ySpacing;
            }
        }

        g.setColor(Color.BLACK);
        drawPoints(g);
    }
    
}
