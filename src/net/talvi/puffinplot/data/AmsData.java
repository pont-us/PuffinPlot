package net.talvi.puffinplot.data;

public class AmsData {
    private final String name;
    private final double[] tensor;
    private final double sampleAz, sampleDip;

    public AmsData(String name, double[] tensor, double sampleAz, double sampleDip) {
        this.name = name;
        this.tensor = tensor;
        this.sampleAz = sampleAz;
        this.sampleDip = sampleDip;
    }

    public String getName() {
        return name;
    }

    public double[] getTensor() {
        return tensor;
    }

    public double getSampleAz() {
        return sampleAz;
    }

    public double getSampleDip() {
        return sampleDip;
    }
}
