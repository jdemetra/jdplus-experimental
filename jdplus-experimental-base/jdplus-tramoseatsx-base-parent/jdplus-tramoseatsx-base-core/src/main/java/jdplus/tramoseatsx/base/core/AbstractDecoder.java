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

import java.io.BufferedReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jdplus.toolkit.base.api.arima.SarimaSpec;
import jdplus.toolkit.base.api.data.Parameter;
import jdplus.toolkit.base.api.data.ParameterType;
import jdplus.toolkit.base.api.modelling.TransformationType;
import jdplus.toolkit.base.api.timeseries.TsData;
import jdplus.toolkit.base.api.timeseries.TsPeriod;
import jdplus.toolkit.base.api.timeseries.calendars.LengthOfPeriodType;
import jdplus.toolkit.base.api.timeseries.calendars.TradingDaysType;
import jdplus.toolkit.base.api.util.IntList;
import jdplus.toolkit.base.api.util.NamedObject;
import jdplus.toolkit.base.core.strings.Tokenizer;
import jdplus.tramoseats.base.api.seats.DecompositionSpec;
import jdplus.tramoseats.base.api.tramo.AutoModelSpec;
import jdplus.tramoseats.base.api.tramo.CalendarSpec;
import jdplus.tramoseats.base.api.tramo.EasterSpec;
import jdplus.tramoseats.base.api.tramo.EstimateSpec;
import jdplus.tramoseats.base.api.tramo.RegressionSpec;
import jdplus.tramoseats.base.api.tramo.RegressionTestType;
import jdplus.tramoseats.base.api.tramo.TradingDaysSpec;
import jdplus.tramoseats.base.api.tramo.TramoSpec;
import jdplus.tramoseats.base.api.tramo.TransformSpec;
import jdplus.tramoseats.base.api.tramoseats.TramoSeatsSpec;

/**
 *
 * @author Jean Palate
 */
public abstract class AbstractDecoder implements Decoder {

    private static final double MISSING = -99999;
    private static final String[] codes;

    static {
        Item[] c = Item.values();
        codes = new String[c.length];
        for (int i = 0; i < codes.length; ++i) {
            codes[i] = c[i].name();
        }
    }

    protected Document readData(BufferedReader br) throws IOException {
        String currentLine = br.readLine();
        if (currentLine == null) {
            return null;
        }
        String name = currentLine.trim();

        /* Read second line (parameters)
         * 1) Number of lines of data
         * 2) Year
         * 3) Period
         * 4) Frequency
         */
        currentLine = br.readLine();
        if (currentLine == null) {
            return null;
        }
        Tokenizer tokenizer = new Tokenizer(currentLine);
        String token;

        // Params : lines, year, period, frequency
        final IntList params = new IntList();

        while (tokenizer.hasNextToken()) {
            token = tokenizer.nextToken();
            int i = Integer.parseInt(token);
            params.add(i);
        }

        // Controlling params
        if (params.size() != 4) {
            return null;
        }
        int nbrObs = params.get(0);
        int period = params.get(2);
        int freq = params.get(3);
        if (freq < 1 || freq > 12 || 12 % freq != 0) {
            return null;
        }
        if (period <= 0 || period > freq) {
            return null;
        }

        List<Number> data = new ArrayList<>();
        while ((currentLine = br.readLine()) != null) {
            tokenizer = new Tokenizer(currentLine);
            while (tokenizer.hasNextToken()) {
                token = tokenizer.nextToken();
                double value = Double.parseDouble(token);
                data.add(value);
            }
            if (data.size() >= nbrObs) {
                break;
            }
        }

        double[] adata = new double[data.size()];
        for (int i = 0; i < data.size(); i++) {
            double obs = data.get(i).doubleValue();
            adata[i] = (obs == MISSING ? Double.NaN : obs);
        }

        TsData series = Utility.seriesOf(freq, params.get(1), params.get(2), adata);
        return new Document(name, series, getContext());
    }

