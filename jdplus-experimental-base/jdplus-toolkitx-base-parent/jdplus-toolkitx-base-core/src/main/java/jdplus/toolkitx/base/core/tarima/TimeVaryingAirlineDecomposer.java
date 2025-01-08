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

import jdplus.toolkit.base.api.arima.SarimaOrders;
import jdplus.toolkit.base.core.sarima.SarimaModel;
import jdplus.toolkit.base.core.ucarima.ModelDecomposer;
import jdplus.toolkit.base.core.ucarima.SeasonalSelector;
import jdplus.toolkit.base.core.ucarima.TrendCycleSelector;
import jdplus.toolkit.base.core.ucarima.UcarimaException;
import jdplus.toolkit.base.core.ucarima.UcarimaModel;

/**
 *
 * @author Jean Palate
 */
public class TimeVaryingAirlineDecomposer {

    private int period;
    private final double[] thetas, bthetas;
    private final UcarimaModel[] ucms;

    private boolean modified;

    public TimeVaryingAirlineDecomposer(int period, double[] thetas, double[] bthetas) {
        if (thetas.length != bthetas.length) {
            throw new IllegalArgumentException();
        }
        this.period = period;
        this.thetas = thetas;
        this.bthetas = bthetas;
        ucms = new UcarimaModel[thetas.length];
        int first = -1, last = -1;
        for (int i = 0; i < ucms.length; ++i) {
            UcarimaModel ucm = ucm(i);
            ucms[i] = ucm;
            if (ucm != null) {
                if (first == -1) {
                    first = i;
                }
                last = i;
            }
        }
        if (first == -1){
            throw new UcarimaException();
           
        }
        if (first != 0){
            modified=true;
            for (int i=0; i<first; ++i){
                ucms[i]=ucms[first];
            }
        }
        if (last+1 != ucms.length){
            modified=true;
            for (int i=last+1; i<ucms.length; ++i){
                ucms[i]=ucms[last];
            }
        }
    }
    
    public UcarimaModel[] ucarimaModels(){
        return ucms;
    }
    
    

    private UcarimaModel ucm(int idx) {
        SarimaModel airline = SarimaModel.builder(SarimaOrders.airline(period))
                .theta(thetas[idx])
                .btheta(bthetas[idx])
                .build();
        TrendCycleSelector tsel = new TrendCycleSelector();
        SeasonalSelector ssel = new SeasonalSelector(period);

        ModelDecomposer decomposer = new ModelDecomposer();
        decomposer.add(tsel);
        decomposer.add(ssel);

        UcarimaModel ucm = decomposer.decompose(airline);
        ucm = ucm.setVarianceMax(2, false);
//        ucm = ucm.setVarianceMax(-1, false);
        return ucm;
    }
    
    public boolean isModified(){
        return modified;
    }
}
