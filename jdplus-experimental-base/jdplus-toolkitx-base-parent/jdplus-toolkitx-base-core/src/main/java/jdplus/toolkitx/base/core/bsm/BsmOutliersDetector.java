/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdplus.toolkitx.base.core.bsm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jdplus.toolkit.base.api.data.DoubleSeq;
import jdplus.toolkit.base.api.data.DoublesMath;
import jdplus.sts.base.api.BsmEstimationSpec;
import nbbrd.design.BuilderPattern;
import jdplus.sts.base.api.BsmSpec;
import jdplus.sts.base.api.Component;
import jdplus.sts.base.core.BsmData;
import jdplus.sts.base.core.BsmKernel;
import jdplus.sts.base.core.BsmMapping;
import jdplus.sts.base.core.SsfBsm2;
import jdplus.sts.base.core.SsfOutlierDetector;
import jdplus.toolkit.base.core.data.DataBlock;
import jdplus.toolkit.base.core.stats.likelihood.DiffuseConcentratedLikelihood;
import jdplus.toolkit.base.core.math.linearsystem.LinearSystemSolver;
import jdplus.toolkit.base.core.math.functions.IFunctionDerivatives;
import jdplus.toolkit.base.core.math.matrices.FastMatrix;
import jdplus.toolkit.base.core.ssf.dk.SsfFunction;
import jdplus.toolkit.base.core.ssf.dk.SsfFunctionPoint;
import jdplus.toolkit.base.core.ssf.univariate.SsfData;
import jdplus.toolkit.base.core.stats.RobustStandardDeviationComputer;
import jdplus.toolkitx.base.api.processing.IProcessingHook.HookInformation;
import jdplus.toolkitx.base.api.processing.IProcessingHookProvider;
import jdplus.toolkitx.base.api.processing.ProcessingHookProvider;

/**
 *
 * @author PALATEJ
 */
public class BsmOutliersDetector implements IProcessingHookProvider<BsmOutliersDetector.OutlierInfo> {

    @lombok.Value
    public static class OutlierInfo {

        int type;
        int pos;
        double tval;
        BsmData bsm;
        boolean added;
    }

    public static enum Estimation {
        Full, Score, Point;
    }

    @BuilderPattern(BsmOutliersDetector.class)
    public static class Builder {

        private BsmSpec spec;
        private boolean ao = true, ls = true, so = false;
        private double cv = 0;
        private int maxIter = 50;
        private double precision = 1e-5;
        private double fullEstimationThreshold = 5;
        private boolean mad = true;
        private Estimation forwardEstimation = Estimation.Score, backwardEstimation = Estimation.Point;

        public Builder bsm(BsmSpec spec) {
            this.spec = spec;
            return this;
        }

        public Builder ao(boolean ao) {
            this.ao = ao;
            return this;
        }

        public Builder ls(boolean ls) {
            this.ls = ls;
            return this;
        }

        public Builder so(boolean so) {
            this.so = so;
            return this;
        }

        public Builder mad(boolean mad) {
            this.mad = mad;
            return this;
        }

        public Builder criticalValue(double cv) {
            this.cv = cv;
            return this;
        }

        public Builder maxIter(int maxIter) {
            this.maxIter = maxIter;
            return this;
        }

        public Builder forwardEstimation(Estimation method) {
            this.forwardEstimation = method;
            return this;
        }

        public Builder backardEstimation(Estimation method) {
            this.backwardEstimation = method;
            return this;
        }

        public Builder precision(double eps) {
            this.precision = eps;
            return this;
        }

        public Builder fullEstimationThreshold(double ft) {
            this.fullEstimationThreshold = ft;
            return this;
        }