    protected NamedObject<TsData> readSeries(BufferedReader reader) throws IOException {
        String currentLine = reader.readLine();
        if (currentLine == null) {
            return null;
        }

        String name = currentLine.trim();

        /* Read second line (parameters)
         * 1) Number of lines of data
         * 2) Year
         * 3) Period
         * 4) Frequency
         */
        currentLine = reader.readLine();
        if (currentLine == null) {
            return null;
        }
        Tokenizer tokenizer = new Tokenizer(currentLine);
        String token;

        // Params : lines, year, period, frequency
        final IntList params = new IntList();

        while (tokenizer.hasNextToken()) {
            token = tokenizer.nextToken();
            int i = Integer.parseInt(token);
            params.add(i);
        }

        // Controlling params
        if (params.size() != 4) {
            return null;
        }
        int nbrObs = params.get(0);
        int period = params.get(2);
        int freq = params.get(3);
        if (freq < 1 || freq > 12 || 12 % freq != 0) {
            return null;
        }
        if (period <= 0 || period > freq) {
            return null;
        }

        List<Number> data = new ArrayList<>();
        while ((currentLine = reader.readLine()) != null) {
            tokenizer = new Tokenizer(currentLine);
            while (tokenizer.hasNextToken()) {
                token = tokenizer.nextToken();
                double value = Double.parseDouble(token);
                data.add(value);
            }
            if (data.size() >= nbrObs) {
                break;
            }
        }

        double[] adata = new double[data.size()];
        for (int i = 0; i < data.size(); i++) {
            double obs = data.get(i).doubleValue();
            adata[i] = (obs == MISSING ? Double.NaN : obs);
        }

        return new NamedObject<>(name, Utility.seriesOf(freq, params.get(1), params.get(2), adata));
    }

    protected static interface TramoSpecDecoder {

        TramoSpec process(Map<String, String> dictionary, TramoSpec spec);
    }

    protected static interface SeatsSpecDecoder {

        DecompositionSpec process(Map<String, String> dictionary, DecompositionSpec spec);
    }

    public static int decodeInt(String str) throws ParseException {
        NumberFormat integerInstance = NumberFormat.getIntegerInstance(Locale.ROOT);
        Number parse = integerInstance.parse(str);
        return parse.intValue();
    }

    public static double decodeDouble(String str) throws ParseException {
        NumberFormat doubleInstance = NumberFormat.getNumberInstance(Locale.ROOT);
        Number parse = doubleInstance.parse(str);
        return parse.doubleValue();
    }

    public static Number takeInt(Item item, Map<String, String> dic) {
        String key = item.name();
        return takeInt(key, dic);
    }

    public static TsPeriod takePeriod(int freq, Item item, int idx, Map<String, String> dic) {
        StringBuilder bkey = new StringBuilder();
        bkey.append(item.name()).append('(').append(idx).append(')');
        String key = bkey.toString();
        return takePeriod(freq, key, dic);
    }

    public static String takeCode(Item item, int idx, Map<String, String> dic) {
        StringBuilder bkey = new StringBuilder();
        bkey.append(item.name()).append('(').append(idx).append(')');
        String key = bkey.toString();
        String code = dic.get(key);
        dic.remove(key);
        return code;
    }

    public static String takeCode(Item item, Map<String, String> dic) {
        String key = item.name();
        String code = dic.get(key);
        dic.remove(key);
        return code;
    }

    public static TsPeriod takePeriod(int freq, Item item, Map<String, String> dic) {
        return takePeriod(freq, item.name(), dic);
    }

    public static Number takeDouble(Item item, Map<String, String> dic) {
        String key = item.name();
        return takeDouble(key, dic);
    }

    public static Number takeInt(Item item, int idx, Map<String, String> dic) {
        StringBuilder bkey = new StringBuilder();
        bkey.append(item.name()).append('(').append(idx).append(')');
        String key = bkey.toString();
        return takeInt(key, dic);
    }

    public static Number takeDouble(Item item, int idx, Map<String, String> dic) {
        StringBuilder bkey = new StringBuilder();
        bkey.append(item.name()).append('(').append(idx).append(')');
        String key = bkey.toString();
        return takeDouble(key, dic);
    }

    private static TsPeriod takePeriod(int freq, String key, Map<String, String> dic) {
        String str = dic.get(key);
        if (str == null) {
            return null;
        }
        NumberFormat integerInstance = NumberFormat.getIntegerInstance(Locale.ROOT);
        Number year;
        Number period;
        try {
            dic.remove(key);
            year = integerInstance.parse(str.substring(0, 4));
            period = integerInstance.parse(str.substring(5, 7));
            return Utility.periodOf(freq, year.intValue(), period.intValue());
        } catch (ParseException ex) {
            return null;
        }
    }

