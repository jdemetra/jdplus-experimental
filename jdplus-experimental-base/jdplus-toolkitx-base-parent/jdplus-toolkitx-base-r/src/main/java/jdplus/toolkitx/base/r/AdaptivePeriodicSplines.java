/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jdplus.toolkitx.base.r;

import java.util.LinkedHashMap;
import java.util.Map;
import jdplus.toolkit.base.api.data.DoubleSeq;
import jdplus.toolkit.base.api.information.GenericExplorable;
import jdplus.toolkit.base.api.information.InformationMapping;
import jdplus.toolkit.base.api.math.matrices.Matrix;
import jdplus.toolkit.base.core.math.splines.AdaptivePeriodicSpline;

/**
 *
 * @author Jean Palate
 */
@lombok.experimental.UtilityClass
public class AdaptivePeriodicSplines {
    
    public static class Iteration implements GenericExplorable {
        
        private final AdaptivePeriodicSpline.Step iteration;
        
        Iteration(final AdaptivePeriodicSpline.Step step) {
            this.iteration = step;
        }
        
        private static final InformationMapping<AdaptivePeriodicSpline.Step> MAPPING = new InformationMapping<>() {
            @Override
            public Class getSourceClass() {
                return AdaptivePeriodicSpline.Step.class;
            }
        };
        
        @Override
        public boolean contains(String id) {
            return MAPPING.contains(id);
        }
        
        @Override
        public Map<String, Class> getDictionary() {
            Map<String, Class> dic = new LinkedHashMap<>();
            MAPPING.fillDictionary(null, dic, true);
            return dic;
        }
        
        @Override
        public <T> T getData(String id, Class<T> tclass) {
            return MAPPING.getData(iteration, id, tclass);
        }
        
        public static final InformationMapping<AdaptivePeriodicSpline.Step> getMapping() {
            return MAPPING;
        }
        
        static {
            MAPPING.set("lambda", Double.class, source -> source.getLambda());
            MAPPING.set("ll", Double.class, source -> source.getLl());
            MAPPING.set("aic", Double.class, source -> source.getAic());
            MAPPING.set("bic", Double.class, source -> source.getBic());
            MAPPING.set("a", double[].class, source -> source.getA());
            MAPPING.set("z", double[].class, source -> source.getZ());
            MAPPING.set("w", double[].class, source -> source.getW());
            MAPPING.set("s", double[].class, source -> source.getS());
        }
    }
    
    public static class AdaptivePSpline implements GenericExplorable {
        
        private final AdaptivePeriodicSpline aspline;
        
        AdaptivePSpline(final AdaptivePeriodicSpline aspline) {
            this.aspline = aspline;
        }
        
        private static final InformationMapping<AdaptivePeriodicSpline> MAPPING = new InformationMapping<>() {
            @Override
            public Class getSourceClass() {
                return AdaptivePeriodicSpline.class;
            }
        };
        
        @Override
        public boolean contains(String id) {
            return MAPPING.contains(id);
        }
        
        @Override
        public Map<String, Class> getDictionary() {
            Map<String, Class> dic = new LinkedHashMap<>();
            MAPPING.fillDictionary(null, dic, true);
            return dic;
        }
        
        @Override
        public <T> T getData(String id, Class<T> tclass) {
            return MAPPING.getData(aspline, id, tclass);
        }
        
        public static final InformationMapping<AdaptivePeriodicSpline> getMapping() {
            return MAPPING;
        }
        
        static {
            MAPPING.set("size", Integer.class, source -> source.getiterationCount());
            MAPPING.set("A", Matrix.class, source -> source.A());
            MAPPING.set("Z", Matrix.class, source -> source.Z());
            MAPPING.set("W", Matrix.class, source -> source.W());
            MAPPING.set("S", Matrix.class, source -> source.S());
            MAPPING.set("selection", int[].class, source -> source.selectedKnotsPosition(source.getiterationCount() - 1));
            MAPPING.set("selectedKnots", double[].class, source -> source.selectedKnots(source.getiterationCount() - 1));
            MAPPING.delegateArray("item", 0, 20, Iteration.class, (source, i) -> new Iteration(source.step(i)));
        }
    }
    
