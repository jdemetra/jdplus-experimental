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
import jdplus.toolkit.base.api.data.DoubleSeq;
import jdplus.toolkit.base.core.arima.IArimaModel;
import jdplus.toolkit.base.core.data.DataBlock;
import jdplus.toolkit.base.core.math.matrices.FastMatrix;
import jdplus.toolkit.base.core.math.polynomials.Polynomial;
import jdplus.toolkit.base.core.ssf.ISsfDynamics;
import jdplus.toolkit.base.core.ssf.ISsfInitialization;
import jdplus.toolkit.base.core.ssf.ISsfLoading;
import jdplus.toolkit.base.core.ssf.StateComponent;
import jdplus.toolkit.base.core.ssf.basic.IntegratedInitialization;
import jdplus.toolkit.base.core.ssf.basic.Loading;
import jdplus.toolkitx.base.core.arima.SsfArima2;

/**
 *
 * @author Jean Palate
 */
@lombok.experimental.UtilityClass
public class TimeVaryingSsfArima2 {
    
    public StateComponent of(int n, IntFunction<IArimaModel> fn) {
        
        IArimaModel m0 = fn.apply(0);
        ISsfInitialization initialization = new SsfArima2.MaInitialization(m0.getMaOrder(), m0.getInnovationVariance());
        TimeVaryingArimaData data = new TimeVaryingArimaData(n, fn);
        ISsfDynamics dynamics = new MaDynamics(data.var);
        
        if (m0.isStationary()) {
            return new StateComponent(initialization, dynamics);
            
        } else {
            ISsfLoading[] loading = new ISsfLoading[n];
            for (int i = 0; i < n; ++i) {
                DoubleSeq theta = fn.apply(i).getMa().coefficients();
                double[] th = theta.toArray();
                int q = th.length - 1;
                int[] pos = new int[th.length];
                for (int j = 0; j <= q; ++j) {
                    pos[j] = j;
                }
                
                loading[i] = Loading.from(pos, th);
            }
            DoubleSeq d = m0.getNonStationaryAr().coefficients().drop(1, 0);
            IntegratedDynamics idyn = new IntegratedDynamics(dynamics, loading, d);
            IntegratedInitialization iinit = new IntegratedInitialization(initialization, d);
            return new StateComponent(iinit, idyn);
            
        }
    }
    
    static class TimeVaryingArimaData {
        
        final double[] var, se;
        final FastMatrix phi, theta;
        
        TimeVaryingArimaData(int n, IntFunction<IArimaModel> fn) {
            var = new double[n];
            se = new double[n];
            
            IArimaModel arima = fn.apply(0);
            int p = arima.getStationaryArOrder(), q = arima.getMaOrder();
            phi = FastMatrix.make(n, p);
            theta = FastMatrix.make(n, q + 1);
            for (int i = 0; i < n; ++i) {
                arima = fn.apply(i);
                var[i] = arima.getInnovationVariance();
                se[i] = Math.sqrt(var[i]);
                Polynomial ar = arima.getStationaryAr().asPolynomial();
                Polynomial ma = arima.getMa().asPolynomial();
                if (!phi.isEmpty()) {
                    phi.row(i).copy(ar.coefficients().drop(1, 0));
                }
                theta.row(i).copy(ma.coefficients());
            }
        }
    }
    
    static class MaDynamics implements ISsfDynamics {
        
        final double[] var;
        
        MaDynamics(double[] var) {
            this.var = var;
        }
        
        @Override
        public int getInnovationsDim() {
            return 1;
        }
        
        @Override
        public void V(int pos, FastMatrix qm) {
            qm.set(0, 0, var[pos]);
        }
        
        @Override
        public void S(int pos, FastMatrix cm) {
            cm.set(0, 0, Math.sqrt(var[pos]));
        }
        
        @Override
        public boolean hasInnovations(int pos) {
            return true;
        }
        
        @Override
        public boolean areInnovationsTimeInvariant() {
            return false;
        }
        
