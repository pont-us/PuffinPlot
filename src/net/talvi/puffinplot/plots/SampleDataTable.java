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
 * A textual display of a set of PCA and great circle parameters
 * for a single sample.
 * 
 * @author pont
 */
public class SampleDataTable extends Plot {

    /** Creates a new PCA table with the supplied parameters.
     * 
     * @param parent the graph display containing the table
     * @param params the parameters of the table
     * @param prefs the preferences containing the table configuration
     */
    public SampleDataTable(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
    }

    @Override
    public String getName() {
        return "pcatable";
    }

    @Override
    public String getNiceName() {
        return "Sample parameters";
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
                
        if (gc != null) {
            strings.addAll(Arrays.asList(
                String.format("GC  dec %.2f / inc %.2f", gc.getPole().getDecDeg(),
                    gc.getPole().getIncDeg()),
                String.format("GC  MAD1 %.2f", gc.getMad1())));
        }
        
        if (pca != null) {
            strings.addAll(Arrays.asList(
                String.format("PCA  dec %.2f / inc %.2f",
                    pca.getDirection().getDecDeg(),
                    pca.getDirection().getIncDeg()),
                String.format("PCA  MAD1 %.2f / MAD3 %.2f",
                    pca.getMad1(), pca.getMad3()),
                pca.getEquation()));
        }
        
        if (!strings.isEmpty()) {
            g.setColor(Color.BLACK);
            for (int i=0; i<strings.size(); i++) {
              writeString(g, strings.get(i),
                      (int) getDimensions().getMinX() + 10,
                      (int) (getDimensions().getMinY() + (i+1) * getFontSize() * 1.2));
            }
        }
    }
}
