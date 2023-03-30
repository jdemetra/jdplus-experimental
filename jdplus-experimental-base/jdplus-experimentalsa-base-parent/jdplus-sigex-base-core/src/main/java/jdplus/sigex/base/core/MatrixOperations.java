/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdplus.sigex.base.core;

import jdplus.toolkit.base.core.math.matrices.FastMatrix;
import jdplus.toolkit.base.core.math.matrices.decomposition.LDLDecomposition;

/**
 *
 * @author Jean Palate <jean.palate@nbb.be>
 */
@lombok.experimental.UtilityClass
public class MatrixOperations {
    FastMatrix[] gcd(FastMatrix x, int rank){
        LDLDecomposition ldl=new LDLDecomposition();
        ldl.decompose(x, 1e-9);
        return new FastMatrix[]{ldl.L(), ldl.D()};
    }
}
