package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import net.talvi.puffinplot.data.Sample;

public class SampleTable extends Plot {

    public SampleTable(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
    }
    
    @Override
    public int getMargin() {
        return 12;
    }

    @Override
    public String getName() {
        return "sampletable";
    }

    @Override
    public String getNiceName() {
        return "Title";
    }

    @Override
    public void draw(Graphics2D g) {
        Sample sample = params.getSample();
        if (sample==null) return;
                
        g.setColor(Color.BLACK);
        Font oldFont = g.getFont();
        Font biggerFont = oldFont.deriveFont(getFontSize()*1.5f);
        g.setFont(biggerFont);
        
        String line = (params.getMeasType() == MeasType.DISCRETE ?
            "Sample: " : "Depth: ") + sample.getNameOrDepth()
                + ", Correction: " + params.getCorrection();
        if (params.isEmptyCorrectionActive()) line = line + " E";
        g.drawString(line, (int) getDimensions().getMinX(), 
                (int) getDimensions().getMinY() + 16);
        g.setFont(oldFont);
    }
    
}
