package net.talvi.puffinplot.plots;

import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedString;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.FisherParams;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Site;

public class SiteDataTable extends Plot {

    /* Using a real alpha character can be tricky: it should be OK if
     * the real alpha is simply absent from the font, since it should
     * just get substituted from the default font (which does contain it).
     * But buggy fonts can contain an incorrect or blank character in the
     * alpha position, so it's safer not to use it.
     */
    private static final boolean USE_REAL_ALPHA = false;
    private static final String alpha = USE_REAL_ALPHA ? "α" : "a";
    private static final String[] PARAM_NAMES = {"Fisher", "GC"};
    
    public SiteDataTable(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
    }
    
    @Override
    public int getMargin() {
        return 12;
    }

    @Override
    public String getName() {
        return "sitetable";
    }

    @Override
    public String getNiceName() {
        return "Site data";
    }

    private TextLayout layoutFisherParams(Graphics2D g, String name,
            FisherParams fp) {
        final String s = String.format("%s: Dec=%.1f  Inc=%.1f  "
                + "%s95=%.1f  k=%.1f", name,
                fp.getMeanDirection().getDecDeg(),
                fp.getMeanDirection().getIncDeg(),
                alpha, fp.getA95(), fp.getK());
        final AttributedString as = new AttributedString(s);
        as.addAttributes(getTextAttributes(), 0, s.length());
        if (USE_REAL_ALPHA) {
            // A nasty little hack to deal with the alpha: if printing in
            // a font that doesn't have an alpha, the whole string will
            // fall back to the default font. So we add a dummy attribute
            // to the alpha character to make sure it's in a different
            // style run; this means that a font substitution on the alpha
            // won't affect the whole string. (See bug #32.)
            int position = s.indexOf(alpha);
            as.addAttribute(TextAttribute.SIZE, getFontSize()+0.00001f,
                    position, position+1);
        }
        final FontRenderContext frc = g.getFontRenderContext();
        return new TextLayout(as.getIterator(), frc);
    }
    
    @Override
    public void draw(Graphics2D g) {
        final Sample sample = params.getSample();
        if (sample == null) return;
        final Site site = sample.getSite();
        if (site == null) return;
        final float xOrig = (float) getDimensions().getMinX();
        final float yOrig = (float) getDimensions().getMinY();
        final FisherParams[] fisherParams =
                {site.getFisher(), site.getGreatCircles()};
        float yPos = 0;
        for (int i=0; i<fisherParams.length; i++) {
            FisherParams fp = fisherParams[i];
            if (fp != null) {
                yPos += getFontSize() * 1.2;
                layoutFisherParams(g, PARAM_NAMES[i], fp).
                        draw(g, xOrig, yOrig + yPos);
            }
        }
    }
}
