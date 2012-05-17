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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.talvi.puffinplot.data.file.FileFormat;

/**
 * <p>This enum represents a field in the {@link Datum} class which is associated
 * with a measurement or other value. It is used to address data values
 * from {@link Datum} in a uniform way, for example when loading or saving
 * data.</p>
 * 
 * <p>Each field has a <i>heading</i>, a string representation intended
 * for use when reading or writing data from to to a file, and 
 * a <i>nice name</i>, a string representation intended to be displayed
 * to the user. In practice these are currently identical for all
 * existing fields, but this should not be relied upon.</p>
 * 
 * <p>Fields are classed as either ‘real’ (corresponding to an actual
 * stored item of data) or ‘virtual’ (corresponding to data which is
 * not explicitly stored but can be calculated on-the-fly from other data).
 * The distinction is useful when storing data to a file, since virtual fields
 * do not need to be stored.</p>
 * 
 * @see Datum
 * 
 * @author pont
 */
public enum DatumField {
    /* When adding fields here, make sure also to add them to Datum.getValue()
     * and Datum.setValue(v).
     */

    // Identifiers
    /** the identifier (name) of a discrete sample */
    DISCRETE_ID("Sample ID"),
    /** the depth in the core of a continuous measurement */
    DEPTH("Depth"),
    /** the number of the machine run during which the measurements were made */
    RUN_NUMBER("Run #"),
    /** the timestamp of the measurement */
    TIMESTAMP("Sample Timestamp"),
    /** for discrete samples, the position of the sample on the measurement tray */
    SLOT_NUMBER("Tray slot number"),

    // Lab measurements
    /** the type of the measurement (discrete or continuous) */
    MEAS_TYPE("Measurement type", "Measurement type", "CONTINUOUS", true, false),
    /** the x component of the magnetic moment measurement */
    X_MOMENT("X moment"),
    /** the y component of the magnetic moment measurement */
    Y_MOMENT("Y moment"),
    /** the z component of the magnetic moment measurement */
    Z_MOMENT("Z moment"),
    /** the measured magnetic susceptibility */
    MAG_SUS("Magnetic susceptibility"),
    /** the volume of a discrete sample */
    VOLUME("Volume"),
    /** the cross-sectional area of a continuous core */
    AREA("Area"),

    // Field measurements
    /** the sample dip azimuth in degrees */
    SAMPLE_AZ("Sample azimuth"),
    /** the sample dip angle in degrees */
    SAMPLE_DIP("Sample dip"),
    /** the formation dip azimuth in degrees */
    FORM_AZ("Formation dip azimuth"),
    /** the formation dip angle in degrees */
    FORM_DIP("Formation dip"),
    /** the local geomagnetic field declination at the sampling site */
    MAG_DEV("Magnetic deviation"),

    // Treatments
    /** the type of treatment applied before measurement (thermal, AF, etc.)*/
    TREATMENT("Treatment type", "Treatment type", "DEGAUSS_XYZ", true, false),
    /** for treatments involving AF, the AF x-axis field strength in Tesla */
    AF_X("AF X field"),
    /** for treatments involving AF, the AF y-axis field strength in Tesla */
    AF_Y("AF Y field"),
    /** for treatments involving AF, the AF z-axis field strength in Tesla */
    AF_Z("AF Z field"),
    /** for thermal treatment, the temperature in degrees Celsius */
    TEMPERATURE("Temperature"),
    /** for IRM treatment, the field strength in Tesla */
    IRM_FIELD("IRM Gauss"),
    /** for ARM treatment, the biasing field strength in Tesla */
    ARM_FIELD("ARM Gauss"),
    /** for ARM treatment, the axis along which the biasing field was applied */
    ARM_AXIS("ARM axis", "ARM axis", "AXIAL", true, false),

    // Processing and display parameters
    /** the selection state of the datum */
    PP_SELECTED("PUFFIN selected", "Selected", "false", false, false),
    /** whether PCA fits are to be anchored for this datum */
    PP_ANCHOR_PCA("PUFFIN anchor PCA", "PCA anchored", "false", false, false),
    /** whether this datum should be hidden on plots */
    PP_HIDDEN("PUFFIN hidden", "Hidden", "false", false, false),
    /** whether this datum is used for a great-circle fit */
    PP_ONCIRCLE("PUFFIN on circle", "Use for great circle", "false", false, false),
    /** whether this datum is used for a PCA fit */
    PP_INPCA("PUFFIN in PCA", "Use for PCA", "false", false, false),

