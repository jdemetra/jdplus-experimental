package jdplus.experimentalsa.base.r;

import jdplus.experimentalsa.base.core.filters.ISymmetricFiltering;
import jdplus.experimentalsa.base.core.filters.SpectralDensity;
import jdplus.toolkit.base.core.math.linearfilters.SymmetricFilter;
import jdplus.experimentalsa.base.core.dfa.DFAFilterFactory;
import jdplus.experimentalsa.base.core.dfa.DFAFilterSpec;

@lombok.experimental.UtilityClass
public class DFAFilters {
	public FiltersToolkit.FiniteFilters filterProperties(double[] target,
			int nlags, int pdegree, boolean rwdensity, double passband, 
			double waccuracy, double wsmoothness, double wtimeliness) {
        // Creates the filters
    	DFAFilterSpec tspec=new DFAFilterSpec();
    	tspec.setW0(0);
    	tspec.setW1(passband);
    	tspec.setAccuracyWeight(passband);
    	tspec.setPolynomialPreservationDegree(pdegree);
        tspec.setLags(nlags);
        tspec.setTarget(SymmetricFilter.ofInternal(target));
        tspec.setAccuracyWeight(waccuracy);
        tspec.setSmoothnessWeight(wsmoothness);
        tspec.setTimelinessWeight(wtimeliness);
        tspec.setDensity(rwdensity ? SpectralDensity.RandomWalk : SpectralDensity.Undefined);

        ISymmetricFiltering dfafilter= DFAFilterFactory.of(tspec);
       
        return new FiltersToolkit.FiniteFilters(dfafilter.symmetricFilter(),
        		dfafilter.endPointsFilters());
    }
}
