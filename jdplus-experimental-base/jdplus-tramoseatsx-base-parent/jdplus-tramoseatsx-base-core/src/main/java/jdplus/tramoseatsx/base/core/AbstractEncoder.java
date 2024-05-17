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
package jdplus.tramoseatsx.base.core;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import jdplus.toolkit.base.api.arima.SarimaSpec;
import jdplus.toolkit.base.api.data.DoubleSeq;
import jdplus.toolkit.base.api.data.Parameter;
import jdplus.toolkit.base.api.math.matrices.Matrix;
import jdplus.toolkit.base.api.modelling.TransformationType;
import jdplus.toolkit.base.api.timeseries.TsPeriod;
import jdplus.toolkit.base.api.timeseries.calendars.LengthOfPeriodType;
import jdplus.toolkit.base.api.timeseries.regression.IOutlier;
import jdplus.toolkit.base.api.timeseries.regression.InterventionVariable;
import jdplus.toolkit.base.api.timeseries.regression.Ramp;
import jdplus.toolkit.base.api.timeseries.regression.TsContextVariable;
import jdplus.tramoseats.base.api.seats.DecompositionSpec;
import jdplus.tramoseats.base.api.seats.SeatsSpec;
import jdplus.tramoseats.base.api.tramo.AutoModelSpec;
import jdplus.tramoseats.base.api.tramo.CalendarSpec;
import jdplus.tramoseats.base.api.tramo.EasterSpec;
import jdplus.tramoseats.base.api.tramo.EstimateSpec;
import jdplus.tramoseats.base.api.tramo.OutlierSpec;
import jdplus.tramoseats.base.api.tramo.RegressionSpec;
import jdplus.tramoseats.base.api.tramo.TradingDaysSpec;
import jdplus.tramoseats.base.api.tramo.TransformSpec;
import jdplus.tramoseats.base.api.tramoseats.TramoSeatsSpec;

/**
 *
 * @author Jean Palate
 */
public abstract class AbstractEncoder implements Encoder {

    protected final DecimalFormat dfmt;
    protected static final String I2 = "%02d", NL = "\r\n";
    protected StringBuilder m_builder;

    protected abstract void openSpecSection();

    protected abstract void openRegSection();

    protected abstract void closeSection();

    protected abstract void addSeparator();

    protected abstract void writeCalendarRegs(TradingDaysSpec spec);

    protected abstract void writeOutlierRegs(IOutlier[] spec);

    protected abstract void writeRampRegs(Ramp[] spec);

    protected abstract void writeInterventionRegs(InterventionVariable[] spec);

    protected abstract void writeUserRegs(TsContextVariable[] spec);

    protected abstract void writeEstimateSpan(TransformSpec spec);

    protected abstract void writeOutliersSpan(OutlierSpec spec);

    protected void openDocument() {
        m_builder = new StringBuilder();
        openSpecSection();
    }