    private static Number takeInt(String key, Map<String, String> dic) {
        String str = dic.get(key);
        if (str == null) {
            return null;
        }
        NumberFormat integerInstance = NumberFormat.getIntegerInstance(Locale.ROOT);
        Number parse;
        try {
            parse = integerInstance.parse(str);
            dic.remove(key);
            return parse;
        } catch (ParseException ex) {
            return null;
        }
    }

    private static Number takeDouble(String key, Map<String, String> dic) {
        String str = dic.get(key);
        if (str == null) {
            return null;
        }
        NumberFormat doubleInstance = NumberFormat.getNumberInstance(Locale.ROOT);
        Number parse;
        try {
            parse = doubleInstance.parse(str);
            dic.remove(key);
            return parse;
        } catch (ParseException ex) {
            return null;
        }
    }

    protected static class RsaDecoder {

        public TramoSeatsSpec process(Map<String, String> dictionary, TramoSeatsSpec spec) {
            Number n = takeInt(Item.RSA, dictionary);
            if (n != null) {
                return switch (n.intValue()) {
                    case 0 ->
                        TramoSeatsSpec.RSA0;
                    case 1 ->
                        TramoSeatsSpec.RSA1;
                    case 2 ->
                        TramoSeatsSpec.RSA2;
                    case 3 ->
                        TramoSeatsSpec.RSA3;
                    default ->
                        TramoSeatsSpec.RSAfull;
                };
            }
            return spec;
        }
    }

    /**
     * Handles P, D, Q, BP, BD, BQ PHI, JPR, BPHI, JPS, TH, JQR, BTH, JQS, INIT
     *
     * @author Jean Palate
     */
    protected static class ArimaDecoder implements TramoSpecDecoder {

        @Override
        public TramoSpec process(Map<String, String> dictionary, TramoSpec spec) {
            boolean processed = false;
            SarimaSpec.Builder builder = SarimaSpec.airline().toBuilder();
            ParameterType ptype = readInit(dictionary);
            Number n = takeInt(Item.P, dictionary);
            if (n != null) {
                builder.p(n.intValue());
                Parameter[] p = readCoefficients(dictionary, n.intValue(), Item.PHI, Item.JPR, ptype);
                if (p != null) {
                    builder.phi(p);
                }
                processed = true;
            }
            n = takeInt(Item.D, dictionary);
            if (n != null) {
                builder.d(n.intValue());
                processed = true;
            }
            n = takeInt(Item.Q, dictionary);
            if (n != null) {
                builder.q(n.intValue());
                Parameter[] p = readCoefficients(dictionary, n.intValue(), Item.TH, Item.JQR, ptype);
                if (p != null) {
                    builder.theta(p);
                }
            }
            n = takeInt(Item.BP, dictionary);
            if (n != null) {
                builder.bp(n.intValue());
                Parameter[] p = readCoefficients(dictionary, n.intValue(), Item.BPHI, Item.JPS, ptype);
                if (p != null) {
                    builder.bphi(p);
                }
                processed = true;
            }
            n = takeInt(Item.BD, dictionary);
            if (n != null) {
                builder.bd(n.intValue());
                processed = true;
            }
            n = takeInt(Item.BQ, dictionary);
            if (n != null) {
                builder.bq(n.intValue());
                Parameter[] p = readCoefficients(dictionary, n.intValue(), Item.BTH, Item.JQS, ptype);
                if (p != null) {
                    builder.btheta(p);
                }
                processed = true;
            }

            if (processed) {
                return spec.toBuilder()
                        .arima(builder.build())
                        .build();
            } else {
                return spec;
            }
        }

        private ParameterType readInit(Map<String, String> dictionary) {
            Number n = takeInt(Item.INIT, dictionary);
            if (n != null) {
                return switch (n.intValue()) {
                    case 1 ->
                        ParameterType.Initial;
                    case 2 ->
                        ParameterType.Fixed;
                    default ->
                        ParameterType.Estimated;
                };

            }
            return ParameterType.Estimated;

        }

        private Parameter[] readCoefficients(Map<String, String> dictionary, int n, Item item, Item status, ParameterType deftype) {
            if (n == 0) {
                return null;
            }
            Parameter[] p = new Parameter[n];
            for (int i = 0; i < n; ++i) {
                Parameter c = null;
                Number v = takeDouble(item, i + 1, dictionary);
                Number s = takeInt(status, i + 1, dictionary);
                if (v != null) {
                    ParameterType type = (s != null && s.intValue() == 1) ? ParameterType.Fixed : deftype;
                    c = Parameter.of(v.doubleValue(), type);
                }else
                    c=Parameter.undefined();
                p[i] = c;
            }
            return p;
        }

    }

