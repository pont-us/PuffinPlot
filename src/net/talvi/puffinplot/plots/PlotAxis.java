package net.talvi.puffinplot.plots;

import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;

import static java.awt.font.TextAttribute.SUPERSCRIPT;
import static java.awt.font.TextAttribute.SUPERSCRIPT_SUPER;

class PlotAxis {
    private Plot plot;

    static enum Direction {
        RIGHT("R","E"), DOWN("D","S"),
        LEFT("L","W"), UP("U","N");
        
        private String letter;
        private String compassDir;
        
        private Direction(String letter, String compassDir) {
            this.letter = letter;
            this.compassDir = compassDir;
        }
        
        boolean isHorizontal() {
            return this==LEFT || this==RIGHT;
        }
        
        Direction labelPos() {
            return this.isHorizontal() ? DOWN : LEFT;
        }
        
        Direction rotAcw90() {
            return this==RIGHT ? UP : this==UP ? LEFT : this==LEFT ? DOWN : RIGHT;
        }
        
        double labelRot() {
            return this.isHorizontal() ? 0 : -Math.PI/2;
        }

        public String getCompassDir() {
            return compassDir;
        }

        public String getLetter() {
            return letter;
        }
    }

    private final int numSteps;
    private final double stepSize;
    private final double extent;
    private final PlotAxis.Direction direction;
    private final String label;
    private final int normalizationFactor;
    private final String endLabel;
    
    PlotAxis(double extent, PlotAxis.Direction direction, double stepSize,
            int numSteps, String label, String endLabel, Plot plot) {
        super();
        this.extent = extent;
        this.numSteps = numSteps;
        this.stepSize = stepSize;
        this.direction = direction;
        this.label = label;
        this.endLabel = endLabel;
        this.plot = plot;
        int nf = 0;
        while (getLength() * Math.pow(10, nf) > 1000) nf -= 3;
        while (getLength() * Math.pow(10, nf) < 1) nf += 1;
        normalizationFactor = nf;
    }
    
    PlotAxis(double extent, Direction direction, double stepSize, String label,
            String endLabel, Plot plot) {
        this(extent, direction, stepSize, (int) (Math.ceil(extent/stepSize)),
                label, endLabel, plot);
    }

    public static double saneStepSize(double extent) {
        // if (extent==0) extent=1;
        double scaleFactor = Math.pow(10, 1-Math.floor(Math.log10(extent)));
        double extentScaledTo100 = extent * scaleFactor;
        double scaledStepSize = calculateStepSize(Math.floor(extentScaledTo100));
        return scaledStepSize / scaleFactor;
    }

    private static double calculateStepSize(double maxValue) {
        return maxValue < 12 ? 2 :
               maxValue < 40 ? 5 :
               maxValue <= 60 ? 10 :
                               20 ;
    }

    private void putText(Graphics2D g, String textString, double x,
            double y, Direction dir, double θ, double padding) {
        AttributedString text = new AttributedString(textString);
        putText(g, text, textString.length(), x, y, dir, θ, padding);
    }

    private void putText(Graphics2D g, AttributedString text, int length, double x,
            double y, Direction dir, double θ, double padding) {
        text.addAttributes(plot.getTextAttributes(), 0, length);
        FontRenderContext frc = g.getFontRenderContext();
        TextLayout layout = new TextLayout(text.getIterator(), frc);
        Rectangle2D bounds = AffineTransform.getRotateInstance(θ).
                createTransformedShape(layout.getBounds()).getBounds2D();
        double w2 = bounds.getWidth()/2;
        double h2 = bounds.getHeight()/2;
        x -= w2;
        y += h2;
        w2 += padding;
        h2 += padding;
        switch (dir) {
        case RIGHT: x += w2; break;
        case DOWN: y += h2; break;
        case LEFT: x -= w2; break;
        case UP: y -= h2; break;
        }

        // Shape bbMoved = AffineTransform.getTranslateInstance(x, y).createTransformedShape(bounds);
        AffineTransform old = g.getTransform();
        g.translate(x - bounds.getMinX(), y - bounds.getMaxY());
        g.rotate(θ);
        layout.draw(g, 0,0);
        g.setTransform(old);
    }
    
    public void draw(Graphics2D g, double scale, int xOrig, int yOrig) {
        int x = 0, y = 0;
        double t = plot.getTickLength() / 2.0f;
        switch (direction) {
        case RIGHT: x = 1; break;
        case DOWN: y = 1; break;
        case LEFT: x = -1; break;
        case UP: y = -1; break;
        }

        for (int i=1; i<=numSteps; i++) {
            double pos = i*getStepSize()*scale;
            g.draw(new Line2D.Double(xOrig+x*pos+y*t, yOrig+y*pos+x*t,
                    xOrig+x*pos-y*t, yOrig+y*pos-x*t));
        }
        
        double xLen = x*getLength()*scale;
        double yLen = y*getLength()*scale;
        g.draw(new Line2D.Double(xOrig, yOrig, xOrig+xLen, yOrig+yLen));
        if (getLength()!=0) putText(g,
                String.format("%3.1f", getNormalizedLength()),
                xOrig+xLen, yOrig+yLen, direction.labelPos(), 0, 5);
        if (label != null) {
            String text = new String(label);
            if (normalizationFactor != 0) {
                text += " \u00D710"; // 00D7 is the multiplication sign
                String exp = Integer.toString(-normalizationFactor);
                text += exp;
                AttributedString as = new AttributedString(text);
                as.addAttribute(SUPERSCRIPT, SUPERSCRIPT_SUPER,
                        text.length()-exp.length(), text.length());
                putText(g, as, text.length(), xOrig+xLen/2, yOrig+yLen/2,
                        direction.labelPos(), direction.labelRot(), 15);
            } else {
                putText(g, text, xOrig+xLen/2,
                        yOrig+yLen/2, direction.labelPos(),
                        direction.labelRot(), 15);
            }
        }
        
        if (endLabel != null) {
            putText(g, endLabel, xOrig+xLen, yOrig+yLen,
                    direction, 0, 8);
        }
    }

    public String getEndLabel() {
        return endLabel;
    }

    public PlotAxis.Direction getDirection() {
        return direction;
    }
    
    public double getStepSize() {
        return stepSize;
    }
        
    double getLength() {
        return stepSize * numSteps;
    }
    
    double getNormalizedLength() {
        return getLength() * Math.pow(10, normalizationFactor);
    }
}