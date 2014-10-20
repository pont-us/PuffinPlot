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
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

/**
 * A table showing some of the demagnetization data.
 * At present the columns are: treatment step,
 * declination, inclination, intensity, magnetic susceptibility.
 * In the future they may become configurable.
 * 
 * @author pont
 */
public class DemagTable extends Plot {

    private final double us = getUnitSize();
    private final List<Double> xSpacing =
            Arrays.asList(360*us, 420*us, 420*us, 620*us, 580*us);
    private final int ySpacing = (int) (120 * getUnitSize());
    private final List<String> headers = 
            Arrays.asList(new String[] {"demag.", "dec.", "inc.", "int.", "m.s."});
    
    /** Creates a data table with the supplied parameters
     * 
     * @param parent the graph display containing the table
     * @param params the parameters of the table
     * @param prefs the preferences containing the table configuration
     */
    public DemagTable(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
    }

    /** Returns the internal plot name for this table. 
     * @return the internal plot name for this table */
    @Override
    public String getName() {
        return "datatable";
    }

    /** Returns the user-friendly plot name for this table. 
     * @return the user-friendly plot name for this table */
    @Override
    public String getNiceName() {
        return "Data table";
    }

    /** Draws the table. 
     * @param g the graphics object to which to draw the table */
    @Override
    public void draw(Graphics2D g) {
        clearPoints();

        final Sample sample = params.getSample();
        if (sample==null) return;
        final List<Datum> data = sample.getData();
        if (data.isEmpty()) return;

        final List<String> headers2 = new ArrayList<>(headers);
        if (sample.getDatum(0).getTreatType() == TreatType.THERMAL)
            headers2.set(0, "temp.");
        points.add(new TextLinePoint(this, g, 10, null, null, headers2, xSpacing, Color.BLACK));
        final boolean useSequence = (Datum.maxTreatmentLevel(data) == 0);
        int sequence = 1;
        float yPos = 2 * ySpacing;
        for (Datum d: data) {
            if (yPos > getDimensions().getHeight()) break;
            final List<String> values = new ArrayList<>(4);
            final Vec3 p = d.getMoment(params.getCorrection());
            final String demag = useSequence ? Integer.toString(sequence)
                    : d.getFormattedTreatmentLevel();
            values.add(demag);
            values.add(format(Locale.ENGLISH, "%.1f", p.getDecDeg()));
            values.add(format(Locale.ENGLISH, "% .1f", p.getIncDeg()));
            // Don't use .1g, it tickles a bug in Java (#6469160) which
            // throws an ArrayFormatException (at least in Sun Java 5 & 6)
            values.add(format(Locale.ENGLISH, "%.2e", p.mag()));
            values.add(Double.isNaN(d.getMagSus()) ? "-" :
                       format(Locale.ENGLISH, "%.1e", d.getMagSus()));
            points.add(new TextLinePoint(this, g, yPos, d, null, values, xSpacing, Color.BLACK));
            yPos += ySpacing;
            sequence++;
        }
        g.setColor(Color.BLACK);
        drawPoints(g);
    }
}
