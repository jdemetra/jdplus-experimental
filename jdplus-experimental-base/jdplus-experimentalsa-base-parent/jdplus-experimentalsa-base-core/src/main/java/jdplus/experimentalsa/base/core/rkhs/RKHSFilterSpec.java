/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdplus.experimentalsa.base.core.rkhs;

import jdplus.experimentalsa.base.core.filters.AsymmetricCriterion;
import jdplus.experimentalsa.base.core.filters.KernelOption;
import jdplus.experimentalsa.base.core.filters.SpectralDensity;

/**
 *
 * @author Jean Palate
 */
@lombok.Data
public class RKHSFilterSpec {
    
    private int filterLength=6;
    private KernelOption kernel=KernelOption.BiWeight;
    private int polynomialDegree=2;
    private boolean optimalBandWidth=true;
    private AsymmetricCriterion asymmetricBandWith=AsymmetricCriterion.FrequencyResponse;
    private SpectralDensity density=SpectralDensity.Undefined;
    private double passBand=Math.PI/8;
    private double bandWidth=filterLength+1;
    private double minBandWidth=filterLength;
    private double maxBandWidth=3*filterLength;
}
