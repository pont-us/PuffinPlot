package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.TreatType;
import static java.lang.String.format;

public class DataTable extends Plot {

    private final double us = getUnitSize();
    private final List<Double> xSpacing =
            Arrays.asList(420*us, 420*us, 420*us, 550*us, 480*us);
    private final int ySpacing = (int) (120 * getUnitSize());
    private final List<String> headers = 
            Arrays.asList(new String[] {"demag.", "dec.", "inc.", "int.", "m.s."});
    
    public DataTable(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
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
        if (data.isEmpty()) return;

        // final FontRenderContext frc = g.getFontRenderContext();
        List<String> headers2 = new ArrayList<String>(headers);
        if (sample.getDatum(0).getTreatType() == TreatType.THERMAL)
            headers2.set(0, "temp.");
        points.add(new TextLinePoint(this, g, 10, null, headers2, xSpacing));
        final boolean useSequence = (Datum.maximumDemag(data) == 0);
        int sequence = 1;
        float yPos = 2 * ySpacing;
        for (Datum d: data) {
            if (yPos > getDimensions().getHeight()) break;
            final List<String> values = new ArrayList<String>(4);
            final Vec3 p = d.getMoment(params.getCorrection());
            String demag = useSequence ? Integer.toString(sequence)
                    : format("%.0f", d.getDemagLevel());
            if (d.getTreatType().getUnit().equals("T")) {
                // turn T into mT
                // TODO: right-justify (non-trivial in non-fixed font)
                demag = format("%-4.0f", d.getDemagLevel() * 1000);
            }
            values.add(demag);
            values.add(format("%.1f", p.getDecDeg()));
            values.add(format("% .1f", p.getIncDeg()));
            // Don't use .1g, it tickles a bug in Java (#6469160) which
            // throws an ArrayFormatException (at least in Sun Java 5 & 6)
            values.add(format("%.2e", p.mag()));
            values.add(format("%.1e", d.getMagSus()));
            points.add(new TextLinePoint(this, g, yPos, d, values, xSpacing));
            yPos += ySpacing;
            sequence++;
        }
        g.setColor(Color.BLACK);
        drawPoints(g);
    }
}
