/*
 * Copyright 2025 JDemetra+.
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *      https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package jdplus.toolkitx.base.r;

import jdplus.sts.base.api.BsmSpec;
import jdplus.sts.base.core.BsmData;
import jdplus.sts.base.r.Bsm;
import jdplus.toolkit.base.api.arima.SarimaOrders;
import jdplus.toolkit.base.api.math.matrices.Matrix;
import jdplus.toolkit.base.api.ssf.sts.SeasonalModel;
import jdplus.toolkit.base.core.sarima.SarimaModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tck.demetra.data.Data;

/**
 *
 * @author Jean Palate
 */
public class OutliersTest {

    public OutliersTest() {
    }

    @Test
    public void testRegArima() {
        double[] y = Data.ABS_RETAIL;
        SarimaOrders spec = SarimaOrders.airline(12);
        SarimaModel sarima = SarimaModel.builder(spec)
                .theta(-.6)
                .btheta(-.6)
                .build();
        Matrix all = Outliers.regarimaOutlier(y, sarima, false, null, new String[]{"ao", "ls"}, "fast", true);
        System.out.println(all);
    }

    @Test
    public void testRegSarima() {
        double[] y = S;
        SarimaOrders spec = SarimaOrders.airline(12);
        SarimaModel sarima = SarimaModel.builder(spec)
                .theta(-.6)
                .btheta(-.6)
                .build();
        Outliers.Results all = Outliers.regarimaOutliers(y, sarima, false, null, 4.5, new String[]{"ao", "ls", "tc"}, "fastc", true);
        System.out.println(all.getOutliers());
    }

    @Test
    public void testTramo() {
        double[] y = Data.RETAIL_BOOKSTORES.clone();
        SarimaOrders spec = SarimaOrders.airline(12);
        SarimaModel sarima = SarimaModel.builder(spec)
                .theta(-.6)
                .btheta(-.6)
                .build();
        Matrix all = Outliers.tramoOutliers(y, sarima, false, null, 0, new String[]{"ao", "ls"}, false, true, false);
        System.out.println(all);
    }

    @Test
    public void testX12() {
        double[] y = Data.RETAIL_BOOKSTORES;
        SarimaOrders spec = SarimaOrders.airline(12);
        SarimaModel sarima = SarimaModel.builder(spec)
                .theta(-.6)
                .btheta(-.6)
                .build();
        Matrix all = Outliers.x12Outliers(y, sarima, false, null, 0, new String[]{"ao", "ls"}, true);
        System.out.println(all);
    }

    @Test
    public void testBsm() {
        double[] y = Data.RETAIL_BOOKSTORES;
        BsmSpec spec = BsmSpec.builder()
                .noise(true)
                .level(true, true)
                .seasonal(SeasonalModel.HarrisonStevens)
                .cycle(false)
                .build();
        Outliers.Results all = Outliers.bsmOutliers(y, 12, spec, null, 2.7 * 2.7, true, true, false, true, "Full", "Point");
        System.out.println(all.getParameters());
    }

    @Test
    public void testBsm2() {
        double[] y = Data.RETAIL_BOOKSTORES;
        BsmData model = BsmData.builder(12)
                .noiseVar(1)
                .levelVar(1)
                .slopeVar(1)
                .seasonalVar(1)
                .seasonalModel(SeasonalModel.HarrisonStevens)
                .build();
        BsmSpec spec = Bsm.specOf(model, false, true);
        Outliers.Results all = Outliers.bsmOutliers(y, 12, spec, null, 2.5 * 2.5, true, true, false, true, "Full", "Point");
        System.out.println(all.getOutliers());
    }

