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
