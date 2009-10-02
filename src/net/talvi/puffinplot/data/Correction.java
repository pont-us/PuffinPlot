package net.talvi.puffinplot.data;

public enum Correction {
    NONE("None"), SAMPLE("Sample"), FORMATION("Formn.");

    private final String niceName;

    private Correction(String niceName) {
        this.niceName = niceName;
    }

    public String getNiceName() {
        return niceName;
    }

    public boolean includesSample() {
        return (this == SAMPLE || this == FORMATION);
    }

    public boolean includesFormation() {
        return (this == FORMATION);
    }
}
