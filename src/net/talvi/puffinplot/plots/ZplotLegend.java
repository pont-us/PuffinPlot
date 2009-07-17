package net.talvi.puffinplot.plots;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;

public class ZplotLegend extends Plot {

    public ZplotLegend(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
    }

    @Override
    public String getName() {
        return "zplotlegend";
    }

    @Override
    public String getNiceName() {
        return "Zplot key";
    }

    @Override
    public int getMargin() {
        return 12;
    }

    @Override
    public void draw(Graphics2D g) {
        final Rectangle2D dims = getDimensions();
        clearPoints();
        double xOrig = dims.getMinX() + getMargin() + getUnitSize()*50;
        double yOrig = dims.getMinY() + getMargin();
        double textOffs = 25 * getUnitSize();
        double lineOffs = 150 * getUnitSize();
        addPoint(null, new Point2D.Double(xOrig, yOrig), false, false, false);
        addPoint(null, new Point2D.Double(xOrig, yOrig + lineOffs), true, false, false);
        writeString(g, "vertical", (float) xOrig + 50 * getUnitSize(),
                (float) (yOrig + textOffs));
        writeString(g, "horizontal", (float) (xOrig + 50 * getUnitSize()),
                (float) (yOrig + lineOffs + textOffs));
        writeString(g, "Units: Gauss", (float) xOrig,
                (float) (yOrig + 2 * lineOffs + textOffs));
        drawPoints(g);
    }

}
