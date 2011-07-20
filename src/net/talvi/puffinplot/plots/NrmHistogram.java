package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.plots.PlotAxis.AxisParameters;
import net.talvi.puffinplot.plots.PlotAxis.Direction;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

/**
 *
 * @author pont
 */
public class NrmHistogram extends Plot {

    private final Preferences prefs;

    public NrmHistogram(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
        this.prefs = prefs;
    }
    
    @Override
    public String getName() {
        return "nrm_histogram";
    }

    @Override
    public void draw(Graphics2D g) {
        clearPoints();
        final Sample sample = params.getSample();
        if (sample==null) return;
        Suite suite = sample.getSuite();
        List<Double> nrms = new ArrayList<Double>(suite.getSamples().size());
        for (Sample s: suite.getSamples()) {
            if (s.hasData()) {
                nrms.add(s.getNRM(Correction.NONE));
            }
        }
        final double minimum = 0; //Collections.min(nrms);
        final double maximum = Collections.max(nrms);
        final double range = maximum-minimum;
        final int nBuckets = 50;
        Rectangle2D dim = cropRectangle(getDimensions(), 270, 200, 50, 250);
        List<Integer> buckets = new ArrayList<Integer>(Collections.nCopies(nBuckets, 0));
        for (double nrm: nrms) {
            int bucket = (int) Math.round(nBuckets * (nrm-minimum)/range);
            if (bucket==nBuckets) bucket--; // rightmost edge of rightmost bucket
            buckets.set(bucket, 1+buckets.get(bucket));
        }

        AxisParameters hAxisParams = new AxisParameters(range, Direction.RIGHT).
                withLabel("NRM (A/m)").withNumberEachTick().withStartValue(minimum);
        final PlotAxis hAxis = new PlotAxis(hAxisParams, this);
        final PlotAxis vAxis =
                new PlotAxis(new AxisParameters(Collections.max(buckets), Direction.UP).
                withLabel("Number of samples").withNumberEachTick(), this);
        
        final double hScale = dim.getWidth() / hAxis.getLength();
        final double vScale = dim.getHeight() / vAxis.getLength();
        
        vAxis.draw(g, vScale, (int)dim.getMinX(), (int)dim.getMaxY());
        hAxis.draw(g, hScale, (int)dim.getMinX(), (int)dim.getMaxY());
        
        for (int i = 0; i < nBuckets; i++) {
            double xPos = dim.getMinX() + (((double) i) / (double) nBuckets) * range * hScale;
            //double yPos = dim.getMaxY() - buckets.get(i) * vScale;
            Rectangle2D r = new Rectangle2D.Double(xPos,
                    dim.getMaxY() - buckets.get(i) * vScale,
                    range * hScale/(double)nBuckets,
                    buckets.get(i) * vScale);
            g.setColor(Color.gray);
            g.fill(r);
        }

        //drawPoints(g);
    }

    @Override
    public String getNiceName() {
        return "NRM Histogram";
    }
}
