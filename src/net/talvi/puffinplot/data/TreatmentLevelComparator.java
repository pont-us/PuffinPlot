/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
 *
 * PuffinPlot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PuffinPlot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot. If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data;

import java.util.Comparator;

/**
 *
 * @author pont
 */
public class TreatmentLevelComparator
        implements Comparator<TreatmentStep> {

    /**
     * Compare treatment steps according to their treatment level. The
     * comparison result is the same as the result of comparing the treatment
     * level. Note that while any two steps may be compared by this comparison,
     * the results may not make sense physically (e.g. comparing a temperature
     * with an AF field strength).
     *
     * @param step1 the first step to be compared
     * @param step2 the second step to be compared
     * @return a negative integer, zero, or a positive integer as the treatment
     * level of the first step is less than, equal to, or greater than that of
     * the second step.
     */
    @Override
    public int compare(TreatmentStep step1, TreatmentStep step2) {
        return Double.compare(step1.getTreatmentLevel(),
                step2.getTreatmentLevel());
    }
    
}
