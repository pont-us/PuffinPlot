package net.talvi.puffinplot.data;

import java.util.HashSet;
import java.util.Set;

/**
 * A container class holding all the data within a suite from a
 * particular line number of their respective files. Intended to be
 * used as a proxy for run number. We can't use the actual run number,
 * because the Long Core software adds the position number to it for
 * discrete samples. We can't correct for that because it can't write
 * the position number to the DAT file.
 */

public class Line {

    private final int lineNumber;
    private final Set<Datum> data;
    // private Datum emptySlot;

    public Line(int lineNumber) {
        this.lineNumber = lineNumber;
        data = new HashSet<Datum>();
    }

    public void add(Datum datum) {
        data.add(datum);
//        if (datum.getMeasType() == MeasType.DISCRETE &&
//                datum.getSampleId().equals("EMPTY")) {
//            emptySlot = datum;
//        }
    }

    public Datum getEmptySlot() {
        for (Datum d: data) {
            if (d.getSample().isEmptySlot()) return d;
        }
        return null;
    }

}
