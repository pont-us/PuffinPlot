/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2018 Pontus Lurcock.
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

import Jama.Matrix;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import net.talvi.puffinplot.TestUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import static net.talvi.puffinplot.TestUtils.randomVector;
import static net.talvi.puffinplot.TestUtils.equalOrOpposite;

/**
 *
 * @author pont
 */
public class TensorTest {
    
    @Test
    public void testConstructor() {
        final Random rnd = new Random(77);
        for (int test=0; test<10; test++) {
            
            final double[] ks = {randomK(rnd), randomK(rnd),
                randomK(rnd), randomK(rnd), randomK(rnd), randomK(rnd)};
            final Matrix identity = Matrix.identity(3, 3);
            final Tensor tensor = new Tensor(ks[0], ks[1], ks[2], ks[3], ks[4], ks[5],
                    identity, identity);
            final Eigens eigens = new Eigens(new Matrix(new double[][] {
                {ks[0], ks[3], ks[5]},
                {ks[3], ks[1], ks[4]},
                {ks[5], ks[4], ks[2]}}));
            
            for (int axisIndex=0; axisIndex<3; axisIndex++) {
                final Vec3 actual = tensor.getAxis(axisIndex);
                final Vec3 expected = eigens.getVectors().get(axisIndex);
                assertTrue(expected.equals(actual, 1e-6));
            }
        }
    }
    
    @Test
    public void testAxisCorrections() {
        final Random rnd = new Random(82);
        for (int test=0; test<10; test++) {
            final double[] ks = {randomK(rnd), randomK(rnd),
                randomK(rnd), randomK(rnd), randomK(rnd), randomK(rnd)};
            final double sampAz = rnd.nextDouble()*2*Math.PI;
            final double sampDip = rnd.nextDouble()*2*Math.PI-Math.PI;
            final Matrix scm = new Matrix(Vec3.getSampleCorrectionMatrix(sampAz, sampDip));
            final double formAz = rnd.nextDouble()*2*Math.PI;
            final double formDip = rnd.nextDouble()*2*Math.PI-Math.PI;
            final Matrix fcm = new Matrix(Vec3.getFormationCorrectionMatrix(formAz, formDip));
            final Tensor correctedTensor = new Tensor(ks[0], ks[1], ks[2], ks[3], ks[4], ks[5],
                    scm, fcm);
            final Matrix identity = Matrix.identity(3, 3);
            final Tensor uncorrectedTensor = new Tensor(ks[0], ks[1], ks[2], ks[3], ks[4], ks[5],
                    identity, identity);
            for (int axis=0; axis<3; axis++) {
                final Vec3 correctedAxis = uncorrectedTensor.getAxis(0).
                        correctSample(sampAz, sampDip).correctForm(formAz, formDip);
                final Vec3 axisFromCorrectedTensor = correctedTensor.getAxis(0);
                assertTrue(equalOrOpposite(correctedAxis, axisFromCorrectedTensor, 1e-6));
            }
        }
    }
    
    private static double randomK(Random rnd) {
        return rnd.nextDouble() * 2 - 1;
    }
    
    @Test
    public void testToTensorComponentString() {
        final Random rnd = new Random(123);
        for (int test=0; test<10; test++) {
            final double[] ks = {randomK(rnd), randomK(rnd),
                randomK(rnd), randomK(rnd), randomK(rnd), randomK(rnd)};
            final Matrix identity = Matrix.identity(3, 3);
            final Tensor tensor = new Tensor(ks[0], ks[1], ks[2], ks[3], ks[4], ks[5],
                    identity, identity);
            final String string = tensor.toTensorComponentString();
            final List<Double> actual =
                    Arrays.asList(string.split(" ")).stream().map(Double::parseDouble).collect(Collectors.toList());
            for (int i=0; i<6; i++) {
                assertEquals(ks[i], actual.get(i), 1e-5);
            }
        }    
    }

    @Test
    public void testFromDirectionsAndGetAxis() {
        final Random rnd = new Random(42);
        for (int test=0; test<10; test++) {
            final List<Vec3> vecs =
                    Arrays.asList(new Vec3[] {randomVector(rnd, 100),
                        randomVector(rnd, 100), randomVector(rnd, 100)});
            final Tensor tensor = Tensor.fromDirections(vecs.get(0),
                    vecs.get(1), vecs.get(2));
            for (int i=0; i<3; i++) {
                assertTrue(vecs.get(i).equals(tensor.getAxis(i), 1e-6));
            }
        }
    }

    @Test
    public void testToStrings() {
        final Random rnd = new Random(57);
        for (int test=0; test<10; test++) {
            final List<Vec3> vecs =
                    Arrays.asList(new Vec3[] {randomVector(rnd, 100),
                        randomVector(rnd, 100), randomVector(rnd, 100)});
            final Tensor tensor = Tensor.fromDirections(vecs.get(0),
                    vecs.get(1), vecs.get(2));
            final List<String> strings = tensor.toStrings();
            for (int i=0; i<3; i++) {
                assertEquals(vecs.get(i).getDecDeg(), Double.parseDouble(strings.get(i*2)), 0.1);
                assertEquals(vecs.get(i).getIncDeg(), Double.parseDouble(strings.get(i*2+1)), 0.1);
            }
        }
    }

    /**
     * Test that result of emptyFields is equal in length to that of
     * getHeaders, and contains only empty strings.
     */
    @Test
    public void testGetEmptyFields() {
        final List<String> emptyFields = Tensor.getEmptyFields();
        final List<String> headers = Tensor.getHeaders();
        assertEquals(headers.size(), emptyFields.size());
        assertTrue(emptyFields.stream().allMatch(field -> "".equals(field)));
    }

    /**
     * Test that getHeaders returns the expected headers, and that
     * they are all composed of printable ASCII characters.
     */
    @Test
    public void testGetHeaders() {
        final String expected =
                "AMS dec1,AMS inc1,AMS dec2,AMS inc2,AMS dec3,AMS inc3";
        final String headers = String.join(",", Tensor.getHeaders());
        assertEquals(expected, headers);
        assertTrue(TestUtils.isPrintableAscii(headers));
    }
    
}
