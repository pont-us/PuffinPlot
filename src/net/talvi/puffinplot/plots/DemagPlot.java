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
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MedianDestructiveField;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.plots.PlotAxis.AxisParameters;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

/**
 * Demagnetization plot. This is a simple biplot with demagnetization
 * step on the x axis and magnetic moment intensity on the y axis.
 * When magnetic susceptibility data is available, it is also overlaid
 * on the plot.
 * 
 * @author pont
 */
public class DemagPlot extends Plot {

    private final Preferences prefs;

    /** Creates a demagnetization plot with the supplied parameters
     * 
     * @param parent the graph display containing the plot
     * @param params the parameters of the plot
     * @param prefs the preferences containing the plot configuration
     */
    public DemagPlot(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
        this.prefs = prefs;
    }

    /** Returns this plot's internal name.
     * @return this plot's internal name */
    @Override
    public String getName() {
        return "demag";
    }

    /** Returns this plot's user-friendly name.
     * @return this plot's user-friendly name */
    @Override
    public String getNiceName() {
        return "Demag.";
    }
    
    /** Draws this plot. 
     * @param g the graphics object to which to draw the plot
     */
    @Override
    public void draw(Graphics2D g) {
        clearPoints();
        Sample sample = params.getSample();
        if (sample==null) return;
        List<Datum> data = sample.getVisibleData();
        if (data.isEmpty()) return;

        Rectangle2D dim = cropRectangle(getDimensions(), 270, 200, 50, 250);
        g.setColor(Color.BLACK);
        double maxDemag = Datum.maxTreatmentLevel(data);
        double maxIntens = Datum.maxIntensity(data);

        // If all the measurements have the same demag level, we'll
        // just plot them in sequence to avoid giving them all the same
        // X co-ordinate.
        boolean xBySequence = false;

        double xAxisLength;
        if (maxDemag == 0) {
            xAxisLength = data.size() > 1 ? data.size() : 1;
            xBySequence = true;
        } else {
            xAxisLength = maxDemag;
        }
        if (maxIntens == 0) maxIntens = 1;

        TreatType treatType = sample.getDatum(sample.getNumData() - 1).getTreatType();
        final String xAxisLabel;
        double demagRescale = 1;
        if (xBySequence) {
            xAxisLabel = "Measurement number";
        } else {
            String unit = treatType.getUnit();
            if (treatType.involvesAf()) {
                unit = "m" + unit;
                demagRescale = 1000;
            }
            xAxisLabel = String.format("%s (%s)", treatType.getAxisLabel(), unit);
        }
        AxisParameters hAxisParams = new AxisParameters(xAxisLength * demagRescale, Direction.RIGHT).
                withLabel(xAxisLabel).withNumberEachTick();
        final MedianDestructiveField midpoint = sample.getMdf();
        if (midpoint != null && midpoint.isHalfIntReached()) {
            hAxisParams.markedPosition = midpoint.getDemagLevel() * demagRescale;
        }
        final PlotAxis hAxis = new PlotAxis(hAxisParams, this);
        final String vAxisLabel = prefs.get("plots."+getName()+".vAxisLabel",
                "Magnetization (A/m)");
        final PlotAxis vAxis =
                new PlotAxis(new AxisParameters(maxIntens, Direction.UP).
                withLabel(vAxisLabel).withNumberEachTick(), this);
        
        double hScale = dim.getWidth() / hAxis.getLength();
        final double vScale = dim.getHeight() / vAxis.getLength();
        
        vAxis.draw(g, vScale, (int)dim.getMinX(), (int)dim.getMaxY());
        hAxis.draw(g, hScale, (int)dim.getMinX(), (int)dim.getMaxY());
        addPoint(null, new Point2D.Double(dim.getMinX()-10, dim.getMaxY()),
                true, false, false);
        
        int i = 0;
        for (Datum d: data) {
            double demagLevel = d.getTreatmentLevel() * demagRescale;
            double xPos = dim.getMinX() +
                    (xBySequence ? (i + 1) : demagLevel) * hScale;
            addPoint(d, new Point2D.Double(xPos,
                    dim.getMaxY() - d.getIntensity() * vScale),
                    true, false, i>0);
            i++;
        }

        if (hAxisParams.markedPosition != null) {
            final double xPos = dim.getMinX() + hScale * hAxisParams.markedPosition;
            final double yPos = dim.getMaxY() - midpoint.getIntensity() * vScale;
            g.draw(new Line2D.Double(dim.getMinX(), yPos,
                    xPos, yPos));
            g.draw(new Line2D.Double(xPos, dim.getMaxY()-getFontSize()*1.5,
                    xPos, yPos));
        }

        if (sample.hasMsData()) {
            addPoint(null, new Point2D.Double(dim.getMaxX() + 10, dim.getMaxY()),
                    false, false, false);
            final AxisParameters msAxisParams =
                    new AxisParameters(Datum.maxMagSus(data),
                    Direction.UP).withNumberEachTick();
            msAxisParams.label = "Mag. sus. (S.I.)";
            msAxisParams.farSide = true;
            final PlotAxis msAxis = new PlotAxis(msAxisParams, this);
            final double msScale = dim.getHeight() / msAxis.getLength();
            msAxis.draw(g, msScale, (int)dim.getMaxX(), (int)dim.getMaxY());
            i = 0;
            boolean first = true;
            for (Datum d: data) {
            final double xPos = dim.getMinX() +
                    (xBySequence ? (i + 1) : d.getTreatmentLevel()) * hScale;
            double magSus = d.getMagSus();
            if (magSus < 0) magSus = 0;
            if (d.hasMagSus()) {
                addPoint(d, new Point2D.Double(xPos,
                    dim.getMaxY() - magSus * msScale),
                    false, false, !first);
                first = false;
            }
            i++;
            }
        }

        drawPoints(g);
    }
}
