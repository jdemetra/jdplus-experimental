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
import jdplus.toolkit.base.api.data.DoubleSeqCursor;
import jdplus.toolkit.base.core.arima.IArimaModel;
import jdplus.toolkit.base.core.data.DataBlock;
import jdplus.toolkit.base.core.data.DataBlockIterator;
import jdplus.toolkit.base.core.math.matrices.FastMatrix;
import jdplus.toolkit.base.core.math.matrices.SymmetricMatrix;
import jdplus.toolkit.base.core.math.polynomials.Polynomial;
import jdplus.toolkit.base.core.math.polynomials.RationalFunction;
import jdplus.toolkit.base.core.ssf.ISsfDynamics;
import jdplus.toolkit.base.core.ssf.ISsfInitialization;
import jdplus.toolkit.base.core.ssf.StateComponent;
import jdplus.toolkit.base.core.ssf.arima.SsfArima;

/**
 *
 * @author Jean Palate
 */
@lombok.experimental.UtilityClass
public class TimeVaryingSsfArima {

    public StateComponent of(int n, IntFunction<IArimaModel> fn) {

        IArimaModel m0 = fn.apply(0);
        StateComponent cmp0 = SsfArima.stateComponent(m0);
        ISsfInitialization initialization = cmp0.initialization();
        Dynamics dynamics = new Dynamics(new ArimasData(n, fn));
        return new StateComponent(initialization, dynamics);
    }

    static class ArimasData {

        final int dim;
        final double[] var, se;
        final FastMatrix phi, psi;

        ArimasData(int n, IntFunction<IArimaModel> fn) {
            var = new double[n];
            se = new double[n];

            IArimaModel arima = fn.apply(0);
            int p = arima.getArOrder(), q = arima.getMaOrder();
            dim = Math.max(p, q + 1);
            phi = FastMatrix.make(n, p + 1);
            psi = FastMatrix.make(n, dim);
            for (int i = 0; i < n; ++i) {
                arima = fn.apply(i);
                var[i] = arima.getInnovationVariance();
                se[i] = Math.sqrt(var[i]);
                Polynomial ar = arima.getAr().asPolynomial();
                Polynomial ma = arima.getMa().asPolynomial();
                phi.row(i).copy(ar.coefficients());
                psi.row(i).copy(DataBlock.of(RationalFunction.of(ma, ar).coefficients(dim)));
            }
        }
    }

    static class Dynamics implements ISsfDynamics {

        private final ArimasData data;
        private final DataBlock z;

        public Dynamics(ArimasData data) {
            this.data = data;
            z = DataBlock.make(data.dim);
        }

        /**
         *
         * @param pos
         * @param tr
         */
        @Override
        public void T(final int pos, final FastMatrix tr) {
            tr.set(0);
            for (int i = 1; i < data.dim; ++i) {
                tr.set(i - 1, i, 1);
            }
            DataBlock phi = data.phi.row(pos);
            for (int i = 1; i < phi.length(); ++i) {
                tr.set(data.dim - 1, data.dim - i, -phi.get(i));
            }
        }

        /**
         *
         * @param pos
         * @param vm
         */
        @Override
        public void TVT(final int pos, final FastMatrix vm) {
            if (data.phi.getColumnsCount() == 1) {
                vm.upLeftShift(1);
                vm.column(data.dim - 1).set(0);
                vm.row(data.dim - 1).set(0);
            } else {
                DataBlock phi = data.phi.row(pos);
                z.set(0);
                DataBlockIterator cols = vm.reverseColumnsIterator();
                for (int i = 1; i < phi.length(); ++i) {
                    z.addAY(-phi.get(i), cols.next());
                }
                TX(pos, z);
                vm.upLeftShift(1);
                vm.column(data.dim - 1).copy(z);
                vm.row(data.dim - 1).copy(z);
            }

        }

        /**
         *
         * @param pos
         * @param x
         */
        @Override
        public void TX(final int pos, final DataBlock x) {
            double tx = 0;
            if (data.phi.getColumnsCount() > 1) {
                DoubleSeqCursor reader = x.reverseReader();
                DoubleSeqCursor.OnMutable phi = data.phi.row(pos).cursor();
                phi.skip(1);
                for (int i = 1; i < data.phi.getColumnsCount(); ++i) {
                    tx -= phi.getAndNext() * reader.getAndNext();
                }
            }
            x.bshift(1);
            x.set(data.dim - 1, tx);
        }

        /**
         *
         * @param pos
         * @param x
         */
        @Override
        public void XT(final int pos, final DataBlock x) {
            double last = -x.get(data.dim - 1);
            x.fshift(1);
            x.set(0, 0);
            if (last != 0) {
                DoubleSeqCursor.OnMutable phi = data.phi.row(pos).cursor();
                phi.skip(1);

                for (int i = 1, j = data.dim - 1; i < data.phi.getColumnsCount(); ++i, --j) {
                    double cur = phi.getAndNext();
                    if (cur != 0) {
                        x.add(j, last * cur);
                    }
                }
            }
        }

        @Override
        public boolean isTimeInvariant() {
            return false;
        }

        @Override
        public boolean areInnovationsTimeInvariant() {
            return false;
        }

        @Override
        public int getInnovationsDim() {
            return 1;
        }

        @Override
        public void V(int pos, FastMatrix qm) {
            SymmetricMatrix.xxt(data.psi.row(pos), qm);
            qm.mul(data.var[pos]);
        }

        @Override
        public void S(int pos, FastMatrix sm) {
            sm.column(0).copy(data.psi.row(pos));
            sm.mul(data.se[pos]);
        }

        @Override
        public boolean hasInnovations(int pos) {
            return true;
        }

        @Override
        public void addV(int pos, FastMatrix p) {
            p.addXaXt(data.var[pos], data.psi.row(pos));
        }

        @Override
        public void XS(int pos, DataBlock x, DataBlock sx) {
            double a = x.dot(data.psi.row(pos)) * data.se[pos];
            sx.set(0, a);
        }

        @Override
        public void addSU(int pos, DataBlock x, DataBlock u) {
            double a = u.get(0) * data.se[pos];
            x.addAY(a, data.psi.row(pos));
        }

    }

}
