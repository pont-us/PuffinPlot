package net.talvi.puffinplot.data;

import java.io.Serializable;
import java.util.Comparator;

public class DatumComparator implements Comparator<Datum>, Serializable {

	private final MeasurementAxis axis;
	private final Correction corr;
	
	public DatumComparator(MeasurementAxis axis, Correction corr) {
		this.axis = axis;
		this.corr = corr;
	}
	
	public int compare(Datum d1, Datum d2) {
		return Double.compare(d1.getPoint(corr).getComponent(axis),
				d2.getPoint(corr).getComponent(axis));
				//getValue(d1), getValue(d2));
	}
}
