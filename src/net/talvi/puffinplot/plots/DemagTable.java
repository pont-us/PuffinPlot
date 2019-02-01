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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.talvi.puffinplot.Util;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.window.PlotParams;

/**
 * A table showing some of the demagnetization data. At present the columns are:
 * treatment step, declination, inclination, intensity, magnetic susceptibility.
 * In the future they may become configurable.
 * 
 * @author pont
 */
public class DemagTable extends Plot {

    private final List<String> headers = Arrays.asList(
            new String[] {"demag.", "dec.", "inc.", "int.", "m.s."});
    
    /**
     * Creates a data table with the supplied parameters
     *
     * @param params the parameters of the table
     */
    public DemagTable(PlotParams params) {
        super(params);
    }

    /**
     * Returns the internal plot name for this table.
     *
     * @return the internal plot name for this table
     */
    @Override
    public String getName() {
        return "datatable";
    }

    /**
     * Returns the user-friendly plot name for this table.
     *
     * @return the user-friendly plot name for this table
     */
    @Override
    public String getNiceName() {
        return "Data table";
    }

    /**
     * Draws the table.
     *
     * @param graphics the graphics object to which to draw the table
     */
    @Override
    public void draw(Graphics2D graphics) {
        clearPoints();

        final Sample sample = params.getSample();
        if (sample==null) {
            return;
        }
        
        final List<TreatmentStep> steps = sample.getTreatmentSteps();
        if (steps.isEmpty()) {
            return;
        }

        final List<String> headers2 = new ArrayList<>(headers);
        if (sample.getTreatmentStepByIndex(0).getTreatmentType() ==
                TreatmentType.THERMAL) {
            headers2.set(0, "temp.");
        }
        
        final double unitSize = getUnitSize();
        final List<Double> xSpacing = Stream.of(360., 450., 450., 660., 580.).
                map(x -> x*unitSize).collect(Collectors.toList());
        final int ySpacing = (int) (120 * unitSize);
        
        points.add(new TextLinePoint(this, graphics, 10, null, null, headers2,
                xSpacing, Color.BLACK));
        final boolean useSequence =
                (TreatmentStep.maxTreatmentLevel(steps) == 0);
        int sequence = 1;
        float yPos = 2 * ySpacing;
        final DecimalFormat angleFormat = 
                new DecimalFormat("0.0;-0.0", Util.getDecimalFormatSymbols());
        final DecimalFormat intensityFormat =
                new DecimalFormat("0.00E00", Util.getDecimalFormatSymbols());
        final DecimalFormat magSusFormat =
                new DecimalFormat("0.0E00", Util.getDecimalFormatSymbols());
        for (TreatmentStep step: steps) {
            if (yPos > getDimensions().getHeight()) {
                break;
            }
            final List<String> values = new ArrayList<>(4);
            final Vec3 moment = step.getMoment(params.getCorrection());
            final String demag = useSequence ? Integer.toString(sequence)
                    : step.getFormattedTreatmentLevel();
            values.add(demag);
            values.add(angleFormat.format(moment.getDecDeg()));
            values.add(angleFormat.format(moment.getIncDeg()));
            /*
             * Don't use .1g, it tickles a bug in Java (#6469160) which throws
             * an ArrayFormatException (at least in Sun Java 5 & 6). Update:
             * apparently fixed in Java 8 -- see
             * http://bugs.java.com/view_bug.do?bug_id=6469160 . No reason to
             * change the format at the moment, though.
             */
            values.add(intensityFormat.format(moment.mag()));
            values.add(Double.isNaN(step.getMagSus()) ? "—" :
                       magSusFormat.format(step.getMagSus()));
            points.add(new TextLinePoint(this, graphics, yPos, step, null,
                    values, xSpacing, Color.BLACK));
            yPos += ySpacing;
            sequence++;
        }
        graphics.setColor(Color.BLACK);
        drawPoints(graphics);
    }
}
