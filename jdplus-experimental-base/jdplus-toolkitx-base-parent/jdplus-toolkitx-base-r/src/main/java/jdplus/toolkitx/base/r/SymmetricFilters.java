/*
 * Copyright 2022 National Bank of Belgium
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

import jdplus.toolkit.base.core.math.linearfilters.BackFilter;
import jdplus.toolkit.base.core.math.linearfilters.SymmetricFilter;
import jdplus.toolkit.base.core.math.polynomials.Polynomial;

/**
 *
 * @author palatej
 */
@lombok.experimental.UtilityClass
public class SymmetricFilters {
    
    public double[] decompose(double[] s, double[] denom){
        SymmetricFilter sf=SymmetricFilter.ofInternal(s);
        BackFilter bf=new BackFilter(Polynomial.ofInternal(denom));
        BackFilter num = sf.decompose(bf);
        return num == null ? null : num.asPolynomial().toArray();
    } 

    public double[] factorize(double[] s){
        SymmetricFilter sf=SymmetricFilter.ofInternal(s);
        SymmetricFilter.Factorization fac = sf.factorize();
        double[] rslt=new double[1+fac.factor.length()];
        rslt[0]=fac.scaling;
        fac.factor.coefficients().copyTo(rslt, 1);
        return rslt;
    } 
}