        @Override
        public void T(int pos, FastMatrix tr) {
            tr.subDiagonal(-1).set(1);
        }
        
        @Override
        public void TX(int pos, DataBlock x) {
            x.fshiftAndZero();
        }
        
        @Override
        public void addSU(int pos, DataBlock x, DataBlock u) {
            x.add(0, Math.sqrt(var[pos]) * u.get(0));
        }
        
        @Override
        public void addV(int pos, FastMatrix p) {
            p.add(0, 0, var[pos]);
        }
        
        @Override
        public void XT(int pos, DataBlock x) {
            x.bshiftAndZero();
        }
        
        @Override
        public void XS(int pos, DataBlock x, DataBlock xs) {
            xs.mul(0, Math.sqrt(var[pos]));
        }
        
        @Override
        public boolean isTimeInvariant() {
            return false;
        }
    }
    
    static class IntegratedDynamics implements ISsfDynamics {
        
        private final ISsfDynamics dynamics;
        private final ISsfLoading[] loading;
        private final DoubleSeq delta;
        
        public IntegratedDynamics(final ISsfDynamics dynamics, final ISsfLoading[] loading, final DoubleSeq delta) {
            this.dynamics = dynamics;
            this.loading = loading;
            this.delta = delta;
        }
        
        private int order() {
            return delta.length();
        }
        
        @Override
        public int getInnovationsDim() {
            return dynamics.getInnovationsDim();
        }
        
        @Override
        public void V(int pos, FastMatrix qm) {
            dynamics.V(pos, qm.dropTopLeft(order(), order()));
        }
        
        @Override
        public void S(int pos, FastMatrix cm) {
            dynamics.S(pos, cm.dropTopLeft(order(), 0));
        }
        
        @Override
        public boolean hasInnovations(int pos) {
            return dynamics.hasInnovations(pos);
        }
        
        @Override
        public boolean areInnovationsTimeInvariant() {
            return dynamics.areInnovationsTimeInvariant();
        }
        
        @Override
        public void T(int pos, FastMatrix tr) {
            int d = order();
            FastMatrix D = tr.extract(0, d, 0, d);
            D.subDiagonal(-1).set(1);
            DataBlock r0 = tr.row(0);
            r0.extract(0, d).setAY(-1, delta);
            loading[pos].Z(pos, r0.drop(d, 0));
            dynamics.T(pos, tr.dropTopLeft(d, d));
        }
        
        @Override
        public void TX(int pos, DataBlock x) {
            int d = order();
            DataBlock x0 = x.extract(0, d);
            DataBlock x1 = x.drop(d, 0);
            double z = loading[pos].ZX(pos, x1) - delta.dot(x0);
            x0.fshift(1);
            x0.set(0, z);
            dynamics.TX(pos, x1);
        }
        
        @Override
        public void addSU(int pos, DataBlock x, DataBlock u) {
            int d = order();
            DataBlock x1 = x.drop(d, 0);
            dynamics.addSU(pos, x1, u);
        }
        
        @Override
        public void addV(int pos, FastMatrix p) {
            int d = order();
            dynamics.addV(pos, p.dropTopLeft(d, d));
        }
        
        @Override
        public void XT(int pos, DataBlock x) {
            int d = order();
            DataBlock x0 = x.extract(0, d);
            DataBlock x1 = x.drop(d, 0);
            dynamics.XT(pos, x1);
            double w = x0.get(0);
            loading[pos].XpZd(pos, x1, w);
            x0.bshiftAndZero();
            x0.addAY(-w, delta);
        }
        
        @Override
        public void XS(int pos, DataBlock x, DataBlock xs) {
            int d = order();
            xs.extract(0, d).set(0);
            DataBlock x1 = x.drop(d, 0);
            dynamics.XS(pos, x1, xs.drop(d, 0));
        }
        
        @Override
        public boolean isTimeInvariant() {
            return false;
        }
        
    }
    
}
