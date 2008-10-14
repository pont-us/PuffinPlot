package net.talvi.puffinplot.plots;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;

import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Datum;

public abstract class Plot extends JComponent

{

    private boolean breakLine = false;

    
    public Plot() {}
    
    
    public Plot(PlotParams params) {
        super();
        setOpaque(false);
        setMaximumSize(new Dimension(600, 600));
        setPreferredSize(new Dimension(600, 600));
        setVisible(true);
    }
    



    void breakLine() {
        breakLine  = true;
    }
    

    
    void drawPoints(Graphics2D g) {

    }


    

}
