package net.talvi.puffinplot.plots;

import java.awt.AlphaComposite;
import static java.lang.Math.min;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.Suite;
import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.Vec3;

public class FisherEqAreaPlot extends EqAreaPlot {

    private List<FisherValues> testFishers;
    
    public FisherEqAreaPlot(GraphDisplay parent, PlotParams params,
            Rectangle2D dimensions) {
        super(parent, params, dimensions);
        
        testFishers = new ArrayList<FisherValues>(20);
        for (double inc=10; inc<=90; inc+=10) {
            testFishers.add(new FisherValues(10, 0, Vec3.fromPolarDegrees(1, inc, 0)));
        }

    }
    
    @Override
    public void draw(Graphics2D g) {
        final Rectangle2D dims = getDimensions();
        final int radius = (int) (min(dims.getWidth(), dims.getHeight()) / 2);
        final int xo = (int) dims.getCenterX();
        final int yo = (int) dims.getCenterY();
        
        clearPoints();
        drawAxes(g, xo, yo, radius);
        Suite suite = PuffinApp.getApp().getSuite();
        if (suite==null) return;
        List<FisherValues> fishers = suite.getFishers();
        if (fishers==null) return;
        
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .1f));
        boolean first = true;
        for (FisherValues fisher: fishers) {
            final Vec3 v = fisher.getMeanDirection();
            addPoint(null, project(v, xo, yo, radius), v.z>0, first, !first);
            
            Polygon ellipse = new Polygon();
            List<Vec3> circle = new ArrayList<Vec3>(36);
            for (double dec = 0; dec < 360; dec += 10) {
                circle.add(Vec3.fromPolarDegrees(1, 90-fisher.getA95(), dec));
            }
              for (Vec3 c: circle) {
                  Vec3 w = c.rotY(Math.PI/2 - v.incRadians());
                  Point2D p = project(w, xo, yo, radius);
                  ellipse.addPoint((int) p.getX(), (int) p.getY());
              }
              
              
//            double r = Math.toRadians(fisher.getA95());
//            for (double t=-Math.PI; t<Math.PI; t += 0.05) {
//                Vec3 w = v.addIncRad(r*Math.cos(t)).addDecRad(r*Math.sin(t));
//                Point2D p = project(w, xo, yo, radius);
//                ellipse.addPoint((int) p.getX(), (int) p.getY());
//            }
              
              g.fill(ellipse);
                
          
            first = false;
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
        drawPoints(g);
    }

}
