/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdplus.businesscycle.base.core;

import jdplus.toolkit.base.api.data.DoubleSeq;
import jdplus.toolkit.base.core.arima.ArimaModel;
import jdplus.toolkit.base.core.math.linearfilters.BackFilter;
import jdplus.toolkit.base.core.math.polynomials.Polynomial;
import jdplus.toolkit.base.core.math.polynomials.UnitRoots;
import jdplus.toolkit.base.core.ssf.dk.DkToolkit;
import jdplus.toolkit.base.core.ssf.composite.CompositeSsf;
import jdplus.toolkit.base.core.ssf.univariate.DefaultSmoothingResults;
import jdplus.toolkit.base.core.ssf.univariate.SsfData;
import jdplus.toolkit.base.core.ucarima.UcarimaModel;
import jdplus.toolkit.base.core.ssf.arima.SsfUcarima;

/**
 *
 * @author palatej
 */
public class ModifiedHodrickPrescottFilter {

    private final double lambda;
    private UcarimaModel ucm;
    private CompositeSsf ssf;

    public ModifiedHodrickPrescottFilter(double snRatio) {
        this.lambda = snRatio;
    }

    public DoubleSeq[] process(ArimaModel trendModel, DoubleSeq trend) {
        Polynomial D = UnitRoots.D(1), D2 = D.times(D);
        ArimaModel i2 = new ArimaModel(BackFilter.ONE, new BackFilter(D2), BackFilter.ONE, 1);
        ArimaModel sum = ArimaModel.add(lambda, i2);

        BackFilter ar = trendModel.getStationaryAr(), diff = trendModel.getNonStationaryAr(),
                ma = trendModel.getMa();
        double tvar = trendModel.getInnovationVariance();

        while (diff.getDegree() < 2) {
            diff = diff.times(BackFilter.D1);
            ma = ma.times(BackFilter.D1);
        }
        BackFilter nar = ar.times(sum.getMa());
        ArimaModel cycle = new ArimaModel(nar, BackFilter.ONE, ma, tvar * lambda / sum.getInnovationVariance());
        ArimaModel ltrend= new ArimaModel(nar, diff, ma, tvar / sum.getInnovationVariance());
        ucm = UcarimaModel.builder()
                .add(ltrend)
                .add(cycle)
                .build();
        ssf = SsfUcarima.of(ucm);
        DefaultSmoothingResults ss = DkToolkit.sqrtSmooth(ssf, new SsfData(trend), false, false);
        DoubleSeq t = ss.getComponent(0);
        DoubleSeq c = ss.getComponent(ssf.componentsPosition()[1]);
        return new DoubleSeq[]{DoubleSeq.of(t.toArray()), DoubleSeq.of(c.toArray())};
    }
}
