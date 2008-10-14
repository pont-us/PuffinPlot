package net.talvi.puffinplot.plots;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.List;

import javax.swing.JComponent;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.Sample;

public class DemagPlot extends Plot {

    private static final long serialVersionUID = 1L;
    private static int margin = 40;
    private GraphDisplay parent;
    private PlotParams params;
    
    public DemagPlot(PlotParams params, GraphDisplay parent) {
        this.params = params;
        this.parent = parent;
    }
    
    public void paint(Graphics2D g, int xOffs, int yOffs, int xSize, int ySize) {
        Sample sample = params.getSample();
        if (sample==null) return;
        List<Datum> data = sample.getData();
        if (data.size() == 0) return;
        
        
        // transform = AffineTransform.getTranslateInstance(margin, getHeight()-margin);
//        transform = (AffineTransform.getScaleInstance
//                (((double) getWidth()) / getVirtualWidth(),
//                ((double) getHeight()) / getVirtualHeight()));
//        transform.concatenate(AffineTransform.getTranslateInstance(margin, getVirtualHeight()-margin));
//        g.transform(transform);

        // clearPoints();
        
        double maxIntens = 0;
        double maxDemag = 0;
        for (Datum d: data) {
            if (d.getDemagLevel() > maxDemag) maxDemag = d.getDemagLevel();
            if (d.getIntensity() > maxIntens) maxIntens = d.getIntensity();
        }
        
        PlotAxis vAxis = new PlotAxis(maxIntens, PlotAxis.Direction.UP,
                PlotAxis.saneStepSize(maxIntens),
                "Intensity", null);
        PlotAxis hAxis = new PlotAxis(maxDemag, PlotAxis.Direction.RIGHT,
                PlotAxis.saneStepSize(maxDemag),
                sample.getDatum(sample.getNumData()-1).getTreatType().getAxisLabel(), null);
        
        double hScale = xSize / hAxis.getLength();
        double vScale = ySize / vAxis.getLength();
        
        vAxis.draw(g, vScale, xOffs, yOffs+ySize);
        hAxis.draw(g, hScale, xOffs, yOffs+ySize);
        
        for (Datum d: data)
            parent.addPoint(d, new Point2D.Double(xOffs + d.getDemagLevel() * hScale,
                    yOffs+ySize - d.getIntensity() * vScale), true, false);
        drawPoints(g);
    }

    
}
