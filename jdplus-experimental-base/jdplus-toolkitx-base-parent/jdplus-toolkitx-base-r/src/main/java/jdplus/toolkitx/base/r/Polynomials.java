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

import jdplus.toolkit.base.api.math.Complex;
import jdplus.toolkit.base.api.math.matrices.Matrix;
import jdplus.toolkit.base.core.data.DataBlock;
import jdplus.toolkit.base.core.math.ComplexUtility;
import jdplus.toolkit.base.core.math.matrices.FastMatrix;
import jdplus.toolkit.base.core.math.matrices.decomposition.EigenSystem;
import jdplus.toolkit.base.core.math.polynomials.Polynomial;

/**
 *
 * @author Jean Palate <jean.palate@nbb.be>
 */
@lombok.experimental.UtilityClass
public class Polynomials {

    public Matrix roots(double[] coeff, boolean robust) {
        Polynomial p = Polynomial.raw(coeff);

        Complex[] roots;
        if (robust) {
            roots = robustRoots(p);
        } else {
            roots = p.roots();
        }
        return convert(roots);
    }

    private FastMatrix convert(Complex[] c) {
        if (c == null || c.length == 0) {
            return null;
        }
        FastMatrix R = FastMatrix.make(c.length, 2);
        for (int i = 0; i < c.length; ++i) {
            Complex ci = c[i];
            R.set(i, 0, ci.getRe());
            R.set(i, 1, ci.getIm());
        }
        return R;

    }

    private Complex[] convert(Matrix R) {
        Complex[] c = new Complex[R.getRowsCount()];
        for (int i = 0; i < c.length; ++i) {
            c[i] = Complex.cart(R.get(i, 0), R.get(i, 1));
        }
        return c;
    }

    public Matrix lejaOrder(Matrix R) {
        Complex[] c = convert(R);
        ComplexUtility.lejaOrder(c);
        return convert(c);
    }

    public double[] polynomialOfRoots(Matrix R, double d, boolean smooth, boolean order) {
        Complex[] c = convert(R);
        if (order) {
            ComplexUtility.lejaOrder(c);
        }
        Polynomial P = Polynomial.fromComplexRoots(c, d);
        if (smooth) {
            P = P.smooth();
        }
        return P.toArray();
    }

    public double[] convolve(double[] w) {
        double[] c = new double[w.length];
        for (int i = 0; i < w.length; ++i) {
            for (int j = i; j < w.length; ++j) {
                c[j - i] += w[i] * w[j];
            }
        }
        return c;
    }

    public double[] evaluate(double[] p, double[] x) {
        double[] px = new double[x.length];
        Polynomial P = Polynomial.ofInternal(p);
        for (int i = 0; i < x.length; ++i) {
            px[i] = P.evaluateAt(x[i]);
        }
        return px;
    }

    public double evaluate(double[] p, double x) {
        Polynomial P = Polynomial.ofInternal(p);
        return P.evaluateAt(x);
    }

    private Complex[] robustRoots(Polynomial p) {
        int n = p.degree();
        if (n == 0) {
            return null;
        }
        // Build the companion matrix
        FastMatrix C = FastMatrix.square(n);
        C.subDiagonal(1).set(1);
        DataBlock row = C.row(n - 1);
        double pn = p.get(n);
        row.setAY(-1 / pn, p.coefficients());
        return EigenSystem.create(C).getEigenValues();
    }
}
