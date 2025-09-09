/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdplus.businesscycle.base.core;

import jdplus.toolkit.base.api.arima.SarimaOrders;
import tck.demetra.data.Data;
import jdplus.toolkit.base.api.data.DoubleSeq;
import jdplus.toolkit.base.core.data.DataBlockStorage;
import jdplus.toolkit.base.core.sarima.SarimaModel;
import jdplus.toolkit.base.core.ssf.dk.DkToolkit;
import jdplus.toolkit.base.core.ssf.composite.CompositeSsf;
import jdplus.toolkit.base.core.ssf.univariate.SsfData;
import jdplus.toolkit.base.core.ucarima.ModelDecomposer;
import jdplus.toolkit.base.core.ucarima.SeasonalSelector;
import jdplus.toolkit.base.core.ucarima.TrendCycleSelector;
import jdplus.toolkit.base.core.ucarima.UcarimaModel;
import jdplus.toolkit.base.core.ssf.arima.SsfUcarima;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author palatej
 */
public class ModifiedHodrickPrescottFilterTest {
    
    public ModifiedHodrickPrescottFilterTest() {
    }

    @Test
    public void testMonthly() {
        UcarimaModel ucmAirline = ucmAirline(12, -.6, -.8);
        CompositeSsf ssf = SsfUcarima.of(ucmAirline);
        DoubleSeq y=DoubleSeq.of(Data.PROD);
        DoubleSeq ly=y.fn(x->Math.log(x));
        DataBlockStorage all = DkToolkit.fastSmooth(ssf, new SsfData(ly));
        ModifiedHodrickPrescottFilter mhp=new ModifiedHodrickPrescottFilter(150000);
        DoubleSeq[] tc = mhp.process(ucmAirline.getComponent(0), all.item(0));
        HodrickPrescottFilter hp=new HodrickPrescottFilter(150000);
        DoubleSeq[] tc2 = hp.process(all.item(0));
//        System.out.println(all.item(0));
//        System.out.println(tc[0]);
//        System.out.println(tc2[0]);
//        System.out.println(tc[1]);
//        System.out.println(tc2[1]);

        assertTrue(all.item(0).distance(tc[0].op(tc[1], (a,b)->a+b))<1e-9);
        assertTrue(all.item(0).distance(tc2[0].op(tc2[1], (a,b)->a+b))<1e-9);
    }
    
    public static UcarimaModel ucmAirline(int period, double th, double bth) {
        SarimaOrders spec=SarimaOrders.airline(period);
        SarimaModel sarima = SarimaModel.builder(spec)
                .theta(1, th)
                .btheta(1, bth)
                .build();

        TrendCycleSelector tsel = new TrendCycleSelector();
        SeasonalSelector ssel = new SeasonalSelector(period);

        ModelDecomposer decomposer = new ModelDecomposer();
        decomposer.add(tsel);
        decomposer.add(ssel);

        UcarimaModel ucm = decomposer.decompose(sarima);
        ucm = ucm.setVarianceMax(-1, false);
        return ucm;
    }
}
