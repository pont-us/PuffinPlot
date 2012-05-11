/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
 *
 * PuffinPlot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PuffinPlot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.plots;

import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.FisherParams;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Site;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

/**
 * This plot shows site directions in textual form.
 * It can show Fisher statistics and/or great-circle statistics
 * provided that the appropriate calculation has been performed. 
 * 
 * @author pont
 */
public class SiteParamsLegend extends Plot {

    /* Using a real alpha character can be tricky: it should be OK if
     * the real alpha is simply absent from the font, since it should
     * just get substituted from the default font (which does contain it).
     * But buggy fonts can contain an incorrect or blank character in the
     * alpha position, so it's safer not to use it. Anyway, if we're doing 
     * it properly we should subscript the 95 too.
     */
    private static final boolean USE_REAL_ALPHA = false;
    private static final String alpha = USE_REAL_ALPHA ? "α" : "a";
    private static final String[] PARAM_NAMES = {"Fisher", "GCs"};
    
    /** Creates a site data table with the supplied parameters.
     * 
     * @param parent the graph display containing the table
     * @param params the parameters of the table
     * @param prefs the preferences containing the table configuration
     */
    public SiteParamsLegend(GraphDisplay parent, PlotParams params, Preferences prefs) {
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
        return "Site parameters";
    }

    private AttributedCharacterIterator layoutFisherParams(Graphics2D g, String name,
            FisherParams fp) {
        final String s = String.format("%s  dec %.1f / inc %.1f / "
                + "%s95 %.1f / k %.1f", name,
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
            as.addAttribute(TextAttribute.SIZE, getFontSize()+.00001f,
                    position, position+1);
        }
        final FontRenderContext frc = g.getFontRenderContext();
        // return new TextLayout(as.getIterator(), frc);
        return as.getIterator();
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
                //layoutFisherParams(g, PARAM_NAMES[i], fp).
                //        draw(g, xOrig, yOrig + yPos);
                g.drawString(layoutFisherParams(g, PARAM_NAMES[i], fp),
                        xOrig, yOrig + yPos);
            }
        }
    }
}
