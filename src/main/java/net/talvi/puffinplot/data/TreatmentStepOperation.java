package net.talvi.puffinplot.data;

/**
 * Represents an operation which can be performed on a treatment step.
 */
public enum TreatmentStepOperation {
    SELECT("Select"), DESELECT("Deselect"), HIDE("Hide"), UNHIDE("Unhide");

    private final String displayName;

    TreatmentStepOperation(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Return this operation's human-readable name
     *
     * @return this operation's human-readable name
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Apply this operation to a treatment step.
     *
     * @param ts the treatment step to which to apply this operation
     */
    public void apply(TreatmentStep ts) {
        if (ts == null) {
            return;
        }
        switch (this) {
            case SELECT: ts.setSelected(true); break;
            case DESELECT: ts.setSelected(false); break;
            case HIDE: ts.setHidden(true); break;
            case UNHIDE: ts.setHidden(false); break;
        }
    }
}
