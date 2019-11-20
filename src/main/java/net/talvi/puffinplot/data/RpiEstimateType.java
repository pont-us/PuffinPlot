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

/**
 *  The parameter used in normalizing an NRM to produce an RPI estimate.
 * 
 * @author pont
 */
public enum RpiEstimateType {
    
    /**
     * Magnetic susceptibility
     */
    MAG_SUS("Magnetic susceptibility"),
    
    /**
     * ARM demagnetization
     */
    ARM_DEMAG("ARM demagnetization"),
    
    /**
     * IRM demagnetization
     */
    IRM_DEMAG("IRM demagnetization"),
    
    /**
     * ARM magnetization
     */
    ARM_MAG("ARM magnetization");
    
    private final String name;
    
    private RpiEstimateType(String name) {
        this.name = name;
    }
    
    /**
     * @return a user-friendly name for this estimate type
     */
    public String getNiceName() {
        return name;
    }
}
