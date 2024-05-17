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
import jdplus.toolkit.base.api.data.DoubleSeq;
import jdplus.toolkit.base.api.data.Range;
import jdplus.toolkit.base.api.timeseries.TsData;
import jdplus.toolkit.base.api.timeseries.TsDomain;
import jdplus.toolkit.base.api.timeseries.TsObs;
import jdplus.toolkit.base.api.timeseries.regression.InterventionVariable;
import jdplus.toolkit.base.api.timeseries.regression.ModellingContext;
import jdplus.toolkit.base.api.timeseries.regression.Ramp;
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
            IGregorianCalendarProvider cal = context.getGregorianCalendars().get(spec.getHolidays());
            if (cal != null) {
                int ntd = spec.getTradingDaysType().getVariablesCount();
                if (spec.isLeapYear()) {
                    ++ntd;
                }
                TsDomain xdom = domain.extend(0, 3 * domain.getFrequency().intValue());
                Matrix M = new Matrix(xdom.getLength(), ntd);
                List<DataBlock> cols = M.columnList();
                cal.calendarData(spec.getTradingDaysType(), xdom, cols, 0);
                if (spec.isLeapYear()) {
                    new LeapYearVariable(LengthOfPeriodType.LeapYear).data(domain.getStart(), cols.get(ntd - 1));
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
                ITsVariable var = context.getTsVariable(vars[i]);
                if (var.getDim() == 1) {
                    DataBlock data = new DataBlock(xdom.getLength());
                    var.data(xdom, Collections.singletonList(data));
                    openRegSection();
                    write(Item.IUSER, 1);
                    write(Item.ILONG, xdom.getLength());
                    write(Item.NSER, 1);
                    write(Item.REGEFF, 2);
                    closeSection();
                    openFreeSection();
                    write(data);
                    closeSection();
                }
            }
        }
    }

    @Override
    protected void writeInterventionRegs(InterventionVariable[] spec) {
        if (spec == null) {
            return;
        }
        for (int i = 0; i < spec.length; ++i) {
            openRegSection();
            List<Range<LocalDateTime>> sequences = spec[i].getSequences();
            int nseq = sequences.size();
            write(Item.ISEQ, nseq);
            double d = spec[i].getDelta();
            if (d != 0) {
                write(Item.DELTA, d);
            }
            double ds = spec[i].getDeltaSeasonal();
            if (ds != 0) {
                write(Item.DELTAS, ds);
            }
            if (spec[i].getDelta() == 1 && spec[i].getDeltaSeasonal() == 1) {
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
    protected void writeUserRegs(TsVariableDescriptor[] spec) {
        if (spec == null) {
            return;
        }
        TsDomain xdom = domain.extend(0, 3 * domain.getFrequency().intValue());
        for (int i = 0; i < spec.length; ++i) {
            ITsVariable var = spec[i].toTsVariable(context);
            Matrix M = new Matrix(xdom.getLength(), var.getDim());
            List<DataBlock> cols = M.columnList();
            var.data(xdom, cols);
            for (int j = 0; j < M.getColumnsCount(); ++j) {
                openRegSection();
                write(Item.IUSER, 1);
                write(Item.NSER, 1);
                write(Item.ILONG, xdom.getLength());
                write(Item.REGEFF, convert(spec[i].getEffect()));
                closeSection();
                openFreeSection();
                write(M.column(i));
                closeSection();
            }
        }
    }

 
    @Override
    protected void writeOutlierRegs(OutlierDefinition[] spec) {
        if (spec == null || spec.length == 0 || domain == null) {
            return;
        }
        openRegSection();
        write(Item.IUSER, 2);
        write(Item.NSER, spec.length);
        closeSection();
        openFreeSection();
        for (int i = 0; i < spec.length; ++i) {
            TsPeriod p = new TsPeriod(domain.getFrequency());
            p.set(spec[i].getPosition());
            m_builder.append(sep).append(p.minus(domain.getStart()) + 1).
                    append(sep).append(spec[i].getCode());
        }
        closeSection();
    }

    @Override
    protected void writeRampRegs(Ramp[] spec) {
        //TODO
    }

    @Override
    protected void writeEstimateSpan(TransformSpec spec) {
        // TODO
    }

    @Override
    protected void writeOutliersSpan(OutlierSpec spec) {
        if (domain != null && spec.getSpan().getType() != PeriodSelectorType.All) {
            TsDomain ndom = domain.select(spec.getSpan());
            if (ndom.getLength() == 0) {
                write(Item.INT1, domain.getLength() + 1);
            } else {
                write(Item.INT1, 1 + (ndom.getStart().minus(domain.getStart())));
                write(Item.INT1, ndom.getEnd().minus(domain.getStart()));
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

}
