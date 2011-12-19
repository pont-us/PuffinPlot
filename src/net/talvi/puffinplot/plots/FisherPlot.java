package net.talvi.puffinplot.plots;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import static java.lang.Math.min;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.PuffinPrefs;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.PcaValues;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Vec3;

public class FisherPlot extends EqAreaPlot {

    private final Stroke dashedStroke =
            new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            10.0f * getUnitSize(), new float[] {1,0,1}, 0);
    
    private final Stroke thinStroke =
            new BasicStroke(getUnitSize() / 2.0f);
    
    private boolean groupedBySite = true;
    
    public FisherPlot(GraphDisplay parent, PlotParams params,
            Rectangle2D dimensions, Preferences prefs) {
        super(parent, params, prefs);
        this.dimensions = dimensions;
    }

    @Override
    public String getName() {
        return "fisherplot";
    }

    @Override
    public void draw(Graphics2D g) {
        updatePlotDimensions(g);
        clearPoints();
        drawAxes();
        Suite suite = PuffinApp.getInstance().getSuite();
        if (suite==null) return;
        List<FisherValues> fishers = groupedBySite 
                ? suite.getSiteFishers()
                : Collections.singletonList(suite.getSuiteFisher());
        if (fishers==null) return;

        final AlphaComposite translucent =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .05f);
        final AlphaComposite opaque =
                AlphaComposite.getInstance(AlphaComposite.SRC);

        boolean firstPoint = true;
        for (FisherValues fisher: fishers) {
            final Vec3 v = fisher.getMeanDirection();
            Point2D meanPoint = project(v);
            addPoint(null, meanPoint, v.z>0, firstPoint, !firstPoint);

            GeneralPath ellipse = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 72);
            boolean firstEllipsePoint = true;
            for (double dec = 0; dec < 360; dec += 5) {
                Vec3 circlePoint =
                        (Vec3.fromPolarDegrees(1, 90-fisher.getA95(), dec));
                Vec3 w = circlePoint.rotY(Math.PI / 2 - v.getIncRad());
                w = w.rotZ(v.getDecRad());
                Point2D.Double p = project(w);
                if (firstEllipsePoint) ellipse.moveTo((float) p.x, (float) p.y);
                else ellipse.lineTo((float) p.x, (float) p.y);
                firstEllipsePoint = false;
            }
            ellipse.closePath();
              
            g.setComposite(translucent);
            g.fill(ellipse);
            g.setComposite(opaque);
            Stroke oldStroke = g.getStroke();
            g.setStroke(thinStroke);
            g.draw(ellipse);
            
            if (groupedBySite) {
                g.setStroke(dashedStroke);
                for (Vec3 w : fisher.getDirections()) {
                    g.draw(new Line2D.Double(meanPoint, project(w)));
                }
                g.setStroke(oldStroke);
            } else {
                g.setStroke(thinStroke);
                for (Sample s: PuffinApp.getInstance().getSelectedSamples()) {
                    if (s == null) continue;
                    final PcaValues pca = s.getPcaValues();
                    if (pca == null) continue;
                    final Vec3 direction = pca.getDirection();
                    if (direction == null) continue;
                    final Point2D.Double p = project(direction);
                    g.draw(new Line2D.Double(p.x - 10, p.y, p.x + 10, p.y));
                    g.draw(new Line2D.Double(p.x, p.y - 10, p.x, p.y + 10));
                }
            }
            firstPoint = false;
        }
        drawPoints(g);
    }

    public boolean isGroupedBySite() {
        return groupedBySite;
    }

    public void setGroupedBySite(boolean groupedBySite) {
        this.groupedBySite = groupedBySite;
    }

}
