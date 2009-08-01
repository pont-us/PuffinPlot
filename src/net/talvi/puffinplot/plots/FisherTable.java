package net.talvi.puffinplot.plots;

import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedString;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.Sample;

public class FisherTable extends Plot {

    public FisherTable(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
    }
    
    @Override
    public int getMargin() {
        return 12;
    }

    @Override
    public String getName() {
        return "fishertable";
    }

    @Override
    public String getNiceName() {
        return "Fisher";
    }

    @Override
    public void draw(Graphics2D g) {
        Sample sample = params.getSample();
        if (sample == null) return;
        
        FisherValues fish = sample.getFisher();
        if (fish != null) {
            float xOrig = (float) getDimensions().getMinX();
            float yOrig = (float) getDimensions().getMinY();
        

               // as.addAttribute(TextAttribute.FONT,
                 //       text.length()-exp.length(), text.length());

            writeString(g,
                    String.format("Dec %.2f  Inc %.2f",
                    fish.getMeanDirection().getDecDeg(),
                    fish.getMeanDirection().getIncDeg()), xOrig, yOrig+16);
            String s = String.format("α95 %.2f  k %.2f"
                    ,fish.getA95(), fish.getK());
            AttributedString as = new AttributedString(s);
            as.addAttributes(getTextAttributes(), 0, s.length());
            // A nasty little hack to deal with the alpha: if printing in
            // a font that doesn't have an alpha, the whole string will
            // fall back to the default font. So we add a dummy attribute
            // to the first character to make sure it's in a different
            // style run; this means that a font substitution on the alpha
            // won't affect the whole string. (See bug #32.)
            as.addAttribute(TextAttribute.SIZE, getFontSize()+0.00001f, 0, 1);
            FontRenderContext frc = g.getFontRenderContext();
            TextLayout layout = new TextLayout(as.getIterator(), frc);
            layout.draw(g, xOrig, yOrig + 32);
        }
    }

}
