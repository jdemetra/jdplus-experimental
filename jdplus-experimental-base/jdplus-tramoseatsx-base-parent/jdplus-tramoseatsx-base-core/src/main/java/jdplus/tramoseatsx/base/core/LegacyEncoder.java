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

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import jdplus.sa.base.api.ComponentType;
import jdplus.sa.base.api.SaVariable;
import jdplus.toolkit.base.api.data.DoubleSeq;
import jdplus.toolkit.base.api.data.Range;
import jdplus.toolkit.base.api.timeseries.TimeSelector.SelectionType;
import jdplus.toolkit.base.api.timeseries.TsData;
import jdplus.toolkit.base.api.timeseries.TsDataSupplier;
import jdplus.toolkit.base.api.timeseries.TsDomain;
import jdplus.toolkit.base.api.timeseries.TsObs;
import jdplus.toolkit.base.api.timeseries.TsPeriod;
import jdplus.toolkit.base.api.timeseries.TsUnit;
import jdplus.toolkit.base.api.timeseries.calendars.CalendarDefinition;
import jdplus.toolkit.base.api.timeseries.calendars.DayClustering;
import jdplus.toolkit.base.api.timeseries.calendars.LengthOfPeriodType;
import jdplus.toolkit.base.api.timeseries.regression.HolidaysCorrectedTradingDays;
import jdplus.toolkit.base.api.timeseries.regression.IOutlier;
import jdplus.toolkit.base.api.timeseries.regression.ITsVariable;
import jdplus.toolkit.base.api.timeseries.regression.InterventionVariable;
import jdplus.toolkit.base.api.timeseries.regression.LengthOfPeriod;
import jdplus.toolkit.base.api.timeseries.regression.ModellingContext;
import jdplus.toolkit.base.api.timeseries.regression.Ramp;
import jdplus.toolkit.base.api.timeseries.regression.TsContextVariable;
import jdplus.toolkit.base.api.timeseries.regression.Variable;
import jdplus.toolkit.base.core.data.DataBlock;
import jdplus.toolkit.base.core.data.DataBlockIterator;
import jdplus.toolkit.base.core.math.matrices.FastMatrix;
import jdplus.toolkit.base.core.modelling.regression.HolidaysCorrectionFactory;
import jdplus.toolkit.base.core.modelling.regression.Regression;
import jdplus.tramoseats.base.api.tramo.OutlierSpec;
import jdplus.tramoseats.base.api.tramo.TradingDaysSpec;
import jdplus.tramoseats.base.api.tramo.TransformSpec;
import jdplus.tramoseats.base.api.tramoseats.TramoSeatsSpec;
import jdplus.tramoseatsx.base.core.Decoder.Document;

/**
 *
 * @author Jean Palate
 */
public class LegacyEncoder extends AbstractEncoder {

    public static final double MISSING = -99999;
    public static final String INPUT = "$INPUT", REG = "$REG", INPUT_ = "INPUT", REG_ = "REG";
    private static final char sep = ' ';
    protected boolean m_closed;
    private boolean batch;
    private final ModellingContext context;

    private TsDomain domain;

    public LegacyEncoder(ModellingContext context) {
        this.domain = null;
        if (context == null) {
            this.context = ModellingContext.getActiveContext();
        } else {
            this.context = context;
        }
    }

    public LegacyEncoder(final TsDomain domain, final ModellingContext context) {
        this.domain = domain;
        if (context == null) {
            this.context = ModellingContext.getActiveContext();
        } else {
            this.context = context;
        }
    }

    public String encode(String name, TsData s, TramoSeatsSpec spec) {
        domain = s.getDomain();
        batch = false;
        return encode(name, s) + encode(spec);
    }

    public String encode(String name, TsData s) {
        StringBuilder builder = new StringBuilder();
        builder.append(name == null ? "Series" : name).append(NL);
        builder.append(s.length()).append(sep).append(s.getStart().year())
                .append(sep).append(s.getStart().annualPosition() + 1).append(sep)
                .append(s.getAnnualFrequency()).append(NL);
        DoubleSeq values = s.getValues();
        for (int i = 0; i < values.length(); ++i) {
            double x = values.get(i);
            if (Double.isNaN(x)) {
                builder.append(MISSING);
            } else {
                builder.append(x);
            }
            builder.append(NL);
        }
        return builder.toString();
    }

    @Override
    protected void openDocument() {
        m_closed = false;
        super.openDocument();
    }

    @Override
    protected void openSpecSection() {
        m_closed = false;
        m_builder.append(INPUT);
        if (batch) {
            write(Item.ITER, 3);
            batch = false;
        }
    }

    protected void openFreeSection() {
        m_closed = false;
    }

    @Override
    protected void openRegSection() {
        if (!m_closed) {
            m_builder.append("$END");
        }
        m_closed = false;
        m_builder.append(REG);
    }

    @Override
    protected void closeSection() {
        if (!m_closed) {
            m_builder.append("$END");
        }
        m_builder.append(NL);
        m_closed = true;
    }

    @Override
    protected void addSeparator() {
        m_builder.append(sep);
    }

