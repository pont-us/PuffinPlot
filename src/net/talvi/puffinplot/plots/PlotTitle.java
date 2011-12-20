package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import net.talvi.puffinplot.data.Sample;

/**
 * A title for the graph display, showing the sample identifier and
 * some other information.
 * 
 * @author pont
 */
public class PlotTitle extends Plot {

    /** Creates a plot title with the supplied parameters.
     * 
     * @param parent the graph display containing the plot title
     * @param params the parameters of the plot title
     * @param prefs the preferences containing the plot title configuration
     */
    public PlotTitle(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
    }
    
    @Override
    public int getMargin() {
        return 12;
    }

    @Override
    public String getName() {
        return "title";
    }

    @Override
    public String getNiceName() {
        return "Title";
    }

    @Override
    public void draw(Graphics2D g) {
        final Sample sample = params.getSample();
        if (sample==null || !sample.hasData()) return;

        g.setFont(Font.getFont(getTextAttributes()));
        g.setColor(Color.BLACK);
        final Font oldFont = g.getFont();
        final Font biggerFont = oldFont.deriveFont(getFontSize()*1.5f);
        g.setFont(biggerFont);
        final boolean discrete = sample.getMeasType().isDiscrete();
        final String line = (discrete ? "Sample: " : "Depth: ")
                + sample.getNameOrDepth().substring(0);
        //        + ", Correction: " + params.getCorrection().getDescription();
        g.drawString(line, (int) getDimensions().getMinX(), 
                (int) getDimensions().getMinY() + 16);
        g.setFont(oldFont);
        if (sample.getSite() != null) g.drawString(//"Slot: "+sample.getSlotNumber()+
                //", Runs: " + sample.getFirstRunNumber() +
                //"-" + sample.getLastRunNumber() + " "+
                "Site: "+sample.getSite().toString().substring(0),
                (int) getDimensions().getMinX(),
                (int) getDimensions().getMinY() + 32);
    }
    
}
