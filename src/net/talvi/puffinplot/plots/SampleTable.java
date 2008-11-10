package net.talvi.puffinplot.plots;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.data.Sample;

public class SampleTable extends Plot {

    public SampleTable(GraphDisplay parent, PlotParams params, Rectangle2D dimensions) {
        super(parent, params, dimensions);
    }
    
    @Override
    public int getMargin() {
        return 12;
    }

    @Override
    public void draw(Graphics2D g) {
        Sample sample = params.getSample();
        if (sample==null) return;
                
        String line = null;
        switch (params.getMeasType()) {
        case DISCRETE: line = "Sample: " + sample.getName();
            break;
        case CONTINUOUS: line = "Depth: " + sample.getDepth();
            break;
        }
        line = line + ", Correction: " + params.getCorrection();
        g.drawString(line, (int) getDimensions().getMinX(), 
                (int) getDimensions().getMinY() + 16);
    }
    
}
