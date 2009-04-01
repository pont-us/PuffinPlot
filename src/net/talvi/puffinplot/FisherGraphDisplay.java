package net.talvi.puffinplot;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import net.talvi.puffinplot.plots.FisherEqAreaPlot;

public class FisherGraphDisplay extends GraphDisplay {

    public FisherGraphDisplay() {

        super();

        zoomTransform = AffineTransform.getScaleInstance(0.5, 0.5);

        plots.add(
                new FisherEqAreaPlot(
                null, null, new Rectangle2D.Double(50, 50, 1200, 1200)));

    }

}
