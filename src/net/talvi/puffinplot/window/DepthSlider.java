package net.talvi.puffinplot.window;

import net.talvi.puffinplot.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.talvi.puffinplot.data.Suite;

public class DepthSlider extends JPanel
        implements MouseListener, MouseMotionListener {

    private int maximum = 10;
    private int value = 1;
    private int rangeStart = -1;
    private int rangeEnd = -1;
    private static final RenderingHints renderingHints =
            PuffinRenderingHints.getInstance();
    private static double POINTER_HALFHEIGHT = 10;
    private List<ChangeListener> changeListeners =
            new LinkedList<ChangeListener>();

    public DepthSlider() {
        super();
        setBackground(Color.LIGHT_GRAY);
        setOpaque(true);
        addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Suite suite = PuffinApp.getInstance().getSuite();
                if (suite != null) {
                    suite.setCurrentSampleIndex(getValue());
                    PuffinApp.getInstance().updateDisplay();
                }
            }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DOWN"), "next");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("UP"), "previous");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control A"), "select_all");
        getActionMap().put("next", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                changeValueBy(1);
            }
        });
        getActionMap().put("previous", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                changeValueBy(-1);
            }
        });
        getActionMap().put("select_all", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                rangeStart = 0;
                rangeEnd = maximum;
                repaint();
            }
        });
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(20,100);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(20,100);
    }

    private void drawPointer(Graphics2D g, double y) {
        GeneralPath pointer = new GeneralPath();
        pointer.moveTo(getX(), (float) (y - POINTER_HALFHEIGHT));
        pointer.lineTo(getX(), (float) (y + POINTER_HALFHEIGHT));
        pointer.lineTo(getX()+getWidth()/2, (float) y);
        g.fill(pointer);
        g.draw(new Line2D.Double(getX(), y, getX() + getWidth(), y));
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHints(renderingHints);
        double scale = getScale();
        double yOrig = getY() + getYMargin();
        double y = yOrig + (scale * value);
        if (getRangeStart() > -1) {
            g2.setColor(Color.RED);
            Rectangle2D selection = new Rectangle2D.Double();
            selection.setFrameFromDiagonal(getX(), yOrig + getRangeStart() * scale,
                    getX() + getWidth(), yOrig + getRangeEnd() * scale);
            g2.fill(selection);
        }
        g2.setColor(Color.WHITE);
        if (scale > 4) {
        for (int i=0; i<=maximum; i++) {
            g2.draw(new Line2D.Double(getX(), yOrig+i*scale,
                    getX()+getWidth(), yOrig+i*scale));
        }
        }
        g2.setColor(Color.BLACK);
        drawPointer(g2, y);
    }

    private double getYMargin() {
        return POINTER_HALFHEIGHT;
    }

    private double getScale() {
        return ((double) getHeight() - 2*getYMargin()) / ((double) maximum);
    }

    public int getValue() {
        return value;
    }

    void changeValueBy(int i) {
        value = constrain(value + i);
        repaint();
        notifyChangeListeners();
    }

    void setForSuite(Suite suite) {
        maximum = suite.getNumSamples() - 1;
        value = suite.getCurrentSampleIndex();
        rangeStart = rangeEnd = -1;
    }

    private void addChangeListener(ChangeListener changeListener) {
        changeListeners.add(changeListener);
    }

    private void notifyChangeListeners() {
        for (ChangeListener cl: changeListeners)
            cl.stateChanged(new ChangeEvent(this));
    }

    public void mouseClicked(MouseEvent e) {
        clickOrDrag(e);
    }

    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
    }

    public void mouseReleased(MouseEvent e) {
       // Do nothing.
    }

    public void mouseEntered(MouseEvent e) {
        // Do nothing.
    }

    public void mouseExited(MouseEvent e) {
       // Do nothing.
    }

    public void mouseDragged(MouseEvent e) {
        clickOrDrag(e);
    }

    public void mouseMoved(MouseEvent e) {
        // Do nothing.
    }

    private void clickOrDrag(MouseEvent e) {
        int v = yposToValue(e.getY());
        if (e.isShiftDown()) {
            adjustRange(v);
        } else {
            if (!e.isControlDown()) rangeStart = rangeEnd = -1;
            value = v;
            notifyChangeListeners();
        }
        repaint();
    }

    private void adjustRange(int v) {
        if (getRangeStart() == -1) rangeStart = value;
        if (getRangeEnd() == -1) rangeEnd = value;
        if (v < getRangeStart()) rangeStart = v;
        else if (v < getRangeEnd()) {
            if (v - getRangeStart() < getRangeEnd() - v) rangeStart = v;
            else rangeEnd = v;
        } else if (v > getRangeEnd()) rangeEnd = v;
        value = v;
        notifyChangeListeners();
        repaint();
    }

    private int yposToValue(double yPos) {
        return constrain((int) (((0.5d + yPos - getYMargin()) / getScale())));
    }

    private int constrain(int v) {
        return v < 0 ? 0
                : v > maximum ? maximum
                : v;
    }

    /**
     * @return the rangeStart
     */
    public int getRangeStart() {
        return rangeStart;
    }

    /**
     * @return the rangeEnd
     */
    public int getRangeEnd() {
        return rangeEnd;
    }

}
