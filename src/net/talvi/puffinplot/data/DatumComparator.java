package net.talvi.puffinplot.data;

import java.io.Serializable;
import java.util.Comparator;

public class DatumComparator implements Comparator<Datum>, Serializable {

	private final MeasurementAxis axis;
	private final Correction corr;
    private boolean emptyCorrection;
	
	public DatumComparator(MeasurementAxis axis, Correction corr, boolean emptyCorrection) {
		this.axis = axis;
		this.corr = corr;
        this.emptyCorrection = emptyCorrection;
	}
	
	public int compare(Datum d1, Datum d2) {
		return Double.compare(d1.getPoint(corr, emptyCorrection).getComponent(axis),
				d2.getPoint(corr, emptyCorrection).getComponent(axis));
				//getValue(d1), getValue(d2));
	}
}