        public BsmOutliersDetector build() {
            return new BsmOutliersDetector(spec, ao, ls, so, cv, mad, maxIter, forwardEstimation, backwardEstimation, precision, fullEstimationThreshold);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @lombok.experimental.Delegate
    private final ProcessingHookProvider<OutlierInfo> hooks = new ProcessingHookProvider<>();

    private final BsmSpec spec;
    private final boolean ao, ls, so;
    private final double cv;
    private final int maxIter;
    private final Estimation forwardEstimation, backwardEstimation;
    private final double eps, eps2, fullEstimationThreshold;
    private final SsfOutlierDetector sod;
    private final List<int[]> outliers = new ArrayList<>();
    private int period;
    private BsmData initialModel, model;
    private BsmSpec curSpec;
    private DiffuseConcentratedLikelihood initialLikelihood, likelihood;
    private DoubleSeq curp;
    private Component curfixed;
    private FastMatrix regressors;
    private boolean full;
    private FastMatrix initialTau;

    private BsmOutliersDetector(BsmSpec spec, boolean ao, boolean ls, boolean so, double cv, boolean mad,
            int maxIter, Estimation forwardEstimation, Estimation backwardEstimation, double eps, double ft) {
        this.spec = spec;
        this.ao = ao;
        this.ls = ls;
        this.so = so;
        this.cv = cv;
        this.maxIter = maxIter;
        this.forwardEstimation = forwardEstimation;
        this.backwardEstimation = backwardEstimation;
        this.eps = eps;
        this.eps2 = Math.sqrt(eps);
        this.fullEstimationThreshold = ft;
        sod = new SsfOutlierDetector(mad ? RobustStandardDeviationComputer.mad() : null);
    }

    public boolean process(DoubleSeq y, FastMatrix X, int period) {
        int n = y.length();
        sod.prepare(n);
        if (!ao) {
            sod.excludeType(0);
        }
        if (!ls) {
            sod.excludeType(1);
        } else {
            sod.exclude(0, 1);
            sod.exclude(n - 1, 1);
        }
        if (!so) {
            sod.excludeType(2);
        } else {
            for (int i = 0, j = n - 1; i < period; ++i, --j) {
                sod.exclude(i, 2);
                sod.exclude(j, 2);
            }
        }
        clear();
        int i = 0;
        this.period = period;
        regressors = x(y.length(), X);
        if (!fullEstimation(y, regressors, period, eps2)) {
            return false;
        }
        initialModel = model;
        initialLikelihood = getLikelihood();
        double cvcur = cv == 0 ? criticalValue(y.length()) : cv;
        double tcur = Math.sqrt(cvcur);
        // forward recursion
        while (i < maxIter) {
            if (!iterate(i++, y, regressors, cvcur)) {
                break;
            }
            regressors = x(y.length(), X);
            if (!estimate(y, regressors, forwardEstimation)) {
                break;
            }
        }
        // backward recursion

        if (!fullEstimation(y, regressors, period, eps)) {
            return false;
        }
        do {
            if (regressors == null) {
                break;
            }
            double[] tstats = getLikelihood().tstats(0, false);
            int nx = X == null ? 0 : X.getColumnsCount();
            if (tstats.length == nx) {
                break;
            }
            int jmin = 0;
            double tmin = Math.abs(tstats[nx]);
            for (int j = nx + 1; j < tstats.length; ++j) {
                if (Math.abs(tstats[j]) < tmin) {
                    tmin = Math.abs(tstats[j]);
                    jmin = j - nx;
                }
            }
            if (tmin > tcur) {
                break;
            }
            outliers.remove(jmin);
            regressors = x(y.length(), X);
            if (!estimate(y, regressors, backwardEstimation)) {
                break;
            }
        } while (!outliers.isEmpty());

        return true;

    }

    private void clear() {
        period = 0;
        outliers.clear();
        model = null;
        likelihood = null;
        initialModel = null;
        initialLikelihood = null;
        regressors = null;
        curp = null;
        curfixed = null;
        full = false;
    }

    private double criticalValue(int n) {
        if (cv != 0) {
            return cv;
        }
        int no = (ao ? 1 : 0) + (ls ? 1 : 0) + (so ? 1 : 0);
        return switch (no) {
            case 3 ->
                defaultCriticalValue3(n);
            case 2 ->
                defaultCriticalValue2(n);
            default ->
                defaultCriticalValue(n);
        };
    }

    private boolean iterate(int round, DoubleSeq y, FastMatrix W, double curcv) {
        full = false;
        if (!sod.process(y, model, W, 0)) {
            return false;
        }
        if (round == 0) {
            initialTau = sod.getTau().deepClone();
        }

        double smax = sod.getMaxTau();
        if (smax < curcv) {
            return false;
        }
        int type = sod.getMaxOutlierType(), imax = sod.getMaxOutlierPosition();

        double gmax = sod.getMaxGlobalTau();
        full = gmax > fullEstimationThreshold * curcv;

        outliers.add(new int[]{imax, type});
        if (hooks.hasHooks()) {
            OutlierInfo info = new OutlierInfo(type, imax, smax, model, true);
            hooks.processHooks(new HookInformation<>(info), false);
        }

        return true;
    }

    private boolean fullEstimation(DoubleSeq y, FastMatrix W, int period, double eps) {
        BsmEstimationSpec espec = BsmEstimationSpec.builder()
                .diffuseRegression(true)
                .precision(eps)
                .build();
        BsmKernel monitor = new BsmKernel(espec);
        monitor.process(y, W, period, spec);
        curp = monitor.maxLikelihoodFunction().getParameters();
        curfixed = monitor.fixedVariance();
        model = monitor.result(true);
        curSpec = monitor.finalSpecification(true);
        likelihood = monitor.getLikelihood();
        return model != null;
    }

    private void pointEstimation(DoubleSeq y, FastMatrix W) {
        SsfFunction<BsmData, SsfBsm2> fn = currentFunction(y, W);
        SsfFunctionPoint<BsmData, SsfBsm2> pt = fn.evaluate(curp);
        likelihood = pt.getLikelihood();
        model = pt.getCore();
    }

    private void scoreEstimation(DoubleSeq y, FastMatrix W) {
        SsfFunction<BsmData, SsfBsm2> fn = currentFunction(y, W);
        SsfFunctionPoint<BsmData, SsfBsm2> pt = fn.evaluate(curp);
        try {
            IFunctionDerivatives D = pt.derivatives();
            FastMatrix H = D.hessian();
            DataBlock G = DataBlock.of(D.gradient());
            LinearSystemSolver.fastSolver().solve(H, G);
            DoubleSeq np = DoublesMath.subtract(curp, G);
            if (fn.getMapping().checkBoundaries(np)) {
                curp = np;
                pt = fn.evaluate(curp);
            }
        } catch (Exception err) {
        }
        likelihood = pt.getLikelihood();
        model = pt.getCore();
    }

    private boolean estimate(DoubleSeq y, FastMatrix W, Estimation method) {
        if (full) {
            return fullEstimation(y, W, model.getPeriod(), eps2);
        }
        try {
            switch (method) {
                case Point -> {
                    pointEstimation(y, W);
                    return true;
                }
                case Score -> {
                    scoreEstimation(y, W);
                    return true;
                }
                default -> {
                    return fullEstimation(y, W, model.getPeriod(), eps2);
                }
            }
        } catch (Exception err) {
            return false;
        }
    }

    private FastMatrix x(int m, FastMatrix X) {
        int nx = X == null ? 0 : X.getColumnsCount();
        int nw = nx + outliers.size();
        if (nw == 0) {
            return null;
        }
        FastMatrix W = FastMatrix.make(m, nw);
        int p = 0;
        if (nx > 0) {
            W.extract(0, m, 0, nx).copy(X);
            p = nx;
        }

        for (int[] o : outliers) {
            DataBlock col = W.column(p++);
            switch (o[1]) {
                case 0 ->
                    col.set(o[0], 1);
                case 1 ->
                    col.drop(o[0], 0).set(1);
                case 2 ->
                    col.drop(o[0], 0).extract(0, -1, period).set(1);
            }
        }
        return W;
    }

    SsfFunction<BsmData, SsfBsm2> currentFunction(DoubleSeq y, FastMatrix W) {
        BsmMapping mapper = new BsmMapping(curSpec == null ? spec : curSpec, model.getPeriod(), curfixed);
        return SsfFunction.builder(new SsfData(y), mapper, bsmmodel -> SsfBsm2.of(bsmmodel))
                .regression(W, W != null ? W.getColumnsCount() : 0)
                .useFastAlgorithm(true)
                .useParallelProcessing(false)
                .useLog(true)
                .useScalingFactor(true)
                .build();
    }

    /**
     * @return the aoPositions
     */
    public List<int[]> outliers() {
        return Collections.unmodifiableList(outliers);
    }

    /**
     * @return the model
     */
    public BsmData getModel() {
        return model;
    }

    public FastMatrix initialTau(){
        return initialTau;
    }
    
    public FastMatrix finalTau(){
        FastMatrix finalTau=sod.getTau();
        finalTau.apply((x-> Double.isNaN(x) ? 0 : x));
        return finalTau;
    }
    
    /**
     * @return the initialModel
     */
    public BsmData getInitialModel() {
        return initialModel;
    }

    /**
     * @return the initialLikelihood
     */
    public DiffuseConcentratedLikelihood getInitialLikelihood() {
        return initialLikelihood;
    }

    /**
     * @return the likelihood
     */
    public DiffuseConcentratedLikelihood getLikelihood() {
        return likelihood;
    }

    /**
     * @return the regressors
     */
    public FastMatrix getRegressors() {
        return regressors;
    }

    public static double defaultCriticalValue(int n) {
        return 1 / (0.06506323 + 2.19443019 / n - 17.48793935 / (n * n));
    }

    public static double defaultCriticalValue2(int n) {
        return 1 / (0.05508697 + 1.72286327 / n - 13.62470737 / (n * n));
    }

    public static double defaultCriticalValue3(int n) {
        return 1 / (0.04400659 + 1.60844248 / n - 23.64272642 / (n * n));
    }
}
