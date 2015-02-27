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
import java.util.Map;

/**
 * Rendering hints for the PuffinPlot graph display.
 * 
 * Note that this class, like its parent, is not immutable and cannot
 * practically be made so. However, getInstance() is guaranteed always
 * to return an unmodified instance. This is necessary because
 * SVGGraphics2D.setRenderingHints sometimes clears the RenderingHints
 * object passed to it.
 * 
 * @author pont
 */
public final class PuffinRenderingHints extends RenderingHints {

    private boolean dirty;
    
    private static PuffinRenderingHints instance =
            new PuffinRenderingHints();

    /**
     * Return a clean instance of this class.
     * 
     * The instance may be the same as a previously returned instance,
     * if it has not been modified. If the previously returned instance 
     * has been modified, a new one is created and returned.
     * 
     * @return a clean instance of {@code PuffinRenderingHints}
     */
    public static PuffinRenderingHints getInstance() {
        if (instance.dirty) {
            instance = new PuffinRenderingHints();
        }
        return instance;
    }
    
    private PuffinRenderingHints() {
        super(null);
        dirty = false;
        super.put(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY);
        super.put(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        super.put(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_DEFAULT);
        super.put(KEY_DITHERING, VALUE_DITHER_DEFAULT);
        super.put(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_DEFAULT);
        super.put(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
        super.put(KEY_RENDERING, VALUE_RENDER_QUALITY);
        super.put(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);
        super.put(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(Map<?,?> m) {
        dirty = true;
        super.putAll(m);
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public Object put(Object key, Object value) {
        dirty = true;
        return super.put(key, value);
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public void add(RenderingHints hints) {
        dirty = true;
        super.add(hints);
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        dirty = true;
        super.clear();
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public Object remove(Object key) {
        dirty = true;
        return super.remove(key);
    }
    
    /**
     * Use E notation rather than superscript notation for orders of magnitude.
     * The value is ignored; the key's existence indicates that E notation
     * should be used.
     */
    public static final PuffinRenderingHints.Key KEY_E_NOTATION = new PuffinRenderingHints.Key(0);
    
    /**
     * A key for a PuffinPlot rendering hint.
     */
    public static class Key extends RenderingHints.Key {
        
        /**
         * Construct a key using the indicated private key.
         * 
         * @see java.awt.RenderingHints.Key
         * 
         * @param privateKey the specified key
         */
        public Key(int privateKey) {
            super(privateKey);    
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCompatibleValue(Object val) {
            if (intKey()==0) {
                return true; // value ignored
            }
            return false;
        }
        
    }
}
