/*
 * Copyright 2020 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * https://joinup.ec.europa.eu/software/page/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package jdplus.toolkitx.base.r;

import jdplus.toolkit.base.api.data.DoubleSeq;
import jdplus.toolkit.base.api.data.DoublesMath;
import jdplus.toolkit.base.core.arima.IArimaModel;
import jdplus.toolkit.base.core.ssf.arima.ExactArimaForecasts;
import jdplus.toolkit.base.core.ssf.arima.FastArimaForecasts;

/**
 *
 * @author Jean Palate <jean.palate@nbb.be>
 */
@lombok.experimental.UtilityClass
public class ArimaModels {

    public double[] psi(IArimaModel arima, int n) {
        return arima.getPsiWeights().getWeights(n);
    }

    public double[] pi(IArimaModel arima, int n) {
        return arima.getPiWeights().getWeights(n);
    }

    public double[] acf(IArimaModel arima, int n) {
        return arima.getAutoCovarianceFunction().values(n);
    }

    public double[] extendsFast(double[] data, IArimaModel model, double mean, int nb, int nf) {
        FastArimaForecasts fcast = new FastArimaForecasts();
        fcast.prepare(model, mean);
        DoubleSeq b = DoubleSeq.empty(), f = DoubleSeq.empty(), m = DoubleSeq.of(data);
        if (nb > 0) {
            b = fcast.backcasts(m, nb);
        }
        if (nf > 0) {
            f = fcast.forecasts(m, nf);
        }
        return DoublesMath.concatenate(b, m, f).toArray();
    }

    public double[] extendsExact(double[] data, IArimaModel model, boolean mean, int nb, int nf) {
        ExactArimaForecasts fcast = new ExactArimaForecasts();
        fcast.prepare(model, mean);
        DoubleSeq b = DoubleSeq.empty(), f = DoubleSeq.empty(), m = DoubleSeq.of(data);
        if (nb > 0) {
            b = fcast.backcasts(m, nb);
        }
        if (nf > 0) {
            f = fcast.forecasts(m, nf);
        }
        return DoublesMath.concatenate(b, m, f).toArray();
    }
}
