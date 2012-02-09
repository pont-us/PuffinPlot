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
package net.talvi.puffinplot.window;

import java.awt.RenderingHints;

final class PuffinRenderingHints extends RenderingHints {

    private static final PuffinRenderingHints instance =
            new PuffinRenderingHints();

    public static PuffinRenderingHints getInstance() {
        return instance;
    }
    
    private PuffinRenderingHints() {
        super(null);
        put(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY);
        put(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        put(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_DEFAULT);
        put(KEY_DITHERING, VALUE_DITHER_DEFAULT);
        put(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_DEFAULT);
        put(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
        put(KEY_RENDERING, VALUE_RENDER_QUALITY);
        put(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);
        put(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
    }
}
