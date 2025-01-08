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
package jdplus.toolkitx.base.core.arima;

import java.util.Random;
import jdplus.toolkit.base.api.arima.SarimaOrders;
import jdplus.toolkit.base.api.data.DoubleSeq;
import jdplus.toolkit.base.api.data.Doubles;
import jdplus.toolkit.base.api.data.DoublesMath;
import jdplus.toolkit.base.core.data.DataBlock;
import jdplus.toolkit.base.core.sarima.SarimaModel;
import jdplus.toolkit.base.core.ssf.arima.SsfArima;
import jdplus.toolkit.base.core.ssf.dk.DefaultDiffuseFilteringResults;
import jdplus.toolkit.base.core.ssf.dk.DkToolkit;
import jdplus.toolkit.base.core.ssf.dk.FastDkSmoother;
import jdplus.toolkit.base.core.ssf.univariate.Ssf;
import jdplus.toolkit.base.core.ssf.univariate.SsfData;
import tck.demetra.data.Data;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Jean Palate
 */
public class SsfArima2Test {

    public SsfArima2Test() {
    }

    @Test
    public void testArima() {
        SarimaOrders spec = SarimaOrders.airline(12);
        SarimaModel arima = SarimaModel.builder(spec).theta(1, -.6).btheta(1, -.8).build();
        double[] data = Data.PROD.clone();
        Random rnd = new Random(0);
        for (int i = 0; i < 500; ++i) {
            data[rnd.nextInt(data.length)] = Double.NaN;
        }

        DoubleSeq z = DoubleSeq.of(Data.PROD);
//        long t0 = System.currentTimeMillis();
//        for (int i = 0; i < 2000; ++i) {
        Ssf ssf = SsfArima.ssf(arima);
        DefaultDiffuseFilteringResults fr = DkToolkit.filter(ssf, new SsfData(data), true);
        FastDkSmoother smoother = new FastDkSmoother(ssf, fr);
        smoother.smooth(z);
        DataBlock sz = smoother.smoothedStates().item(0);
//        }
//        long t1 = System.currentTimeMillis();
//        System.out.println(t1 - t0);

//        t0 = System.currentTimeMillis();
//        for (int i = 0; i < 2000; ++i) {
        Ssf ssf2 = SsfArima2.ssf(arima);
        DefaultDiffuseFilteringResults fr2 = DkToolkit.filter(ssf2, new SsfData(data), true);
        FastDkSmoother smoother2 = new FastDkSmoother(ssf2, fr2);

        smoother2.smooth(z);
        DataBlock sz2 = smoother2.smoothedStates().item(0);
//        }
//        t1 = System.currentTimeMillis();
//        System.out.println(t1 - t0);
        assertTrue(DoublesMath.subtract(sz, sz2).norm2() < 1e-9);
    }

    @Test
    public void testSsf() {
    }

}
