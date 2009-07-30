package net.talvi.puffinplot.plots;

import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.data.Sample;
import static java.lang.String.format;

public class DataTable extends Plot {

    private final int xSpacing = (int) (600 * getUnitSize());
    private final int ySpacing = (int) (120 * getUnitSize());
    private final List<String> headers = 
            Arrays.asList(new String[] {"demag.", "dec.", "inc.", "int."});
    
    public DataTable(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
    }

    private void writeLine(Graphics2D g, float yPos, Datum d, List<String> values) {

        final float xMin = (float) getDimensions().getMinX();
        final float yMin = (float) getDimensions().getMinY();

        if (d != null) {
            if (d.isSelected()) writeString(g, "‣", xMin, yMin + yPos);
            if (d.isHidden()) writeString(g, "-", xMin, yMin + yPos);
        }

        float xPos = 10;
        for (String s: values) {
            writeString(g, s, xMin + xPos, yMin + yPos);
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
        clearPoints();

        final Sample sample = params.getSample();
        if (sample==null) return;
        final List<Datum> data = sample.getData();
        if (data.size() == 0) return;

        final FontRenderContext frc = g.getFontRenderContext();
        points.add(new TextLinePoint(this, frc, 0, null, headers, xSpacing));
        final boolean useSequence = (Datum.maximumDemag(data) == 0);
        int sequence = 1;
        float yPos = 2 * ySpacing;
        for (Datum d: data) {
            final List<String> values = new ArrayList<String>(4);
            final Vec3 p = d.getPoint(params.getCorrection(), params.isEmptyCorrectionActive());
            final String demag = useSequence ? Integer.toString(sequence)
                    : format("%.0f", d.getDemagLevel());
            values.add(demag);
            values.add(format("%.1f", p.getDecDeg()));
            values.add(format("%.1f", p.getIncDeg()));
            // Don't use .1g, it tickles a bug in Java (#6469160) which
            // throws an ArrayFormatException (at least in Sun Java 5 & 6)
            values.add(format("%.3g", p.mag()));
            points.add(new TextLinePoint(this, frc, yPos, d, values, xSpacing));
            yPos += ySpacing;
            sequence++;
        }
        drawPoints(g);

    }
}
