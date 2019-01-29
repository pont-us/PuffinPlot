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
public class TreatmentStepTreatmentComparator implements Comparator<TreatmentStep> {

    /**
     *
     * @param d0
     * @param d1
     * @return
     */
    @Override
    public int compare(TreatmentStep d0, TreatmentStep d1) {
        if (d0.getTreatmentType() != d1.getTreatmentType()) {
            return 0;
        }
        return Double.compare(d0.getTreatmentLevel(), d1.getTreatmentLevel());
    }
    
}
