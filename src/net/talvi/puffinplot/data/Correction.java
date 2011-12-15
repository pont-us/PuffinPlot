package net.talvi.puffinplot.data;

import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * <p>This class represents the corrections which may be applied to the measured
 * remanence to estimate the true remanence. The main corrections are the
 * orientation corrections for sample and formation orientations. The class
 * also contains facilities for handling tray corrections (subtracting the
 * tray remanence) and empty-slot corrections (monitoring the measured
 * remanence of an empty measurement slot to correct for instrument drift).
 * At present, these are not actually used by PuffinPlot: the tray correction
 * is applied when the data file is first loaded, and the empty-slot
 * correction is not implemented.</p>
 * 
 * <p>Note that this class does not contain any of the data for actually
 * applying to corrections; it just determines which corrections
 * should be applied.</p>
 * 
 * @author pont
 */
public class Correction {

    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");
    private boolean tray;
    private boolean empty;
    private Rotation rotation;
    public static final Correction NONE =
            new Correction(false, false, Rotation.NONE);

    /**
     * Creates a new set of corrections
     * 
     * @param tray {@code true} to use the tray correction
     * @param empty {@code} true} to use the empty slot correction
     * @param rotation the type of rotation correction to use
     */
    public Correction(boolean tray, boolean empty, Rotation rotation) {
        this.tray = tray;
        this.empty = empty;
        this.rotation = rotation;
    }

    /** Sets the rotation correction. 
     * @param rotation the type of rotation correction to use */
    public void setRotation(Rotation rotation) {
        this.rotation = rotation;
    }

    /** Returns the rotation correction. 
     * @return the type of rotation correction currently in use */
    public Rotation getRotation() {
        return rotation;
    }

    /**
     * An enumeration of the types of rotation correction which may
     * be applied to a sample's data.
     */
    public static enum Rotation {
        /** no rotation applied to data */
        NONE("None"),
        /** data rotated to correct for sample orientation */
        SAMPLE("Sample"),
        /** data rotated to correct for sample and formation orientation */
        FORMATION("Formn.");
        
        private final String niceName;
        
        private Rotation(String niceName) {
            this.niceName = niceName;
        }

        /** Returns a user-friendly name for this rotation correction. 
         * @return a user-friendly name for this rotation correction */
        public String getNiceName() {
            return niceName;
        }
    }
    
    /** Returns a user-friendly string describing this correction. 
     * @returna user-friendly string describing this correction */
    public String getDescription() {
        return getRotation().getNiceName() +
                (empty ? " E" : "") +
                (tray ? " T" : "");
    }

    /** Returns {@code true} if this correction includes a rotation for sample
     * orientation. This is the case if the rotation is {@code SAMPLE}
     * or {@code FORMATION}.
     * @return {@code true} if this correction includes a rotation for sample orientation
     */
    public boolean includesSample() {
        return (getRotation() == Rotation.SAMPLE || getRotation() == Rotation.FORMATION);
    }

    /** Returns {@code true} if this correction includes a rotation for formation
     * orientation. This is the case if the rotation is {@code FORMATION}.
     * @return {@code true} if this correction includes a rotation for formation orientation
     */
    public boolean includesFormation() {
        return (getRotation() == Rotation.FORMATION);
    }

    /** Returns {@code true} if this correction includes a correction for tray remanence.
     * @return {@code true} if this correction includes a correction for tray remanence */
    public boolean includesTray() {
        return tray;
    }

    /** Returns {@code true} if this correction includes an empty-slot correction.
     * @return {@code true} if this correction includes an empty-slot correction */
    public boolean includesEmpty() {
        return empty;
    }

    /**
     * Returns a parseable string representation of this correction. The string
     * is intended for storing and restoring state and is not user-friendly.
     * Use {@link #getDescription()} for a user-friendly description.
     * 
     * @return a string representation of this correction
     * @see #getDescription()
     * @see #fromString(java.lang.String)
     */
    @Override
    public String toString() {
        return String.format("%b %b %s", tray, empty, getRotation().name());
    }

    /**
     * Creates a correction from the supplied string. The string should be
     * in the format produced by {@link #toString()}.
     * 
     * @param string a string representation of the correction to be created
     * @return a correction created according to the provided string
     * @see #toString()
     */
    public static Correction fromString(String string) {
        Scanner s = new Scanner(string);
        Correction c = null;
        try {
            // At present, the two booleans should always be false.
            c = new Correction(s.nextBoolean(), s.nextBoolean(),
                                  Rotation.valueOf(s.next()));
        } catch (InputMismatchException e) {
            c = NONE;
            logger.info("Malformed correction string in preferences: defaulting to no correction.");
        }
        return c;
    }
}
