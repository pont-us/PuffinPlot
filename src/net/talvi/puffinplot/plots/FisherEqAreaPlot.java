package net.talvi.puffinplot.plots;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import static java.lang.Math.min;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.Suite;
import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Vec3;

public class FisherEqAreaPlot extends EqAreaPlot {

    private final Stroke dashedStroke =
            new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            10.0f * getUnitSize(), new float[] {1,0,1}, 0);
    
    private final Stroke thinStroke =
            new BasicStroke(getUnitSize() / 2.0f);
    
    private boolean groupedBySite = true;
    
    public FisherEqAreaPlot(GraphDisplay parent, PlotParams params,
            Rectangle2D dimensions) {
        super(parent, params, dimensions);
    }
    
    @Override
    public void draw(Graphics2D g) {
        final Rectangle2D dims = getDimensions();
        final int radius = (int) (min(dims.getWidth(), dims.getHeight()) / 2);
        final int xo = (int) dims.getCenterX();
        final int yo = (int) dims.getCenterY();
        
        clearPoints();
        drawAxes(g, xo, yo, radius);
        Suite suite = PuffinApp.getInstance().getSuite();
        if (suite==null) return;
        List<FisherValues> fishers = groupedBySite 
                ? suite.getFishers()
                : Collections.singletonList(suite.getSuiteFisher());
        if (fishers==null) return;

        final AlphaComposite translucent =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .05f);
        final AlphaComposite opaque =
                AlphaComposite.getInstance(AlphaComposite.SRC);

        boolean first = true;
        for (FisherValues fisher: fishers) {
            final Vec3 v = fisher.getMeanDirection();
            Point2D meanPoint = project(v, xo, yo, radius);
            addPoint(null, meanPoint, v.z>0, first, !first);
            
            Polygon ellipse = new Polygon();
            List<Vec3> circle = new ArrayList<Vec3>(36);
            for (double dec = 0; dec < 360; dec += 5) {
                circle.add(Vec3.fromPolarDegrees(1, 90-fisher.getA95(), dec));
            }
            for (Vec3 c: circle) {
                Vec3 w = c.rotY(Math.PI / 2 - v.incRadians());
                w = w.rotZ(v.decRadians());
                Point2D p = project(w, xo, yo, radius);
                ellipse.addPoint((int) p.getX(), (int) p.getY());
            }
              
            g.setComposite(translucent);
            g.fill(ellipse);
            g.setComposite(opaque);
            Stroke oldStroke = g.getStroke();
            g.setStroke(thinStroke);
            g.draw(ellipse);
            
            if (groupedBySite) {
                g.setStroke(dashedStroke);
                for (Vec3 w : fisher.getDirections()) {
                    g.draw(new Line2D.Double(meanPoint, project(w, xo, yo, radius)));
                }
                g.setStroke(oldStroke);
            } else {
                g.setStroke(thinStroke);
                for (Sample s: PuffinApp.getInstance().getSelectedSamples()) {
                    if (s != null && s.getPca() != null && s.getPca().getDirection() != null) {
                        Point2D p = project(s.getPca().getDirection(), xo, yo, radius);
                        g.draw(new Line2D.Double(p.getX() - 10, p.getY(), p.getX() + 10, p.getY()));
                        g.draw(new Line2D.Double(p.getX(), p.getY() - 10, p.getX(), p.getY() + 10));
                    }
                }
            }

            first = false;
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
