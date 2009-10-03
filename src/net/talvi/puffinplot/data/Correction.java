package net.talvi.puffinplot.data;

import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.logging.Logger;

public class Correction {

    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");
    final private boolean tray;
    final private boolean empty;
    final private Rotation rotation;
    public final static Correction NONE =
            new Correction(false, false, Rotation.NONE);

    public Correction(boolean tray, boolean empty, Rotation rotation) {
        this.tray = tray;
        this.empty = empty;
        this.rotation = rotation;
    }

    public static enum Rotation {
        NONE("None"), SAMPLE("Sample"), FORMATION("Formn.");
        private final String niceName;
        private Rotation(String niceName) {
            this.niceName = niceName;
        }
    }
    
    public String getDescription() {
        return rotation.niceName +
                (empty ? " E" : "") +
                (tray ? " T" : "");
    }

    public boolean includesSample() {
        return (rotation == Rotation.SAMPLE || rotation == Rotation.FORMATION);
    }

    public boolean includesFormation() {
        return (rotation == Rotation.FORMATION);
    }

    public boolean includesTray() {
        return tray;
    }

    public boolean includesEmpty() {
        return empty;
    }

    @Override
    public String toString() {
        return String.format("%b %b %s", tray, empty, rotation.name());
    }

    public static Correction fromString(String string) {
        Scanner s = new Scanner(string);
        Correction c = null;
        try {
            c = new Correction(s.nextBoolean(), s.nextBoolean(),
                                  Rotation.valueOf(s.next()));
        } catch (InputMismatchException e) {
            c = NONE;
            logger.info("Malformed correction string in preferences: defaulting to no correction.");
        }
        return c;
    }
}
