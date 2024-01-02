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

import jdplus.toolkit.base.api.math.matrices.Matrix;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Jean Palate <jean.palate@nbb.be>
 */
public class PolynomialsTest {
    
    public PolynomialsTest() {
    }

    @Test
    public void testRoots() {
        double[] p=new double[]{1, -.5, .3, -.04};
        Matrix roots = Polynomials.roots(p, false);
//        for (int i=0; i<roots.getRowsCount(); ++i){
//            System.out.println(roots.row(i));
//        }
        
        roots = Polynomials.roots(p, true);
//        for (int i=0; i<roots.getRowsCount(); ++i){
//            System.out.println(roots.row(i));
//        }
    }
    
}