    /**
     * Handles IDIF, INIC, CANCEL, PC, PCR, TSIG, UB1, UB2
     *
     * @author Jean Palate
     */
    protected static class AutoModelDecoder implements TramoSpecDecoder {

        @Override
        public TramoSpec process(Map<String, String> dictionary, TramoSpec spec) {
            AutoModelSpec.Builder auto = spec.getAutoModel().toBuilder();
            boolean processed = false;
            Number idif = takeInt(Item.IDIF, dictionary);
            Number inic = takeInt(Item.INIC, dictionary);
            if (idif != null && inic != null) {
                processed = true;
                auto.enabled(idif.intValue() > 0 && inic.intValue() > 0);
            }
            Number cancel = takeDouble(Item.CANCEL, dictionary);
            if (cancel != null) {
                processed = true;
                auto.cancel(cancel.doubleValue());
            }
            Number pc = takeDouble(Item.PC, dictionary);
            if (pc != null) {
                processed = true;
                auto.pc(pc.doubleValue());
            }
            Number pcr = takeDouble(Item.PCR, dictionary);
            if (pcr != null) {
                processed = true;
                auto.pcr(pcr.doubleValue());
            }
            Number tsig = takeDouble(Item.TSIG, dictionary);
            if (tsig != null) {
                processed = true;
                auto.tsig(tsig.doubleValue());
            }
            Number ub1 = takeDouble(Item.UB1, dictionary);
            if (ub1 != null) {
                processed = true;
                auto.ub1(ub1.doubleValue());
            }
            Number ub2 = takeDouble(Item.UB2, dictionary);
            if (ub2 != null) {
                processed = true;
                auto.ub2(ub2.doubleValue());
            }
            if (processed) {
                return spec.toBuilder()
                        .autoModel(auto.build())
                        .build();
            } else {
                return spec;
            }
        }
    }

    protected static class MeanDecoder implements TramoSpecDecoder {

        @Override
        public TramoSpec process(Map<String, String> dictionary, TramoSpec spec) {
            Number n = takeInt(Item.IMEAN, dictionary);
            if (n != null && n.intValue() == 0) {
                return spec.toBuilder().regression(RegressionSpec.DEFAULT_UNUSED).build();
            } else {
                return spec.toBuilder().regression(RegressionSpec.DEFAULT_CONST).build();
            }
        }
    }

    /**
     * Handles LAM, FCT
     *
     * @author Jean Palate
     */
    protected static class TransformDecoder implements TramoSpecDecoder {

        @Override
        public TramoSpec process(Map<String, String> dictionary, TramoSpec spec) {
            TransformSpec.Builder transform = spec.getTransform().toBuilder();
            boolean processed = false;
            Number lam = takeInt(Item.LAM, dictionary);
            if (lam != null) {
                processed = true;
                switch (lam.intValue()) {
                    case 0 ->
                        transform.function(TransformationType.Log);
                    case 1 ->
                        transform.function(TransformationType.None);
                    case -1 ->
                        transform.function(TransformationType.Auto);
                }
            }
            Number fct = takeDouble(Item.FCT, dictionary);
            if (fct != null) {
                processed = true;
                transform.fct(fct.doubleValue());
            }
            if (processed) {
                return spec.toBuilder()
                        .transform(transform.build())
                        .build();
            } else {
                return spec;
            }
        }
    }

    /**
     * Handles ITRAD, IEAST, IDUR
     *
     * @author Jean Palate
     */
    protected static class CalendarDecoder implements TramoSpecDecoder {

