/*
 * Copyright 2024 JDemetra+.
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
package jdplus.toolkitx.base.core.tarima;

import jdplus.toolkit.base.api.data.DoubleSeq;
import jdplus.toolkit.base.core.ssf.composite.CompositeSsf;
import jdplus.toolkit.base.core.ssf.dk.DkToolkit;
import jdplus.toolkit.base.core.ssf.univariate.DefaultSmoothingResults;
import jdplus.toolkit.base.core.ssf.univariate.SsfData;
import jdplus.toolkit.base.core.ucarima.UcarimaModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tck.demetra.data.Data;

/**
 *
 * @author Jean Palate
 */
public class TimeVaryingAirlineDecomposerTest {

    public TimeVaryingAirlineDecomposerTest() {
    }

    @Test
    public void testLinear() {
        double[] s = Data.RETAIL_BOOKSTORES;
        double[] th = linear(s.length, 0.3, -0.6);
        double[] bth = linear(s.length, 0.1, -0.8);
        long t0 = System.currentTimeMillis();
        TimeVaryingAirlineDecomposer decomposer = new TimeVaryingAirlineDecomposer(12, th, bth);
        UcarimaModel[] ucarimaModels = decomposer.ucarimaModels();
        
        CompositeSsf ssf = TimeVaryingSsfUcarima.of(s.length, i->ucarimaModels[i]);
        
        DefaultSmoothingResults sf = DkToolkit.smooth(ssf, new SsfData(s), false, false);
        long t1 = System.currentTimeMillis();
        System.out.println(t1 - t0);
        int[] pos = ssf.componentsPosition();
        System.out.println(sf.item(pos[0]));
        System.out.println(sf.item(pos[1]));
        System.out.println(sf.item(pos[2]));
        System.out.println(DoubleSeq.of(s));
//        for (int i = 0; i < ucarimaModels.length; ++i) {
//            UcarimaModel ucm = ucarimaModels[i];
//            for (int j = 0; j < ucm.getComponentsCount(); ++j) {
//                System.out.print(ucm.getComponent(j).getInnovationVariance());
//                System.out.print('\t');
//            }
//            System.out.println();
//        }
    }

    public static double[] linear(int n, double a, double b) {
        double d = (b - a) / (n - 1.0);
        double[] s = new double[n];
        s[0] = a;
        double cur = a;
        for (int i = 1; i < n; ++i) {
            cur += d;
            s[i] = cur;
        }
        return s;
    }

}
