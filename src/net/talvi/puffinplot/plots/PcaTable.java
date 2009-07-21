package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.data.PcaValues;
import net.talvi.puffinplot.data.Sample;

public class PcaTable extends Plot {

    private static final int X_SPACE = 100;
    private static final int Y_SPACE = 20;
    
    public PcaTable(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
    }

    @Override
    public String getName() {
        return "pcatable";
    }

    @Override
    public String getNiceName() {
        return "PCA";
    }

    @Override
    public int getMargin() {
        return 12;
    }
    
    @Override
    public void draw(Graphics2D g) {
        Sample sample = params.getSample();
        if (sample==null) return;
 
        final PcaValues pca = sample.getPcaValues();
        
        if (pca != null) {
            String[] strings = {
                String.format("Dec %.2f", pca.getDecDegrees()),
                String.format("Inc %.2f", pca.getIncDegrees()),
                String.format("MAD1 %.2f", pca.getMad1()),
                String.format("MAD3 %.2f", pca.getMad3())
            };
            
            g.setColor(Color.BLACK);

            for (int x=0; x<2; x++)
                for (int y=0; y<2; y++)
                    writeString(g, strings[2*y+x],
                            (int) getDimensions().getMinX() + x * X_SPACE,
                            (int) getDimensions().getMinY() + (y+1) * Y_SPACE);
        }
    }

}