    protected AbstractEncoder() {
        dfmt = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ROOT);
        dfmt.setMaximumFractionDigits(9);
    }

    public DecimalFormat getNumberFormat() {
        return dfmt;
    }

    @Override
    public String encode(TramoSeatsSpec spec) {
        openDocument();
        write(Item.SEATS, 2);
        write(spec.getTramo().getTransform());
        write(spec.getTramo().getArima());
        write(spec.getTramo().getAutoModel());
        write(spec.getTramo().getRegression());
        write(spec.getTramo().getOutliers());
        writeMissingSpec();
        write(spec.getTramo().getEstimate());
        write(spec.getSeats());
        int nregs = regsCount(spec.getTramo().getRegression());
        if (nregs > 0) {
            write(Item.IREG, nregs);
            closeSection();
            writeRegs(spec.getTramo().getRegression());
        } else {
            closeSection();
        }
        return m_builder.toString();
    }

    private int regsCount(RegressionSpec spec) {
        if (spec == null) {
            return 0;
        }
        int n = 0;
        n += spec.getRamps().size();
        n += spec.getOutliers().size();
        n += spec.getUserDefinedVariables().size();

        n += tdUserCount(spec.getCalendar().getTradingDays());
        return n;
    }

    private int tdUserCount(TradingDaysSpec spec) {
        if (!spec.isUsed()) {
            return 0;
        }
        if (spec.getHolidays() != null) {
            return spec.getTradingDaysType().getVariablesCount() + (spec.getLengthOfPeriodType() != LengthOfPeriodType.None ? 1 : 0);
        } else if (spec.getUserVariables() != null) {
            return spec.getUserVariables().length;
        } else {
            return 0;
        }
    }

    protected void write(int val) {
        m_builder.append(dfmt.format(val));
        addSeparator();
    }

    protected void write(double val) {
        m_builder.append(val);
        addSeparator();
    }

    protected void write(DoubleSeq data) {
        for (int i = 0; i < data.length(); ++i) {
            write(data.get(i));
        }
    }

    protected void write(Matrix m) {
        for (int i = 0; i < m.getRowsCount(); ++i) {
            write(m.row(i));
            m_builder.append(NL);
        }
    }

    protected void write(Item item, int value) {
        addSeparator();
        m_builder.append(item).append('=').append(value);
    }

    protected void write(Item item, int idx, int value) {
        addSeparator();
        m_builder.append(item.name()).append('(').append(idx).append(")=").append(value);
    }

    protected void write(Item item, int idx, double value) {
        addSeparator();
        m_builder.append(item).append('(').append(idx).append(")=").append(dfmt.format(value));
    }

    protected void write(Item item, double value) {
        addSeparator();
        m_builder.append(item).append('=').append(dfmt.format(value));
    }

    protected void write(Item item, TsPeriod p) {
        addSeparator();
        m_builder.append(item).append('=').append(p.year()).append('.').append(String.format(I2, p.annualPosition() + 1));
    }

    protected void write(Item item, int idx, TsPeriod p) {
        addSeparator();
        m_builder.append(item).append('(').append(idx).append(")=").append(p.year()).append('.').append(String.format(I2, p.annualPosition() + 1));
    }

    protected void write(DecompositionSpec spec) {
        if (spec == null) {
            return;
        }
        if (spec.getApproximationMode() != DecompositionSpec.ModelApproximationMode.None) {
            write(Item.NOADMISS, 1);
        }
        if (spec.getTrendBoundary() != DecompositionSpec.DEF_RMOD) {
            write(Item.RMOD, spec.getTrendBoundary());
        }
        if (spec.getSeasTolerance() != DecompositionSpec.DEF_EPSPHI) {
            write(Item.EPSPHI, spec.getSeasTolerance());
        }
        if (spec.getXlBoundary() != DecompositionSpec.DEF_XL) {
            write(Item.XL, spec.getXlBoundary());
        }
    }

    protected void write(TransformSpec spec) {
        if (spec == null) {
            return;
        }
        write(Item.LAM, convert(spec.getFunction()));
        if (spec.getFct() != TransformSpec.DEF_FCT) {
            write(Item.FCT, spec.getFct());
        }
    }

    private int convert(TransformationType function) {
        return switch (function) {
            case None ->
                1;
            case Log ->
                0;
            default ->
                -1;
        };
    }

    protected void write(EstimateSpec spec) {
        if (spec == null) {
            return;
        }
        //if (spec.getTol() != EstimateSpec.DEF_TOL) {
        write(Item.TOL, spec.getTol());
        //}
        if (spec.getUbp() != EstimateSpec.DEF_UBP) {
            write(Item.UBP, spec.getUbp());
        }
    }

    protected void writeMissingSpec() {
        write(Item.INTERP, 2);
    }

    protected void write(OutlierSpec spec) {
        if (spec == null || !spec.isUsed()) {
            return;
        }
        write(Item.IATIP, 1);
        write(Item.AIO, Utility.aio(spec));
        if (spec.getCriticalValue() != 0) {
            write(Item.VA, spec.getCriticalValue());
        }
        if (spec.isMaximumLikelihood()) {
            write(Item.IMVX, 1);
        }
        writeOutliersSpan(spec);
    }

    protected void writeRegs(RegressionSpec spec) {
        if (spec == null) {
            return;
        }
        writeCalendarRegs(spec.getCalendar().getTradingDays());
        writeOutlierRegs(spec.getOutliers().stream().map(v -> v.getCore()).toArray(IOutlier[]::new));
        writeRampRegs(spec.getRamps().stream().map(v -> v.getCore()).toArray(Ramp[]::new));
        writeInterventionRegs(spec.getInterventionVariables().stream().map(v -> v.getCore()).toArray(InterventionVariable[]::new));
        writeUserRegs(spec.getUserDefinedVariables().stream().map(v -> v.getCore()).toArray(TsContextVariable[]::new));
    }

    protected void write(RegressionSpec spec) {
        if (spec == null) {
            return;
        }
        write(Item.IMEAN, spec.getMean().isDefined() ? 1 : 0);
        write(spec.getCalendar());
    }

    protected void write(CalendarSpec spec) {
        if (spec == null) {
            return;
        }
        if (spec.getTradingDays().isAutomatic()) {
            write(Item.ITRAD, -2);
            write(Item.pFTD, 1 - spec.getTradingDays().getProbabilityForFTest());
        } else if (spec.getTradingDays().getHolidays() == null && spec.getTradingDays().getUserVariables() == null) {
            int itrad = spec.getTradingDays().getTradingDaysType().getVariablesCount();
            if (spec.getTradingDays().getLengthOfPeriodType() != LengthOfPeriodType.None) {
                ++itrad;
            }
            if (spec.getTradingDays().isTest()) {
                itrad = -itrad;
            }
            if (itrad != 0) {
                write(Item.ITRAD, itrad);
            }
        }
        int ieast = spec.getEaster().getType() == EasterSpec.Type.Unused ? 0 : 1;
        if (spec.getEaster().isTest()) {
            ieast = -ieast;
        }
        if (ieast != 0) {
            write(Item.IEAST, ieast);
        }
        int idur = spec.getEaster().getDuration();
        if (idur != EasterSpec.DEF_IDUR) {
            write(Item.IDUR, idur);
        }
    }

    protected void write(AutoModelSpec spec) {
        if (!spec.isEnabled()) {
            return;
        }
        write(Item.INIC, 3);
        write(Item.IDIF, 3);
        if (spec.getCancel() != AutoModelSpec.DEF_CANCEL) {
            write(Item.CANCEL, spec.getCancel());
        }
        if (spec.getUb1() != AutoModelSpec.DEF_UB1) {
            write(Item.UB1, spec.getUb1());
        }
        if (spec.getUb2() != AutoModelSpec.DEF_UB2) {
            write(Item.UB2, spec.getUb2());
        }
        if (spec.getTsig() != AutoModelSpec.DEF_TSIG) {
            write(Item.TSIG, spec.getTsig());
        }
        if (spec.getPc() != AutoModelSpec.DEF_PC) {
            write(Item.PC, spec.getPc());
        }
        if (spec.getPcr() != AutoModelSpec.DEF_PCR) {
            write(Item.PCR, spec.getPcr());
        }
    }

    private void write(SarimaSpec spec) {
        if (spec.isAirline() && spec.isUndefined()) {
            return;
        }
        write(Item.P, spec.getP());
        write(Item.D, spec.getD());
        write(Item.Q, spec.getQ());
        write(Item.BP, spec.getBp());
        write(Item.BD, spec.getBd());
        write(Item.BQ, spec.getBq());
        if (spec.isDefined()) {
            write(Item.INIT, 2);
            write(Item.PHI, spec.getPhi());
            write(Item.BPHI, spec.getBphi());
            write(Item.TH, spec.getTheta());
            write(Item.BTH, spec.getBtheta());
        } else if (spec.hasFixedParameters()) {
            write(Item.PHI, Item.JPR, spec.getPhi());
            write(Item.BPHI, Item.JPS, spec.getBphi());
            write(Item.TH, Item.JQR, spec.getTheta());
            write(Item.BTH, Item.JQS, spec.getBtheta());

        }
    }

    private void write(Item p, Parameter[] parameter) {
        if (Parameter.isDefault(parameter)) {
            return;
        }
        for (int i = 0; i < parameter.length; ++i) {
            write(p, i + 1, parameter[i].getValue());
        }
    }

    private void write(Item p, Item f, Parameter[] parameter) {
        if (Parameter.isDefault(parameter)) {
            return;
        }
        for (int i = 0; i < parameter.length; ++i) {
            if (Parameter.isDefined(parameter[i])) {
                write(p, i + 1, parameter[i].getValue());
                if (parameter[i].isFixed()) {
                    write(f, i + 1, 1);
                }
            }
        }
    }

}
