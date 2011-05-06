package net.talvi.puffinplot.data;

import Jama.Matrix;
import java.util.ArrayList;
import java.util.List;

public class Tensor {

    private double k11, k22, k33, k12, k23, k13;
    private List<Vec3> amsAxes;

    public Tensor(double k11, double k22, double k33,
            double k12, double k23, double k13) {
        this.k11 = k11;
        this.k22 = k22;
        this.k33 = k33;
        this.k12 = k12;
        this.k23 = k23;
        this.k13 = k13;
        double[] elts = {k11, k12, k13, k12, k22, k23, k13, k23, k33};
        Matrix ams = new Matrix(elts, 3);
        Eigens amsEigens = new Eigens(ams);
        // For the present, we just keep the directions
        amsAxes = amsEigens.vectors;
    }

    public Tensor(double k11, double k22, double k33,
            double k12, double k23, double k13, Matrix correct1, Matrix correct2) {
        double[] elts = {k11, k12, k13, k12, k22, k23, k13, k23, k33};
        // correct2 = new Matrix(Vec3.getFormationCorrectionMatrix(0, Math.PI/2.));
        Matrix ams1 = new Matrix(elts, 3);
        Matrix ams2 = correct1.times(ams1).times(correct1.transpose());
        Matrix ams3 = correct2.times(ams2).times(correct2.transpose());
        //ams3 = ams2;
        Eigens amsEigens = new Eigens(ams3);
        double[][] k = ams3.getArray();
        this.k11 = k[0][0];
        this.k12 = k[0][1];
        this.k13 = k[0][2];
        this.k22 = k[1][1];
        this.k23 = k[1][2];
        this.k33 = k[2][2];
        amsAxes = amsEigens.vectors;
    }

    public Tensor(List<Vec3> axes) {
        Vec3 v1 = axes.get(0);
        Vec3 v2 = axes.get(1);
        Vec3 v3 = axes.get(2);
        k11 = v1.x;
        k12 = v1.y;
        k13 = v1.z;
        k22 = v2.y;
        k23 = v2.z;
        k33 = v3.z;
        amsAxes = axes;
    }

    public String toTensorComponentString() {
        String fmt = "%.5f %.5f %.5f %.5f %.5f %.5f";
        return String.format(fmt, k11, k22, k33, k12, k23, k13);
    }

    public static Tensor fromDirections(Vec3 k1, Vec3 k2, Vec3 k3) {
        List<Vec3> axes = new ArrayList<Vec3>(3);
        axes.add(k1);
        axes.add(k2);
        axes.add(k3);
        return new Tensor(axes);
    }
    
    public static Tensor fromDirections(double i1, double d1, double i2,
            double d2, double i3, double d3) {
        Vec3 v1 = Vec3.fromPolarDegrees(1, i1, d1);
        Vec3 v2 = Vec3.fromPolarDegrees(1, i2, d2);
        Vec3 v3 = Vec3.fromPolarDegrees(1, i3, d3);
        return Tensor.fromDirections(v1, v2, v3);
    }

    Vec3 getAxis(int axis) {
        return amsAxes.get(axis);
    }
}
