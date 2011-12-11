package net.talvi.puffinplot.data;

import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.logging.Logger;

public class Correction {

    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");
    private boolean tray;
    private boolean empty;
    private Rotation rotation;
    public static final Correction NONE =
            new Correction(false, false, Rotation.NONE);

    public Correction(boolean tray, boolean empty, Rotation rotation) {
        this.tray = tray;
        this.empty = empty;
        this.rotation = rotation;
    }

    public void setRotation(Rotation rotation) {
        this.rotation = rotation;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public static enum Rotation {
        NONE("None"), SAMPLE("Sample"), FORMATION("Formn.");
        public final String niceName;
        private Rotation(String niceName) {
            this.niceName = niceName;
        }
    }
    
    public String getDescription() {
        return getRotation().niceName +
                (empty ? " E" : "") +
                (tray ? " T" : "");
    }

    public boolean includesSample() {
        return (getRotation() == Rotation.SAMPLE || getRotation() == Rotation.FORMATION);
    }

    public boolean includesFormation() {
        return (getRotation() == Rotation.FORMATION);
    }

    public boolean includesTray() {
        return tray;
    }

    public boolean includesEmpty() {
        return empty;
    }

    @Override
    public String toString() {
        return String.format("%b %b %s", tray, empty, getRotation().name());
    }

    public static Correction fromString(String string) {
        Scanner s = new Scanner(string);
        Correction c = null;
        try {
            // In practice the two booleans should always be false.
            c = new Correction(s.nextBoolean(), s.nextBoolean(),
                                  Rotation.valueOf(s.next()));
        } catch (InputMismatchException e) {
            c = NONE;
            logger.info("Malformed correction string in preferences: defaulting to no correction.");
        }
        return c;
    }
}
