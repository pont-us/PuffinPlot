package net.talvi.puffinplot.plots;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.List;

import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.Sample;

public class DemagPlot extends Plot {

    private static final long serialVersionUID = 1L;
    private static int margin = 40;
    
    public DemagPlot(PlotParams params) {
        super(params);
        withLines = true;
    }
    
    @Override
    public void paint(Graphics g1) {
        Sample sample = params.getSample();
        if (sample==null) return;
        List<Datum> data = sample.getData();
                if (data.size() == 0) return;
        
        Graphics2D g = (Graphics2D) g1;
        g.setRenderingHints(renderingHints);
        // transform = AffineTransform.getTranslateInstance(margin, getHeight()-margin);
        transform = (AffineTransform.getScaleInstance
                (((double) getWidth()) / getVirtualWidth(),
                ((double) getHeight()) / getVirtualHeight()));
        transform.concatenate(AffineTransform.getTranslateInstance(margin, getVirtualHeight()-margin));
        g.transform(transform);

        clearPoints();
        
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
        
        double hScale = (getVirtualWidth()-2*margin)/ hAxis.getLength();
        double vScale = (getVirtualHeight()-2*margin)/ vAxis.getLength();
        
        vAxis.draw(g, vScale);
        hAxis.draw(g, hScale);
        
        for (Datum d: data)
            addPoint(d, new Point2D.Double(d.getDemagLevel() * hScale, -d.getIntensity() * vScale), true);
        drawPoints(g);
    }

    @Override
    protected int getVirtualWidth() { return 300; }
    
    @Override
    protected int getVirtualHeight() { return 400; }
    
}