        @Override
        public TramoSpec process(Map<String, String> dictionary, TramoSpec spec) {
            RegressionSpec regression = spec.getRegression();
            CalendarSpec calendar = regression.getCalendar();
            EasterSpec.Builder easter = calendar.getEaster().toBuilder();
            TradingDaysSpec tradingDays = calendar.getTradingDays();
            boolean tprocessed = false;
            Number ntd = takeInt(Item.ITRAD, dictionary);
            Number pftd = takeDouble(Item.pFTD, dictionary);
            if (ntd != null) {
                tprocessed = true;

                int itrad = ntd.intValue();
                if (itrad == -2) {
                    double ftd = TradingDaysSpec.DEF_PFTD;
                    if (pftd != null) {
                        ftd = 1 - pftd.doubleValue();
                    }
                    tradingDays = TradingDaysSpec.automatic(LengthOfPeriodType.LeapYear, TradingDaysSpec.AutoMethod.FTEST, ftd, false);
                } else {
                    boolean test = false;
                    if (itrad < 0) {
                        test = true;
                        itrad = -itrad;
                    }
                    boolean lp = false;
                    if (itrad == 2 || itrad == 7) {
                        lp = true;
                        --itrad;
                    }
                    TradingDaysType td
                            = switch (itrad) {
                        case 1 ->
                            TradingDaysType.TD2;
                        case 6 ->
                            TradingDaysType.TD7;
                        default ->
                            TradingDaysType.NONE;
                    };
                    tradingDays = TradingDaysSpec.td(td,
                            lp ? LengthOfPeriodType.LeapYear : LengthOfPeriodType.None,
                            test ? RegressionTestType.Separate_T : RegressionTestType.None,
                            false);
                }
                calendar = calendar
                        .toBuilder()
                        .tradingDays(tradingDays)
                        .build();

            }
            Number nee = takeInt(Item.IEAST, dictionary);
            boolean eprocessed = false;
            if (nee != null) {
                eprocessed = true;
                int ieast = nee.intValue();
                if (ieast < 0) {
                    easter.test(true);
                    ieast = -ieast;
                }
                switch (ieast) {
                    case 1 ->
                        easter.type(EasterSpec.Type.IncludeEaster); // ?
                    case 2 ->
                        easter.type(EasterSpec.Type.IncludeEaster);
                    case 3 ->
                        easter.type(EasterSpec.Type.IncludeEasterMonday);
                    default ->
                        easter.type(EasterSpec.Type.Unused);
                }
            }
            Number dur = takeInt(Item.IDUR, dictionary);
            if (dur != null) {
                eprocessed = true;
                easter.duration(dur.intValue());
            }
            if (eprocessed) {
                calendar = calendar
                        .toBuilder()
                        .easter(easter.build())
                        .build();
            }
            if (eprocessed || tprocessed) {
                regression = regression.toBuilder()
                        .calendar(calendar)
                        .build();
                return spec.toBuilder()
                        .regression(regression)
                        .build();
            } else {
                return spec;
            }
        }
    }

    /**
     * Handles RMOD, EPSPHI, XL, NOADMISS
     *
     * @author Jean Palate
     */
    protected static class SeatsDecoder implements SeatsSpecDecoder {

        @Override
        public DecompositionSpec process(Map<String, String> dictionary, DecompositionSpec spec) {
            boolean processed = false;
            DecompositionSpec.Builder seats = spec.toBuilder();
            Number rmod = takeDouble(Item.RMOD, dictionary);
            if (rmod != null) {
                processed = true;
                seats.trendBoundary(rmod.doubleValue());
            }
            Number eps = takeDouble(Item.EPSPHI, dictionary);
            if (eps != null) {
                processed = true;
                seats.seasTolerance(eps.doubleValue());
            }
            Number xl = takeDouble(Item.XL, dictionary);
            if (xl != null) {
                processed = true;
                seats.xlBoundary(xl.doubleValue());
            }
            Number nadmiss = takeInt(Item.NOADMISS, dictionary);
            if (nadmiss != null) {
                processed = true;
                seats.approximationMode(nadmiss.intValue() == 0 ? DecompositionSpec.ModelApproximationMode.None
                        : DecompositionSpec.ModelApproximationMode.Legacy);
            }
            if (processed) {
                return seats.build();
            } else {
                return spec;
            }
        }
    }

    /**
     * Handles TOL, UBP
     *
     * @author Jean Palate
     */
    protected static class EstimateDecoder implements TramoSpecDecoder {

        @Override
        public TramoSpec process(Map<String, String> dictionary, TramoSpec spec) {
            EstimateSpec.Builder estimate = spec.getEstimate().toBuilder();
            boolean processed = false;
            Number tol = takeDouble(Item.TOL, dictionary);
            if (tol != null) {
                processed = true;
                estimate.tol(tol.doubleValue());
            }
            Number ubp = takeDouble(Item.UBP, dictionary);
            if (ubp != null) {
                processed = true;
                estimate.ubp(ubp.doubleValue());
            }
            if (processed) {
                return spec.toBuilder()
                        .estimate(estimate.build())
                        .build();
            } else {
                return spec;
            }
        }
    }

