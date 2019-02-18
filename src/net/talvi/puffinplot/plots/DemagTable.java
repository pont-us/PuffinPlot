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

/**
 * A table showing some of the data from individual treatment steps of a single
 * sample. Since the most usual treatments are demagnetizing ones, it is called
 * {@code DemagTable}. At present the columns are: treatment level (or step
 * number, if no level is known), declination, inclination, intensity, magnetic
 * susceptibility. In the future they may become configurable.
 *
 * @author pont
 */
public class DemagTable extends Plot {

    private final List<String> defaultHeaders = Arrays.asList(
            new String[] {"demag.", "dec.", "inc.", "int.", "m.s."});
    
    /**
     * Creates a demagnetization table with the supplied parameters
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

        final List<String> headers = new ArrayList<>(defaultHeaders);
        if (sample.getTreatmentStepByIndex(0).getTreatmentType() ==
                TreatmentType.THERMAL) {
            headers.set(0, "temp.");
        }
        
        final List<Double> xSpacing = Stream.of(360., 450., 450., 660., 580.).
                map(x -> x * getUnitSize()).collect(Collectors.toList());
        final int ySpacing = (int) (120 * getUnitSize());
        
        points.add(new TextLinePoint(this, graphics, 10, null, null, headers,
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
            final List<String> values = new ArrayList<>(5);
            final Vec3 moment = step.getMoment(params.getCorrection());
            final String demag = useSequence ? Integer.toString(sequence)
                    : step.getFormattedTreatmentLevel();
            values.add(demag);
            values.add(angleFormat.format(moment.getDecDeg()));
            values.add(angleFormat.format(moment.getIncDeg()));
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
