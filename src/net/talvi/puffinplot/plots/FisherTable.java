package net.talvi.puffinplot.plots;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.Sample;

public class FisherTable extends Plot {

    public FisherTable(GraphDisplay parent, PlotParams params, Rectangle2D dimensions) {
        super(parent, params, dimensions);
    }
    
    @Override
    public int getMargin() {
        return 12;
    }

    
    @Override
    public void draw(Graphics2D g) {
        Sample sample = params.getSample();
        if (sample == null) return;
        
        FisherValues fish = sample.getFisher();
        if (fish != null) {
            float xOrig = (float) getDimensions().getMinX();
            float yOrig = (float) getDimensions().getMinY();
            g.drawString(
                    String.format("Dec %.2f  Inc %.2f",
                    fish.getMeanDirection().decDegrees(),
                    fish.getMeanDirection().incDegrees()), xOrig, yOrig+16);
            g.drawString(String.format(/*"α*/"a95 %.2f  k %.2f" 
                    ,fish.getA95(), fish.getK()), xOrig, yOrig + 32);
        }
    }

}
