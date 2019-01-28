/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2018 Pontus Lurcock.
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
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.Sample;

/**
 * <p>A ‘data point’ which actually consists of a line of text.</p>
 * 
 * @author pont
 */
class TextLinePoint implements PlotPoint {

    private final Plot plot;
    private final TreatmentStep treatmentStep;
    private final Sample sample;
    private final double yPos;
    private final List<Double> xSpacing;
    private Rectangle2D bbox;
    private final List<AttributedCharacterIterator> strings;
    private final double xMin;
    private final Color colour;

    public TextLinePoint(Plot plot, Graphics2D g, double yOffset, TreatmentStep d,
            Sample sample, List<String> values, List<Double> xSpacing,
            Color colour) {
        this.plot = plot;
        this.treatmentStep = d;
        this.sample = sample;
        this.yPos = yOffset + plot.getDimensions().getMinY();
        this.xSpacing = xSpacing;
        this.strings = new ArrayList<>(values.size());
        this.colour = colour;
        double xPos = 10;
        xMin = plot.getDimensions().getMinX();
        final FontMetrics metrics = g.getFontMetrics();
        for (int i=0; i<values.size(); i++) {
            final String s = values.get(i);
            final double space = xSpacing.get(i);
            final AttributedString as = new AttributedString(s);
            plot.applyTextAttributes(as);
            final AttributedCharacterIterator ai = as.getIterator();
            strings.add(ai);
            final double x = xMin + xPos;
            final Rectangle2D b = metrics.getStringBounds(ai, 0, s.length(), g);
            final Rectangle2D b2 = new Rectangle2D.Double(b.getMinX() + x,
                    b.getMinY() + yPos, b.getWidth(), b.getHeight());
            if (bbox == null) {
                bbox = (Rectangle2D) b2.clone();
            } else {
                bbox.add(b2);
            }
            xPos += space;
        }
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(colour);
        if (treatmentStep != null) {
            if (treatmentStep.isSelected()) {
                plot.writeString(g, "*", (float) xMin, (float) yPos);
            }
            if (treatmentStep.isHidden()) {
                plot.writeString(g, "-", (float) xMin, (float) yPos);
            }
        }

        double x = xMin + 10;
        for (int i=0; i<strings.size(); i++) {
            final AttributedCharacterIterator s = strings.get(i);
            /*
             * Note: TextLayout is more accurate than
             * FontMetrics::getStringBounds, since the latter uses a plain
             * (non-attributed) character iterator -- see e.g.
             * https://stackoverflow.com/a/24019384/6947739 . There's still
             * a one-pixel horizontal offset between negative and
             * non-negative numbers; so far I've found no possible remedy for
             * this.
             */
            final TextLayout tl = new TextLayout(s, g.getFontRenderContext());
            final Rectangle2D bounds = tl.getBounds();
            final double space = xSpacing.get(i);
            final double xOffset = space - bounds.getWidth();
            g.drawString(s, (float) (x + xOffset), (float) yPos);
            x += space;
        }
    }

    @Override
    public void drawWithPossibleLine(Graphics2D g, PlotPoint prev,
            boolean annotate) {
        draw(g);
    }

    @Override
    public TreatmentStep getTreatmentStep() {
        return treatmentStep;
    }
    
    @Override
    public Sample getSample() {
        return sample;
    }

    @Override
    public Shape getShape() {
        return bbox;
    }

    @Override
    public Point2D getCentre() {
        return new Point2D.Double(bbox.getCenterX(), bbox.getCenterY());
    }

    @Override
    public boolean isNear(Point2D point, double distance) {
        return bbox.contains(point);
    }
}