    public static void main(String[] args) {
        double[] y = Data.ABS_RETAIL;
        SarimaOrders spec = SarimaOrders.airline(12);
        SarimaModel sarima = SarimaModel.builder(spec)
                .theta(-.6)
                .btheta(-.6)
                .build();
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < 100; ++i) {
            Matrix all = Outliers.regarimaOutlier(y, sarima, false, null, new String[]{"ao", "ls"}, "ansley", true);
        }
        long t1 = System.currentTimeMillis();
        System.out.println(t1 - t0);
        t0 = System.currentTimeMillis();
        for (int i = 0; i < 100; ++i) {
            Matrix all = Outliers.regarimaOutlier(y, sarima, false, null, new String[]{"ao", "ls"}, "fast", true);
        }
        t1 = System.currentTimeMillis();
        System.out.println(t1 - t0);
        t0 = System.currentTimeMillis();
        for (int i = 0; i < 100; ++i) {
            Matrix all = Outliers.regarimaOutlier(y, sarima, false, null, new String[]{"ao", "ls", "tc", "tc:0.9"}, "fast", false);
        }
        t1 = System.currentTimeMillis();
        System.out.println(t1 - t0);
        t0 = System.currentTimeMillis();
        for (int i = 0; i < 100; ++i) {
            Matrix all = Outliers.regarimaOutlier(y, sarima, false, null, new String[]{"ao", "ls", "tc", "tc:0.9"}, "ansley", false);
        }
        t1 = System.currentTimeMillis();
        System.out.println(t1 - t0);
        t0 = System.currentTimeMillis();
        for (int i = 0; i < 100; ++i) {
            Matrix all = Outliers.regarimaOutlier(y, sarima, false, null, new String[]{"ao", "ls", "tc", "tc:0.9"}, "kalman", false);
        }
        t1 = System.currentTimeMillis();
        System.out.println(t1 - t0);
        t0 = System.currentTimeMillis();
        for (int i = 0; i < 100; ++i) {
            Matrix all = Outliers.regarimaOutlier(y, sarima, false, null, new String[]{"ao", "ls", "tc", "tc:0.9"}, "ljungbox", false);
        }
        t1 = System.currentTimeMillis();
        System.out.println(t1 - t0);
        t0 = System.currentTimeMillis();
        for (int i = 0; i < 100; ++i) {
            Matrix all = Outliers.regarimaOutlier(y, sarima, false, null, new String[]{"ao", "ls", "tc", "tc:0.9"}, "x12", false);
        }
        t1 = System.currentTimeMillis();
        System.out.println(t1 - t0);
    }

    private static final double[] S = new double[]{
        6.045968, 6.096813, 6.122618, 6.164997, 6.164369, 6.151369, 6.130501, 6.146288, 6.127681, 6.136532, 6.1497, 6.172021,
        6.20388, 6.218123, 6.207076, 6.22827, 6.236534, 6.251495, 6.224947, 6.251124, 6.252108, 6.270963, 6.290585, 6.3126,
        6.322089, 6.34946, 6.312017, 6.351572, 6.348021, 6.354955, 6.32692, 6.317231, 6.31229, 6.300938, 6.159746, 6.029988,
        6.204407, 6.25687, 6.252851, 6.214736, 6.171388, 6.131262, 6.037656, 6.005146, 5.990401, 6.014727, 6.006404, 6.010323,
        6.007592, 6.033103, 6.108039, 6.109739, 6.114476, 6.122527, 6.104102, 6.06023, 6.081631, 6.077339, 6.09031, 6.061857,
        5.986265, 6.056793, 6.051556, 6.07919, 6.078176, 6.085993, 6.043835, 5.956626, 5.946543, 5.964835, 5.973338, 5.922049,
        5.789473, 5.61957, 5.489163, 5.860573, 5.984366, 6.007972, 5.99544, 5.976875, 6.002722, 6.007354, 6.011885, 6.037741,
        5.993564, 5.994016, 6.04376, 6.022652, 6.030835, 6.016456, 6.003758, 6.016559, 6.039972, 6.037596, 6.029113, 6.050138,
        6.044055, 6.072829, 6.066926, 6.092941, 6.026028, 5.919898, 5.795994, 5.797473, 5.797516, 5.809941, 5.862276, 5.853987,
        5.884354, 5.882196, 5.880539, 5.715481, 5.72601, 5.737957, 5.842326, 5.836221, 5.831813, 5.829592, 5.791703, 5.752651,
        5.657281, 5.610723, 5.563602, 5.510839, 5.451574, 5.386374, 5.318588, 5.250185, 5.186282, 5.130487, 5.088733, 5.06691,
        5.066977, 5.08764, 5.125052, 5.16793, 5.199968, 5.211837, 5.209547, 5.204683, 5.207169, 5.219034, 5.239374, 5.267326,
        5.302805, 5.343245, 5.385939, 5.418818, 5.425917, 5.399167, 5.350137, 5.297953, 5.261133, 5.240582, 5.232096, 5.232101,
        5.239507, 5.25304, 5.270864, 5.286336, 5.290139, 5.276328, 5.24702, 5.207998, 5.169334, 5.142096, 5.138787, 5.166657,
        5.214985, 5.264375, 5.302517, 5.313246, 5.274489, 5.176278, 5.049546, 4.950616, 4.930727, 4.966834, 5.012981, 5.040177,
        5.059408, 5.090367, 5.145014, 5.209546, 5.259115, 5.280013, 5.28106, 5.279011, 5.28644, 5.301013, 5.315288, 5.324232,
        5.330501, 5.338621, 5.351304, 5.36445, 5.371322, 5.367931, 5.358083, 5.348869, 5.345844, 5.346046, 5.343872, 5.335926,
        5.325617, 5.319201, 5.319066, 5.319655, 5.312276, 5.292042, 5.265906, 5.245082, 5.238799, 5.245209, 5.259319, 5.276264,
        5.291529, 5.300236, 5.300056, 5.290087, 5.270207, 5.243411, 5.220347, 5.214418, 5.231427, 5.25433, 5.260365, 5.234531,
        5.185009, 5.132831, 5.092122, 5.07093, 5.077654, 5.110779, 5.151819, 5.180321, 5.183859, 5.169852, 5.150885, 5.137303,
        5.129998, 5.126803, 5.125196, 5.123341, 5.119616, 5.113273, 5.105953, 5.100029, 5.097189, 5.095956, 5.094057, 5.090306,
        5.086853, 5.086986, 5.092255, 5.100282, 5.106801, 5.108742, 5.106169, 5.100143, 5.091963, 5.082542, 5.072903, 5.064806,
        5.062066, 5.068678, 5.085523, 5.10521, 5.116222, 5.111877, 5.097673, 5.083424, 5.077456, 5.079806, 5.088032, 5.099541,
        5.111233, 5.119277, 5.122154, 5.120629, 5.116222, 5.110864, 5.106688, 5.105895, 5.10922, 5.113125, 5.113001, 5.106332,
        5.096761, 5.090497, 5.091334, 5.097921, 5.10704, 5.115227, 5.1188, 5.114009, 5.099688, 5.081613, 5.068206, 5.06493,
        5.06683, 5.065847, 5.056998, 5.043183, 5.031582, 5.028045, 5.032364, 5.042016, 5.052209, 5.053338, 5.03425, 4.990125,
        4.939489, 4.914049, 4.930193, 4.970388, 5.003588, 5.009698, 4.996437, 4.980516, 4.974526, 4.97468, 4.972353, 4.962431,
        4.95071, 4.947028, 4.95574, 4.969358, 4.975507, 4.967297, 4.953058, 4.946529, 4.955779, 4.970603, 4.975996, 4.963455,
        4.943333, 4.933381, 4.943401, 4.963703, 4.978111, 4.975739, 4.959184, 4.9353, 4.911728, 4.893638, 4.88634, 4.892616,
        4.90716, 4.921992, 4.933388, 4.942059, 4.950383, 4.958719, 4.956853, 4.930631, 4.873221, 4.798474, 4.73302, 4.701296,
        4.698996, 4.709984, 4.722386, 4.734551, 4.74802, 4.764087, 4.779399, 4.789549, 4.791705, 4.78794, 4.782079, 4.777249,
        4.773264, 4.769705, 4.766586, 4.765577, 4.770231, 4.783471, 4.781377, 4.784776, 4.783972, 4.77979, 4.774742, 4.771631,
        4.771952, 4.777061, 4.787467, 4.79825, 4.801961, 4.796977, 4.793328, 4.794513, 4.805819, 4.815492, 4.809279, 4.780877,
        4.746531, 4.734674, 4.760121, 4.803464, 4.832496, 4.828837, 4.819375, 4.79266, 4.769562, 4.753933, 4.748085, 4.752868,
        4.76216, 4.768503, 4.768087, 4.761229, 4.750049, 4.73903, 4.74745, 4.746435, 4.741472, 4.734284, 4.729118, 4.729737,
        4.735742, 4.745946, 4.759656, 4.772026, 4.776959, 4.773316, 4.783879, 4.786609, 4.792287, 4.803245, 4.82266, 4.850933,
        4.87664, 4.885023, 4.871359, 4.852187, 4.853723, 4.890445, 4.955581, 4.986583, 4.969607, 4.932072, 4.89008, 4.866847,
        4.843536, 4.791929, 4.690902, 4.559729, 4.462495, 4.45605, 4.534954, 4.597546, 4.615283, 4.619319, 4.631647, 4.677673,
        4.740078, 4.795713, 4.831867, 4.850616, 4.857063, 4.858186, 4.87953, 4.878905, 4.86389, 4.850449, 4.834511, 4.82722,
        4.828203, 4.84196, 4.865211, 4.887875, 4.896824, 4.886999, 4.891174, 4.880978, 4.873031, 4.875252, 4.870527, 4.860566,
        4.846606, 4.840339, 4.842883, 4.845095, 4.835199, 4.808225, 4.797778, 4.777508, 4.767219, 4.776901, 4.791131, 4.811926,
        4.835438, 4.863384, 4.892354, 4.911338, 4.905889, 4.8717, 4.849654, 4.825269, 4.824775, 4.84266, 4.843181, 4.817446,
        4.775764, 4.748634, 4.750397, 4.773094, 4.801045, 4.823049, 4.859955, 4.871192, 4.865937, 4.858373, 4.841568, 4.82481,
        4.806458, 4.79234, 4.780594, 4.770188, 4.763324, 4.761185, 4.779626, 4.767211, 4.727755, 4.68195, 4.635098, 4.606389,
        4.590885, 4.58531, 4.581061, 4.574334, 4.564516, 4.553218, 4.564343, 4.557669, 4.544101, 4.538373, 4.534247, 4.540348,
        4.549114, 4.556963, 4.559161, 4.558195, 4.562622, 4.579197, 4.625147, 4.65121, 4.660194, 4.66331, 4.651164, 4.633697,
        4.619039, 4.624169, 4.65274, 4.688656, 4.709926, 4.705564, 4.70924, 4.698583, 4.694557, 4.696614, 4.675756, 4.628162,
        4.568517, 4.534178, 4.541505, 4.569887, 4.586418, 4.571212, 4.558531, 4.529926, 4.513828, 4.52276, 4.54273, 4.573368,
        4.594182, 4.589017, 4.545055, 4.470795, 4.394738, 4.347998, 4.3545, 4.361411, 4.371931, 4.405326, 4.461923, 4.547725,
        4.634754, 4.697494, 4.727521, 4.727369, 4.704167, 4.669922, 4.659875, 4.646795, 4.648476, 4.664049, 4.665961, 4.649923,
        4.619675, 4.595213, 4.585263, 4.582975, 4.579422, 4.569677, 4.578522, 4.570995, 4.561872, 4.563096, 4.564597, 4.57303,
        4.580936, 4.586408, 4.586024, 4.577916, 4.564741, 4.551285, 4.562417, 4.559818, 4.555056, 4.556836, 4.551602, 4.544563,
        4.532208, 4.536687, 4.542275, 4.537637, 4.528208, 4.515826, 4.478812, 4.438558, 4.54201, 4.499362, 4.474219, 4.470879,
        4.424281, 4.40036, 4.396214, 4.410099, 4.411493, 4.401676, 4.408224

    };
}
