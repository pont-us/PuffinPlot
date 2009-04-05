package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.data.Sample;

public class DataTable extends Plot {

    private final int xSpacing = (int) (600 * getUnitSize());
    private final int ySpacing = (int) (120 * getUnitSize());
    private final List<String> headers = 
            Arrays.asList(new String[] {"demag.", "dec.", "inc.", "int."});
    
    public DataTable(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
    }

    private void writeLine(Graphics2D g, float yPos, boolean selected, List<String> values) {

        if (selected) writeString(g, "‣", (float) getDimensions().getMinX(),
                (float) (getDimensions().getMinY() + yPos));
        float xPos = 10;
        for (String s: values) {
            writeString(g, s, (float) (getDimensions().getMinX() + xPos),
                    (float) (getDimensions().getMinY() + yPos));
            xPos += xSpacing;
        }
    }

    @Override
    public String getName() {
        return "datatable";
    }

    @Override
    public String getNiceName() {
        return "Data table";
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(Color.BLACK);
        Sample sample = params.getSample();
        if (sample==null) return;
        List<Datum> data = sample.getData();
        if (data.size() == 0) return;

        writeLine(g, ySpacing, false, headers);
        float yPos = 2 * ySpacing;
        for (Datum d: data) {
            List<String> values = new ArrayList<String>(4);
            Vec3 p = d.getPoint(params.getCorrection());
            values.add(String.format("%.0f", d.getDemagLevel()));
            values.add(String.format("%.1f", p.decDegrees()));
            values.add(String.format("%.1f", p.incDegrees()));
            // Don't use .1g, it tickles a bug in Java (#6469160) which
            // throws an ArrayFormatException (at least in Sun Java 5 & 6)
            values.add(String.format("%.3g", p.mag()));
            writeLine(g, yPos,d.isSelected(), values);
            yPos += ySpacing;
        }
    }
}