    protected final Map<String, String> elements = new HashMap<>();
    protected final Map<String, String> unused = new HashMap<>();
    protected final RsaDecoder rsadecoder;
    protected final List<TramoSpecDecoder> tdecoders = new ArrayList<>();
    protected final SeatsSpecDecoder sdecoder;

    protected AbstractDecoder() {
        rsadecoder = new RsaDecoder();
        tdecoders.add(new MeanDecoder());
        tdecoders.add(new TransformDecoder());
        tdecoders.add(new EstimateDecoder());
        tdecoders.add(new ArimaDecoder());
        tdecoders.add(new AutoModelDecoder());
        tdecoders.add(new CalendarDecoder());
        sdecoder = new SeatsDecoder();

    }

    public Map<String, String> unusedElements() {
        return Collections.unmodifiableMap(unused);
    }

    protected abstract String nextInput(BufferedReader reader);

    protected abstract String nextRegs(BufferedReader reader);

    protected abstract int readRegs(BufferedReader reader, RegressionSpec.Builder regspec);

    protected abstract void readSpecificInputSection(TramoSeatsSpec spec);

    protected TramoSeatsSpec readInputSection(BufferedReader reader, TramoSeatsSpec spec) {
        String input = nextInput(reader);
        if (input == null || !read(input)) {
            return null;
        }
        spec = rsadecoder.process(elements, spec);

        TramoSpec tspec = spec.getTramo();
        for (TramoSpecDecoder decoder : tdecoders) {
            tspec = decoder.process(elements, tspec);
        }
        DecompositionSpec sspec = sdecoder.process(elements, spec.getSeats());
        spec = spec.toBuilder()
                .tramo(tspec)
                .seats(sspec)
                .build();

        readSpecificInputSection(spec);

        Number nregs = takeInt(Item.IREG, elements);
        int iregs = nregs == null ? 0 : nregs.intValue();

        if (iregs > 0) {
            unused.clear();
            unused.putAll(elements);
            String regs;
            RegressionSpec.Builder rbuilder = spec.getTramo().getRegression().toBuilder();
            while (iregs > 0) {
                regs = nextRegs(reader);
                if (regs == null) {
                    break;
                }
                if (!read(regs)) {
                    return null;
                }

                int cur = readRegs(reader, rbuilder);
                if (cur == 0) {
                    return null;
                } else {
                    iregs -= cur;
                }
            }
        spec = spec.toBuilder()
                .tramo(tspec
                        .toBuilder()
                        .regression(rbuilder.build())
                        .build())
                .seats(sspec)
                .build();
        }

        return spec;
    }

    protected abstract void update(TsData s);

    @Override
    public Document decodeDocument(BufferedReader reader) {
        clear();
        Document doc;
        try {
            doc=readData(reader);
            if (doc == null) {
                return null;
            }
        } catch (IOException ex) {
            return null;
        }
        update(doc.series);
        doc.spec = readInputSection(reader, TramoSeatsSpec.DEFAULT);

        return doc;

    }

    @Override
    public TramoSeatsSpec decodeSpec(BufferedReader reader) {
        clear();
        TramoSeatsSpec spec = TramoSeatsSpec.DEFAULT;
        return readInputSection(reader, spec);
    }

    protected void clear() {
        unused.clear();
        elements.clear();
    }

    protected boolean read(final String str) {
        elements.clear();
        String input = str.replace(',', ' ');
        Tokenizer tokens = new Tokenizer(input);
        while (tokens.hasNextToken()) {
            String token = tokens.nextToken();
            String[] items = token.split("=");
            switch (items.length) {
                case 2 ->
                    elements.put(normalize(items[0]), items[1]);
                case 1 ->
                    elements.put(normalize(items[0]), null);
                default -> {
                    return false;
                }
            }
        }
        return true;
    }

    protected static String normalize(final String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (!Character.isWhitespace(c)) {
                b.append(c);
            }
        }
        String c = b.toString();
        for (int i = 0; i < codes.length; ++i) {
            int k = c.indexOf('(');
            String pc = c;
            if (k >= 0) {
                pc = c.substring(0, k);
            }
            if (codes[i].equalsIgnoreCase(pc)) {
                return k < 0 ? codes[i] : codes[i] + c.substring(k);
            }
        }
        return c; // should not append
    }
}
