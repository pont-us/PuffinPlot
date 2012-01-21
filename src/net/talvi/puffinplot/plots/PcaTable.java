package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.GreatCircle;
import net.talvi.puffinplot.data.GreatCircles;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import net.talvi.puffinplot.data.PcaValues;
import net.talvi.puffinplot.data.Sample;

/**
 * A textual display of a set of PCA parameters.
 * 
 * @author pont
 */
public class PcaTable extends Plot {

    private static final int X_SPACE = 100;
    private static final int Y_SPACE = 20;
    
    /** Creates a new PCA table with the supplied parameters.
     * 
     * @param parent the graph display containing the table
     * @param params the parameters of the table
     * @param prefs the preferences containing the table configuration
     */
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
        final GreatCircle gc = sample.getGreatCircle();
        
        List<String> strings = new ArrayList<String>(10);
        
        if (pca != null) {
            strings.addAll(Arrays.asList(
                String.format("PCA Dec %.2f", pca.getDirection().getDecDeg()),
                String.format("Inc %.2f", pca.getDirection().getIncDeg()),
                String.format("PCA MAD1 %.2f", pca.getMad1()),
                String.format("MAD3 %.2f", pca.getMad3()),
                pca.getEquation(), ""));
        }
        
        if (gc != null) {
            strings.addAll(Arrays.asList(
                String.format("GC Dec %.2f", gc.getPole().getDecDeg()),
                String.format("Inc %.2f", gc.getPole().getIncDeg()),
                String.format("GC MAD1 %.2f", gc.getMad1())));
        }

        
        if (!strings.isEmpty()) {
            g.setColor(Color.BLACK);

            for (int i=0; i<strings.size(); i++) {
              final int x = i % 2;
              final int y = i / 2;
              writeString(g, strings.get(i),
                      (int) getDimensions().getMinX() + x * X_SPACE,
                      (int) getDimensions().getMinY() + (y+1) * Y_SPACE);
            }
        }
    }

}
