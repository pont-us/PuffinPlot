package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.Point;
import net.talvi.puffinplot.data.Sample;

public class DataTable extends Plot {

    private final int xSpacing = 80;
    private final int ySpacing = 20;
    private final List<String> headers = 
            Arrays.asList(new String[] {"demag.", "dec.", "inc.", "int."});
    
    public DataTable(GraphDisplay parent, PlotParams params, Rectangle2D dimensions) {
        super(parent, params, dimensions);
    }
    
    private void writeLine(Graphics2D g, float yPos, List<String> values) {
        float xPos = 0;
        for (String s: values) {
            g.drawString(s, (float) (getDimensions().getMinX() + xPos),
                    (float) (getDimensions().getMinY() + yPos));
            xPos += xSpacing;
        }
    }
        
    @Override
    public void draw(Graphics2D g) {
        g.setColor(Color.BLACK);
        Sample sample = params.getSample();
        if (sample==null) return;
        List<Datum> data = sample.getData();
        if (data.size() == 0) return;

        writeLine(g, ySpacing, headers);
        float yPos = 2 * ySpacing;
        for (Datum d: data) {
            List<String> values = new ArrayList<String>(4);
            Point p = d.getPoint(params.getCorrection());
            values.add(String.format("%.0f", d.getDemagLevel()));
            values.add(String.format("%.1f", p.decDegrees()));
            values.add(String.format("%.1f", p.incDegrees()));
            values.add(String.format("%.1g", p.mag()));
            writeLine(g, yPos, values);
            yPos += ySpacing;
        }
    }
}