    @Override
    protected void writeCalendarRegs(TradingDaysSpec spec) {
        if (spec.getHolidays() != null) {
            CalendarDefinition cal = context.getCalendars().get(spec.getHolidays());
            if (cal != null) {
                int ntd = spec.getTradingDaysType().getVariablesCount();
                DayClustering dc = ntd == 6 ? DayClustering.TD7 : DayClustering.TD2;
                HolidaysCorrectedTradingDays td = HolidaysCorrectedTradingDays
                        .builder()
                        .clustering(dc)
                        .contrast(true)
                        .build();
                boolean lp = spec.getLengthOfPeriodType() != LengthOfPeriodType.None;
                TsDomain xdom = domain.extend(0, 3 * domain.getAnnualFrequency());
                LengthOfPeriod vlp = lp ? new LengthOfPeriod(spec.getLengthOfPeriodType()) : null;
                FastMatrix M;
                if (lp) {
                    M = Regression.matrix(xdom, td, vlp);
                } else {
                    M = Regression.matrix(xdom, td);
                }
                for (int i = 0; i < M.getColumnsCount(); ++i) {
                    openRegSection();
                    write(Item.IUSER, 1);
                    write(Item.ILONG, xdom.getLength());
                    write(Item.NSER, 1);
                    write(Item.REGEFF, 2);
                    closeSection();
                    openFreeSection();
                    write(M.column(i));
                    closeSection();
                }
            }
        } else if (spec.getUserVariables() != null) {
            String[] vars = spec.getUserVariables();
            TsDomain xdom = domain.extend(0, 3 * domain.getAnnualFrequency());

            for (int i = 0; i < vars.length; ++i) {
                TsDataSupplier var = context.getTsVariable(vars[i]);
                TsData s = var.get();
                s = TsData.fitToDomain(s, xdom);
                openRegSection();
                write(Item.IUSER, 1);
                write(Item.ILONG, xdom.getLength());
                write(Item.NSER, 1);
                write(Item.REGEFF, 2);
                closeSection();
                openFreeSection();
                write(s.getValues());
                closeSection();
            }
        }
    }

    @Override
    protected void writeInterventionRegs(List<Variable<InterventionVariable>> spec) {
        if (spec.isEmpty()) {
            return;
        }
        for (Variable<InterventionVariable> cur : spec) {
            openRegSection();
            InterventionVariable core = cur.getCore();
            List<Range<LocalDateTime>> sequences = core.getSequences();
            int nseq = sequences.size();
            write(Item.ISEQ, nseq);
            double d = core.getDelta();
            if (d != 0) {
                write(Item.DELTA, d);
            }
            double ds = core.getDeltaSeasonal();
            if (ds != 0) {
                write(Item.DELTAS, ds);
            }
            if (core.getDelta() == 1 && core.getDeltaSeasonal() == 1) {
                write(Item.ID1DS, 1);
            }
            closeSection();
            openFreeSection();
            for (Range<LocalDateTime> seq : sequences) {
                int start = domain.indexOf(seq.start()), end = domain.indexOf(seq.end());
                write(start + 1);
                write(end - start + 1);
            }
            closeSection();
        }
    }

    @Override
    protected void writeUserRegs(List<Variable<TsContextVariable>> spec) {
        if (spec.isEmpty()) {
            return;
        }
        TsDomain xdom = domain.extend(0, 3 * domain.getAnnualFrequency());
        for (Variable<TsContextVariable> cur: spec) {
            ITsVariable var = cur.getCore().instantiateFrom(context, null);
            FastMatrix M = Regression.matrix(xdom, var);
            DataBlockIterator cols = M.columnsIterator();
            for (int j = 0; j < M.getColumnsCount(); ++j) {
                openRegSection();
                write(Item.IUSER, 1);
                write(Item.NSER, 1);
                write(Item.ILONG, xdom.getLength());
                write(Item.REGEFF, convert(SaVariable.regressionEffect(cur)));
                closeSection();
                openFreeSection();
                write(cols.next());
                closeSection();
            }
        }
    }

    @Override
    protected void writeOutlierRegs(List<Variable<IOutlier>> spec) {
        if (spec.isEmpty() || domain == null) {
            return;
        }
        openRegSection();
        write(Item.IUSER, 2);
        write(Item.NSER, spec.size());
        closeSection();
        openFreeSection();
        int freq = domain.getAnnualFrequency();
        TsUnit unit = TsUnit.ofAnnualFrequency(freq);
        TsPeriod start = domain.getStartPeriod();
        for (Variable<IOutlier> cur : spec) {
            LocalDateTime pos = cur.getCore().getPosition();
            TsPeriod p = TsPeriod.of(unit, pos);
            m_builder.append(sep).append(start.until(p) + 1).
                    append(sep).append(cur.getCore().getCode());
        }
        closeSection();
    }

    @Override
    protected void writeRampRegs(List<Variable<Ramp>> spec) {
        //TODO
    }

    @Override
    protected void writeEstimateSpan(TransformSpec spec) {
        // TODO
    }

    @Override
    protected void writeOutliersSpan(OutlierSpec spec) {
        if (domain != null && spec.getSpan().getType() != SelectionType.All) {
            TsDomain ndom = domain.select(spec.getSpan());
            if (ndom.getLength() == 0) {
                write(Item.INT1, domain.getLength() + 1);
            } else {
                write(Item.INT1, 1 + domain.getStartPeriod().until(ndom.getStartPeriod()));
                write(Item.INT1, domain.getStartPeriod().until(ndom.getEndPeriod()));
            }
        }
    }

    public void encodeMultiDocument(FileWriter writer, List<Document> docs) {
        batch = true;
        for (Document doc : docs) {
            domain = doc.series.getDomain();
            String ts = encode(doc.name, doc.series);
            String spec = encode(doc.spec);
            try {
                writer.append(ts);
                writer.append(spec);
            } catch (IOException ex) {

            }
        }
    }

    static int convert(ComponentType cmp) {
        return switch (cmp) {
            case Trend -> 1;
            case Seasonal -> 2;
            case Irregular -> 3;
            default -> 0;
        };
    }

}
