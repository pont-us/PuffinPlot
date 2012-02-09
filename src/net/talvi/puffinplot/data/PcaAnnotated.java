/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
 *
 * PuffinPlot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PuffinPlot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class encapsulates a set of principal component analysis (PCA)
 * parameters along with some data on the treatment steps from which 
 * the PCA was calculated. At present the treatment step data can only
 * be read via the {@link #toStrings()} method which is intended for
 * export to a file.
 * 
 * @see PcaValues
 * @author pont
 */
public class PcaAnnotated {

    private final PcaValues pcaValues;
    private final double demagStart;
    private final double demagEnd;
    private final boolean contiguous;
    private static final List<String> HEADERS;

    static {
        List<String> hA = new ArrayList<String>();
        hA.addAll(PcaValues.getHeaders());
        hA.addAll(Arrays.asList("PCA start (°C or mT)", "PCA end (°C or mT)",
                "PCA contiguous"));
        HEADERS = Collections.unmodifiableList(hA);
    }

    private PcaAnnotated(PcaValues pcaValues, double demagStart,
            double demagEnd, boolean contiguous) {
        this.pcaValues = pcaValues;
        this.demagStart = demagStart;
        this.demagEnd = demagEnd;
        this.contiguous = contiguous;
    }

    /**
     * Performs principal component analysis (PCA) on the specified sample.
     * Points to use for PCA are determined using the 
     * {@link Datum#isInPca()} method. The starting and ending treatment
     * steps are stored, as is a flag indicating whether the treatment
     * step were contiguous.
     * 
     * @param sample the sample on which to perform PCA
     * @param correction the correction to apply to the magnetic moment data
     * @return results of principal component analysis
     */
    public static PcaAnnotated calculate(Sample sample, Correction correction) {
        List<Datum> rawData = sample.getVisibleData();
        List<Vec3> points = new ArrayList<Vec3>(rawData.size());
        List<Datum> data = new ArrayList<Datum>(rawData.size());
        
        int runEndsSeen = 0;
        boolean thisIsPca = false, lastWasPca = false;
        for (Datum d: rawData) {
            thisIsPca = d.isInPca();
            if (thisIsPca) {
                points.add(d.getMoment(correction));
                data.add(d);
            } else {
                if (lastWasPca) runEndsSeen++;
            }
            lastWasPca = thisIsPca;
        }
        if (thisIsPca) runEndsSeen++;
        boolean contiguous = (runEndsSeen <= 1);
        if (points.size() < 2) return null;
        PcaValues pca = PcaValues.calculate(points, sample.isPcaAnchored());
        
        return new PcaAnnotated(pca,
                data.get(0).getTreatmentLevel(),
                data.get(data.size() - 1).getTreatmentLevel(),
                contiguous);
    }

    /** Returns the results of the principal component analysis.
     * @return the results of the principal component analysis */
    public PcaValues getPcaValues() {
        return pcaValues;
    }

    /** Returns the parameters as a list of strings.
     * The order of the parameters is the same as the order of
     * the headers provided by {@link #getHeaders()}.
     * @return the parameters as a list of strings
     */
    public List<String> toStrings() {
        ArrayList<String> result = new ArrayList<String>();
        result.addAll(pcaValues.toStrings());
        result.add(Double.toString(demagStart));
        result.add(Double.toString(demagEnd));
        result.add(contiguous ? "Y" : "N");
        return Collections.unmodifiableList(result);
    }

    /** Returns the headers describing the parameters as a list of strings.
     * @return the headers describing the parameters */
    public static List<String> getHeaders() {
        return HEADERS;
    }

    /** Returns a list of empty strings equal in length to the number of parameters.
     * @return  a list of empty strings equal in length to the number of parameters
     */
    public static List<String> getEmptyFields() {
        return Collections.nCopies(HEADERS.size(), "");
    }
}