    // Virtual parameters
    // These are not explicitly stored, but are calculated when required
    /** the intensity of the magnetic dipole moment per unit volume (‘magnetization’) */
    VIRT_MAGNETIZATION("Magnetization", "Magnetization", "0", true, true),
    /** declination of magnetization vector (degrees) */
    VIRT_DECLINATION("Declination", "Declination", "0", true, true),
    /** inclination of magnetization vector (degrees) */
    VIRT_INCLINATION("Inclination", "Inclination", "0", true, true),
    /** the temperature at which the magnetic susceptibility increases sharply */
    VIRT_MSJUMP("MS jump temp.", "MS jump temp.", "0", false, true),
    /** the hade of a discrete sample (degrees) */
    VIRT_SAMPLE_HADE("Sample hade", "Sample hade", "0", true, true),
    /** the strike of the formation orientation (degrees) */
    VIRT_FORM_STRIKE("Formation strike", "Formation strike", "0", true, true);
    
    
    private final String heading;
    private final String niceName;
    private final String defaultValue;
    private final boolean virtual;
    private final boolean importable;
    private final static Map<String, DatumField> nameMap
            = new HashMap<String, DatumField>();
    private static final List<String> realFieldHeadings;
    private static final List<DatumField> realFields;
    
    static {
        final List<DatumField> realFieldsTmp =
                new ArrayList<DatumField>(values().length - 1);
        final List<String> realFieldHeadersTmp =
                new ArrayList<String>(values().length-1);
        for (DatumField field: values())
        {
            nameMap.put(field.getHeading(), field);
            if (!field.isVirtual()) {
                realFieldsTmp.add(field);
                realFieldHeadersTmp.add(field.toString());
            }
        }
        realFields = Collections.unmodifiableList(realFieldsTmp);
        realFieldHeadings = Collections.unmodifiableList(realFieldHeadersTmp);
    }

    private DatumField(String heading, String niceName, String defaultValue,
            boolean importable, boolean virtual) {
        this.importable = importable;
        this.heading = heading;
        this.niceName = niceName;
        this.virtual = virtual;
        this.defaultValue = defaultValue;
    }

    private DatumField(String heading) {
        this(heading, heading, "0", true, false);
    }

    /**
     * Returns the field whose heading string is the specified string,
     * or {@code null} if no such field exists.
     * 
     * @param heading a heading string for a field
     * @return the field whose heading string is the specified string,
     * or {@code null} if no such field exists
     */
    public static DatumField getByHeading(String heading) {
        return nameMap.get(heading);
    }

    /** Returns the heading string for this field.
     * @return the heading string for this field
     */
    public String getHeading() {
        return heading;
    }
    
    /** Returns this field's ‘nice name’ (string representation for display to user)
     * @return this field's ‘nice name’ (string representation for display to user)
     */
    public String getNiceName() {
        return niceName;
    }
    
    /**
     * Currently only used in {@link Datum#setValue(DatumField, String)}.
     * 
     * @return a string representation of the default value for the field
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /** Reports whether this field is virtual. Virtual fields have no corresponding
     * explicitly stored data value. Their values are calculated on-the-fly
     * from other fields.
     * @return {@code true} if this field is virtual
     */
    public boolean isVirtual() {
        return virtual;
    }
    
    /** Reports whether this field is importable. Importable fields are those
     * which may be specified for custom data import. Note that (perhaps
     * counterintuitively) the virtual fields for declination, inclination, 
     * and magnetization <em>are</em> importable, although they cannot
     * be set by {@link Datum#setValue(DatumField, String)}: they are
     * handled as a special case by {@link FileFormat#readLine(String)}.
     * @return {@code true} if this field is importable
     */
    public boolean isImportable() {
        return importable;
    }

    /** Returns an unmodifiable list of the real fields.
     * A real field corresponds to an explicitly stored data value.
     * @return an unmodifiable list of the real fields */
    public static List<DatumField> getRealFields() {
        return realFields;
    }

    /** Returns an unmodifiable list of the headings of the real fields.
     * A real field corresponds to an explicitly stored data value.
     * @return an unmodifiable list of the headings of the real fields */
    public static List<String> getRealFieldHeadings() {
        return realFieldHeadings;
    }
}
