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

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class EigensTest {

    @Test
    public void testFromVectors() {
        
        /* An unpleasantly large and monolithic test, but it does the job.
         * Test data and expected outputs are precalculated using a Python
         * script.
         * 
         * Format:
         * 
         * {
         * { {input vectors (length-3 arrays)},
         *   {output eigenvectors, eigenvalues, and MADs (non-normalized)},
         *   {output eigenvectors, eigenvalues, and MADs (normalized)}
         * }, ...
         * }
         * 
         * In more detail, each top-level sub-array (type double[][][])
         * has the structure:
         * 
         * { {{vec0x, vec0y, vec0z}, {vec1x, vec1y, vec1z}, ... },
         *   {{evec0x, evec0y, evec0z}, ... , {evec2x, evec2y, evec2z},
         *    {eval0, eval1, eval2}, {mad1, mad3}}, // non-normalized
         *   {{evec0x, evec0y, evec0z}, ... , {evec2x, evec2y, evec2z},
         *    {eval0, eval1, eval2}, {mad1, mad3}} // normalized
         * } 
         * 
         */
        
        final double[][][][] testData = {
            {{{-78,-95,52},{-21,9,-2},{36,-8,-66},{86,-50,-31},{14,-96,-43},},
{{0.796579, 0.162098, -0.582397},{-0.0730627, 0.982133, 0.173425},{-0.600103, 0.0955951, -0.79419},{22736.5, 21010.5, 2426.04},{25.2368,45.4344}},
{{-0.771405, 0.442237, 0.45756},{-0.372831, -0.896802, 0.23821},{0.515686, 0.0131634, 0.856677},{2.9088, 1.43177, 0.659427},{39.6593,40.2944}}},
{{{-93,-72,-78},{82,28,8},{-94,32,61},{88,11,-8},{98,-85,-47},{-90,51,-5},},
{{0.969073, -0.210915, -0.12811},{-0.243949, -0.740455, -0.626271},{-0.0372302, -0.638154, 0.769008},{51310.7,  24652, 2800.24},{22.2975,36.1836}},
{{0.988491, -0.119052, -0.0933431},{-0.15125, -0.790472, -0.593529},{0.00312411, -0.600816, 0.799381},{4.27294, 1.48743, 0.239623},{24.9866,32.4463}}},
{{{59,-44,89},{-7,45,95},{-5,47,-27},{70,-19,47},},
{{-0.475895, 0.201511, -0.856106},{-0.468671, 0.765575, 0.440729},{0.744225, 0.610973, -0.269891},{24761.6, 8233.17, 1875.25},{28.8507,32.5757}},
{{0.478855, -0.382321, 0.790271},{-0.142668, 0.854333, 0.499761},{-0.866224, -0.352059, 0.354557},{2.66621, 0.934775, 0.399017},{37.2089,35.2714}}},
{{{22,-54,84},{24,52,15},{73,65,-58},},
{{-0.346879, -0.675956, 0.650199},{-0.790415, -0.162503, -0.590624},{-0.504895, 0.718802, 0.477917},{ 19550, 5862.98, 1466.03},{29.6883,31.4782}},
{{-0.38716, -0.806428, 0.446969},{0.486178, 0.233346, 0.842129},{-0.783415, 0.543345, 0.301725},{1.94901, 0.832134, 0.218851},{31.492,36.2909}}},
{{{-54,37,-17},{73,34,-49},{50,-70,-78},{-74,10,-90},},
{{0.69811, -0.376356, -0.609097},{0.634402, -0.0692416, 0.769896},{0.33193, 0.923884, -0.190423},{18157.8, 16753.5, 5708.72},{38.987,48.0415}},
{{0.931389, -0.363748, -0.014191},{0.0187917, 0.00911208, 0.999782},{-0.363539, -0.931453, 0.0153223},{2.00712, 1.37692, 0.615969},{40.9735,44.8981}}},
{{{47,-28,52},{-15,-72,-5},{-26,39,-21},{17,41,-44},{-33,-61,-72},},
{{-0.387763, -0.713801, -0.583205},{0.2897, -0.695022, 0.658041},{0.875051, -0.0862093, -0.476292},{ 14234, 11645.3, 1789.75},{27.8613,44.1726}},
{{-0.199325, 0.838131, -0.507746},{-0.50429, -0.531999, -0.680198},{-0.840215, 0.120471, 0.528701},{2.79595, 1.76838, 0.435669},{32.3822,41.6007}}},
{{{82,27,-39},{-15,40,-63},{91,52,-74},},
{{0.694786, 0.407922, -0.592344},{0.718667, -0.361601, 0.593938},{-0.028088, 0.838358, 0.544396},{26617.8, 4609.74, 1.49215},{1.1164,22.598}},
{{0.603764, 0.446118, -0.660642},{0.796704, -0.309553, 0.519076},{-0.0270656, 0.839735, 0.542321},{2.26846, 0.731411, 0.000128928},{0.874716,29.5912}}},
{{{-57,16,-91},{-85,62,7},{1,-94,-58},{-42,76,-96},{-43,90,-85},{38,14,92},},
{{0.474106, -0.492231, 0.730022},{-0.106642, 0.790923, 0.602552},{-0.873986, -0.363525, 0.322488},{53028.4, 21704.5, 4406.13},{28.1414,35.0575}},
{{-0.507164, 0.409743, -0.758218},{-0.169827, 0.814996, 0.554022},{0.844952, 0.409746, -0.343752},{3.7926, 1.85378, 0.353623},{28.0538,37.3403}}},
{{{-11,-95,-3},{26,-50,-90},{-80,-32,20},},
{{-0.0203538, 0.842441, 0.538404},{0.754563, 0.366236, -0.544523},{-0.655912, 0.395177, -0.643129},{15142.4, 11135.2, 1977.41},{29.036,42.9402}},
{{0.299487, 0.918979, 0.256488},{-0.736202, 0.0515859, 0.674793},{0.606889, -0.390918, 0.692003},{1.54189, 1.24266, 0.215447},{29.2295,44.1999}}},
{{{94,18,-24},{-44,-76,-73},{43,36,67},{-56,77,40},},
{{0.310332, 0.696662, 0.646805},{0.950179, -0.206395, -0.233585},{0.0292327, -0.687069, 0.726004},{24185.9, 14870.9, 2019.2},{25.0919,39.8845}},
{{0.376294, 0.658712, 0.651538},{0.926162, -0.24845, -0.283718},{0.0250139, -0.710191, 0.703565},{2.30759, 1.47519, 0.217225},{26.1654,40.5766}}},
        };
        
        for (double[][][] inputsAndOutputs: testData) {
            double[][] inputArray = inputsAndOutputs[0];

            final List<Vec3> inputs = new ArrayList<>(inputArray.length);
            for (double[] v : inputArray) {
                inputs.add(new Vec3(v[0], v[1], v[2]));
            }
            
            for (int normalize=0; normalize<2; normalize++) {
                double[][] expectedOutputs = inputsAndOutputs[1 + normalize];
                final List<Vec3> expectedVectors = new ArrayList<>(3);
                for (int i=0; i<3; i++) {
                    final double[] v = expectedOutputs[i];
                    expectedVectors.add(new Vec3(v[0], v[1], v[2]));
                }
                final Eigens actualOutput =
                        Eigens.fromVectors(inputs, normalize==1);
                for (int i=0; i<3; i++) {
                    final Vec3 expectedVector = expectedVectors.get(i);
                    final Vec3 actualVector = actualOutput.getVectors().get(i);
                    assertTrue(expectedVector.equals(actualVector, 1e-6) ||
                            expectedVector.invert().equals(actualVector, 1e-6));
                    assertEquals(expectedOutputs[3][i],
                            actualOutput.getValues().get(i), 0.1);
                    assertEquals(expectedOutputs[4][0],
                            actualOutput.getMad1(), 1e-3);
                    assertEquals(expectedOutputs[4][1],
                            actualOutput.getMad3(), 1e-3);
                }
            
            }
        }
    }
}
