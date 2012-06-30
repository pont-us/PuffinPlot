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

        final List<String> headers2 = new ArrayList<String>(headers);
        if (sample.getDatum(0).getTreatType() == TreatType.THERMAL)
            headers2.set(0, "temp.");
        points.add(new TextLinePoint(this, g, 10, null, null, headers2, xSpacing));
        final boolean useSequence = (Datum.maxTreatmentLevel(data) == 0);
        int sequence = 1;
        float yPos = 2 * ySpacing;
        for (Datum d: data) {
            if (yPos > getDimensions().getHeight()) break;
            final List<String> values = new ArrayList<String>(4);
            final Vec3 p = d.getMoment(params.getCorrection());
            String demag = useSequence ? Integer.toString(sequence)
                    : format("%.0f", d.getTreatmentLevel());
            if (d.getTreatType().getUnit().equals("T")) {
                // turn T into mT
                demag = format("%.0f", d.getTreatmentLevel() * 1000);
            }
            values.add(demag);
            values.add(format("%.1f", p.getDecDeg()));
            values.add(format("% .1f", p.getIncDeg()));
            // Don't use .1g, it tickles a bug in Java (#6469160) which
            // throws an ArrayFormatException (at least in Sun Java 5 & 6)
            values.add(format("%.2e", p.mag()));
            values.add(format("%.1e", d.getMagSus()));
            points.add(new TextLinePoint(this, g, yPos, d, null, values, xSpacing));
            yPos += ySpacing;
            sequence++;
        }
        g.setColor(Color.BLACK);
        drawPoints(g);
    }
}