    public static class AdaptivePSplines implements GenericExplorable {
        
        private final jdplus.toolkit.base.core.math.splines.AdaptivePeriodicSplines splines;
        
        AdaptivePSplines(final jdplus.toolkit.base.core.math.splines.AdaptivePeriodicSplines splines) {
            this.splines = splines;
        }
        
        private static final InformationMapping<jdplus.toolkit.base.core.math.splines.AdaptivePeriodicSplines> MAPPING = new InformationMapping<>() {
            @Override
            public Class getSourceClass() {
                return AdaptivePeriodicSplines.class;
            }
        };
        
        @Override
        public boolean contains(String id) {
            return MAPPING.contains(id);
        }
        
        @Override
        public Map<String, Class> getDictionary() {
            Map<String, Class> dic = new LinkedHashMap<>();
            MAPPING.fillDictionary(null, dic, true);
            return dic;
        }
        
        @Override
        public <T> T getData(String id, Class<T> tclass) {
            return MAPPING.getData(splines, id, tclass);
        }
        
        public static final InformationMapping<jdplus.toolkit.base.core.math.splines.AdaptivePeriodicSplines> getMapping() {
            return MAPPING;
        }
        
        static {
            MAPPING.set("size", Integer.class, source -> source.resultsSize());
            MAPPING.delegateArray("result", 0, Integer.MAX_VALUE, Iteration.class, (source, i) -> new Iteration(source.result(i)));
            MAPPING.set("best", Integer.class, source -> source.best());
            MAPPING.set("selection", int[].class, source -> source.selectedKnotsPosition());
            MAPPING.set("selectedKnots", double[].class, source -> source.selectedKnots());
            MAPPING.set("A", Matrix.class, source -> source.A());
            MAPPING.set("Z", Matrix.class, source -> source.Z());
            MAPPING.set("W", Matrix.class, source -> source.W());
            MAPPING.set("S", Matrix.class, source -> source.S());
        }
    }
    
    public AdaptivePSpline aspline(double[] x, double[] y, double[] knots, double period, int order, double lambda, int maxiter, double precision, double threshold) {
        AdaptivePeriodicSpline.Specification spec = AdaptivePeriodicSpline.Specification.builder()
                .x(DoubleSeq.of(x))
                .y(DoubleSeq.of(y))
                .knots(knots)
                .period(period)
                .splineOrder(order)
                .precision(precision)
                .selectionThreshold(threshold)
                .maxIter(maxiter)
                .build();
        
        AdaptivePeriodicSpline aspline = AdaptivePeriodicSpline.of(spec);
        aspline.process(lambda);
        return new AdaptivePSpline(aspline);
    }
    
    public AdaptivePSplines asplines(double[] x, double[] y, double[] knots, double period, int order, int maxiter, double precision, double threshold,
            double lambda0, double lambda1, double dlambda, int minKnots, String criterion) {
        AdaptivePeriodicSpline.Specification spec = AdaptivePeriodicSpline.Specification.builder()
                .x(DoubleSeq.of(x))
                .y(DoubleSeq.of(y))
                .knots(knots)
                .period(period)
                .splineOrder(order)
                .precision(precision)
                .selectionThreshold(threshold)
                .maxIter(maxiter)
                .build();
        jdplus.toolkit.base.core.math.splines.AdaptivePeriodicSplines.Specification mspec
                = jdplus.toolkit.base.core.math.splines.AdaptivePeriodicSplines.Specification.builder()
                        .lambda0(lambda0)
                        .lambda1(lambda1)
                        .lambdaStep(dlambda)
                        .minKnots(minKnots)
                        .criterion(jdplus.toolkit.base.core.math.splines.AdaptivePeriodicSplines.Criterion.valueOf(criterion))
                        .build();
        
        AdaptivePeriodicSpline aspline = AdaptivePeriodicSpline.of(spec);
        jdplus.toolkit.base.core.math.splines.AdaptivePeriodicSplines splines=
                new jdplus.toolkit.base.core.math.splines.AdaptivePeriodicSplines(mspec);
        splines.process(aspline);
        return new AdaptivePSplines(splines);
    }
}
