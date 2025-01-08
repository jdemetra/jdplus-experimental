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

import java.util.function.IntFunction;
import jdplus.toolkit.base.core.ssf.basic.Loading;
import jdplus.toolkit.base.core.ssf.composite.CompositeSsf;
import jdplus.toolkit.base.core.ucarima.UcarimaModel;
import jdplus.toolkitx.base.core.arima.SsfArima2;

/**
 *
 * @author Jean Palate
 */
@lombok.experimental.UtilityClass
public class TimeVaryingSsfUcarima2 {
    
        public CompositeSsf of(int n, IntFunction<UcarimaModel> fn) {
            int m = fn.apply(0).getComponentsCount();
        CompositeSsf.Builder builder = CompositeSsf.builder();
        for (int i = 0; i < m; ++i) {
            int cmp=i;
            builder.add(TimeVaryingSsfArima2.of(n, k->fn.apply(k).getComponent(cmp)), Loading.fromPosition(0));
//            builder.add(SsfArima2.stateComponent(fn.apply(0).getComponent(cmp)), Loading.fromPosition(0));
        }
        return builder.build();
    }

}
