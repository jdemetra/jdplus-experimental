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

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jdplus.sa.base.api.ComponentType;
import jdplus.sa.base.api.SaVariable;
import jdplus.toolkit.base.api.data.Range;
import jdplus.toolkit.base.api.information.InformationSet;
import jdplus.toolkit.base.api.math.matrices.Matrix;
import jdplus.toolkit.base.api.timeseries.StaticTsDataSupplier;
import jdplus.toolkit.base.api.timeseries.TimeSelector;
import jdplus.toolkit.base.api.timeseries.TsData;
import jdplus.toolkit.base.api.timeseries.TsDataSupplier;
import jdplus.toolkit.base.api.timeseries.TsDomain;
import jdplus.toolkit.base.api.timeseries.TsPeriod;
import jdplus.toolkit.base.api.timeseries.regression.AdditiveOutlier;
import jdplus.toolkit.base.api.timeseries.regression.IOutlier;
import jdplus.toolkit.base.api.timeseries.regression.InterventionVariable;
import jdplus.toolkit.base.api.timeseries.regression.LevelShift;
import jdplus.toolkit.base.api.timeseries.regression.ModellingContext;
import jdplus.toolkit.base.api.timeseries.regression.PeriodicOutlier;
import jdplus.toolkit.base.api.timeseries.regression.TransitoryChange;
import jdplus.toolkit.base.api.timeseries.regression.TsContextVariable;
import jdplus.toolkit.base.api.timeseries.regression.TsDataSuppliers;
import jdplus.toolkit.base.api.timeseries.regression.TsVariable;
import jdplus.toolkit.base.api.timeseries.regression.Variable;
import jdplus.toolkit.base.api.util.Arrays2;
import jdplus.toolkit.base.api.util.IntList;
import jdplus.toolkit.base.api.util.NameManager;
import jdplus.toolkit.base.api.util.NamedObject;
import jdplus.toolkit.base.api.util.Paths;
import jdplus.toolkit.base.core.math.matrices.FastMatrix;
import jdplus.toolkit.base.core.strings.Tokenizer;
import jdplus.tramoseats.base.api.tramo.CalendarSpec;
import jdplus.tramoseats.base.api.tramo.OutlierSpec;
import jdplus.tramoseats.base.api.tramo.RegressionSpec;
import jdplus.tramoseats.base.api.tramo.RegressionTestType;
import jdplus.tramoseats.base.api.tramo.TradingDaysSpec;
import jdplus.tramoseats.base.api.tramo.TramoSpec;
import jdplus.tramoseats.base.api.tramoseats.TramoSeatsSpec;

/**
 *
 * @author Jean Palate
 */
public class LegacyDecoder extends AbstractDecoder {

    private static final String INTERNAL = "_ts_internal";
    private static final String EXTERNAL = "_ts_external_";
    private static int idx = 0;
    private static final HashMap<String, String> extFiles = new HashMap<>();
    private final File path;
    private TsDomain domain;
    private final ModellingContext context;
    private int iter;

    public LegacyDecoder(ModellingContext context, File path) {
        this.path=path;
        domain = null;
        tdecoders.add(new OutliersDecoder());
        if (context == null) {
            this.context = ModellingContext.getActiveContext();
        } else {
            this.context = context;
        }
    }
    
    @Override
    public ModellingContext getContext(){
        return context;
    }

    public List<Document> decodeMultiDocument(BufferedReader reader) {
        unused.clear();
        elements.clear();
        ArrayList<Document> docs = new ArrayList<>();
        Document doc0;
        try {
            doc0=readData(reader);
            if (doc0==null) {
                return null;
            }
        } catch (IOException ex) {
            return null;
        }
        update(doc0.series);
        doc0.setSpec(readInputSection(reader, TramoSeatsSpec.DEFAULT));
        docs.add(doc0);
        if (iter != 0) {
            switch (iter) {
                case 1 -> {
                    while (true) {
                        TramoSeatsSpec spec = TramoSeatsSpec.DEFAULT;
                        spec = readInputSection(reader, spec);
                        if (spec == null) {
                            break;
                        }
                        Document doc = new Document(doc0.getName(), doc0.getSeries(), context);
                        doc.setSpec(spec); 
                        docs.add(doc);
                    }
                }
                case 2 -> {
                    ArrayList<NamedObject<TsData>> items = new ArrayList<>();
                    try {
                        readData(reader, items);
                    } catch (IOException ex) {
                        return null;
                    }
                    for (NamedObject<TsData> cur : items) {

                         Document doc = new Document(cur.getName(), cur.getObject(), context);
                        doc.setSpec(doc0.getSpec()); 
                        docs.add(doc);
                    }
                }
                case 3 -> {
                    while (true) {
                        Document doc;
                        try {
                            doc=readData(reader);
                            if (doc == null) {
                                break;
                            }
                        } catch (IOException ex) {
                            return null;
                        }
                        update(doc.series);
                        doc.setSpec(readInputSection(reader, TramoSeatsSpec.DEFAULT));
                        docs.add(doc);
                    }
                }
            }
        }

        return docs;
    }

    @Override
    protected void readSpecificInputSection(TramoSeatsSpec spec) {
        Number niter = takeInt(Item.ITER, elements);
        if (niter != null) {
            iter = niter.intValue();
        } else {
            iter = 0;
        }
    }

    @Override
    protected void update(TsData s) {
        domain = s.getDomain();
    }

    protected void readData(BufferedReader reader, List<NamedObject<TsData>> data) throws IOException {
        do {
            NamedObject<TsData> s = readSeries(reader);
            if (s == null) {
                break;
            } else {
                data.add(s);
            }
        } while (true);
    }

    @Override
    protected int readRegs(BufferedReader reader, RegressionSpec.Builder regression) {
        try {
            Number user = takeInt(Item.IUSER, elements);
            if (user != null) {
                switch (user.intValue()) {
                    case 2 -> {
                        return readOutliers(reader, regression);
                    }
                    case 1 -> {
                        return readVariables(reader, regression, false);
                    }
                    case -1 -> {
                        return readVariables(reader, regression, true);
                    }
                    case -2 -> {
                        return readHolidays(reader, regression);
                    }
                }
                return 0;
            }
            return readIntervention(reader, regression);
        } catch (IOException ex) {
            return 0;
        }
    }

    private int readOutliers(BufferedReader reader, RegressionSpec.Builder regression) throws IOException {
        Number nser = takeInt(Item.NSER, elements);
        if (nser != null) {
            Tokenizer details = new Tokenizer(reader.readLine());
            for (int i = 0; i < nser.intValue(); ++i) {
                if (!details.hasNextToken()) {
                    return nser.intValue();
                }
                int pos;
                try {
                    pos = Integer.parseInt(details.nextToken());
                } catch (NumberFormatException err) {
                    return 0;
                }
                if (!details.hasNextToken()) {
                    return 0;
                }
                String code = details.nextToken().toUpperCase(Locale.ROOT);
                TsPeriod p = domain.get(pos - 1);
                IOutlier o;
                switch (code) {
                    case AdditiveOutlier.CODE ->
                        o = new AdditiveOutlier(p.start());
                    case LevelShift.CODE ->
                        o = new LevelShift(p.start(), true);
                    case TransitoryChange.CODE ->
                        o = new TransitoryChange(p.start(), 0.7);
                    case PeriodicOutlier.CODE, PeriodicOutlier.PO ->
                        o = new PeriodicOutlier(p.start(), 0, true);
                    default -> {
                        return 0;
                    }
                }
                regression.outlier(Variable.variable(o.description(domain), o));
            }
            return nser.intValue();
        } else {
            return 0;
        }
    }

    @Override
    protected String nextInput(BufferedReader reader) {
        return nextItem(LegacyEncoder.INPUT_, reader);
    }

    private void nextLine(BufferedReader reader) throws IOException {
        int ch;
        while ((ch = reader.read()) != -1) {
            char c = (char) ch;
            if (c == '\n') {
                break;
            }
        }
    }

    private String nextItem(String id, BufferedReader reader) {
        StringBuilder builder = new StringBuilder();
        boolean start = false, stop = false;
        while (!stop) {
            try {
                int ch = reader.read();
                if (ch == -1) {
                    break;
                }
                if (ch == '$') {
                    if (!start) {
                        start = true;
                    } else {
                        stop = true;
                        nextLine(reader);
                    }
                } else {
                    char c = (char) ch;
                    if (start) {
                        builder.append(c);
                    }
                }
            } catch (IOException ex) {
                return null;
            }
        }
        String s = builder.toString().trim();
        int pos = s.indexOf(id);
        if (pos >= 0) {
            return s.substring(pos + id.length());
        }
        return null;
    }

    @Override
    protected String nextRegs(BufferedReader reader) {
        return nextItem(LegacyEncoder.REG_, reader);
    }

    private int readVariables(BufferedReader reader, RegressionSpec.Builder regression, boolean external) throws IOException {
        if (external) {
            return readExternalVariables(reader, regression);
        } else {
            return readInternalVariables(reader, regression);
        }
    }

    private String nameFromFile(String file) {
        String name = extFiles.get(file);
        if (name == null) {
            name = EXTERNAL + (++idx);
            extFiles.put(file, name);
        }
        return name;
    }

    private int readExternalVariables(BufferedReader reader, RegressionSpec.Builder regression) throws IOException {
        Number n = takeInt(Item.ILONG, elements);
        Number nser = takeInt(Item.NSER, elements);
        Number regeffect = takeInt(Item.REGEFF, elements);
        if (n == null || nser == null) {
            return 0;
        }
        int nvars = nser.intValue();
        String input = reader.readLine();
        String fullName = path == null ? input : Paths.concatenate(path.getAbsolutePath(), input);
        String vname = nameFromFile(fullName);
        String[] names = new String[nvars];
        int ip = input.indexOf('.');
        String ninput = ip < 0 ? input : input.substring(0, ip);
        for (int i = 0; i < nvars; ++i) {
            names[i] = ninput + '_' + i;
        }
        NameManager<TsDataSuppliers> vmgr = context.getTsVariableManagers();
        TsDataSuppliers vars = vmgr.get(vname);
        if (vars == null) {
            Matrix M = readExternalMatrix(n.intValue(), nvars, input);
            if (M != null) {
                vars = new TsDataSuppliers();
                for (int i = 0; i < nvars; ++i) {
                    TsDataSupplier cvar = new StaticTsDataSupplier(TsData.of(domain.getStartPeriod(), M.column(i)));
                    vars.set(names[i], cvar);
                }
                vmgr.set(vname, vars);
            } else {
                return nser.intValue();
            }
        }
        ArrayList<String> cdesc = new ArrayList<>();
        for (int i = 0; i < nvars; ++i) {
            if (regeffect != null && regeffect.intValue() == 6) {
                cdesc.add(InformationSet.item(vname, names[i]));
            } else {
                TsContextVariable s = new TsContextVariable(InformationSet.item(vname, names[i]));
                Variable<TsContextVariable> v = Variable.<TsContextVariable>builder()
                        .core(s)
                        .name(s.getId())
                        .attribute(SaVariable.REGEFFECT, convert(regeffect == null ? 0 : regeffect.intValue()).name())
                        .build();
                regression.userDefinedVariable(v);
            }
        }
        if (!cdesc.isEmpty()) {
            CalendarSpec calendar = regression.build().getCalendar();

            String[] ntd = new String[cdesc.size()];
            ntd = cdesc.toArray(ntd);
            String[] oldtd = calendar.getTradingDays().getUserVariables();
            if (oldtd != null) {
                ntd = Arrays2.concat(oldtd, ntd);
            }
            TradingDaysSpec td = TradingDaysSpec.userDefined(ntd, RegressionTestType.None);
            calendar = calendar.toBuilder()
                    .tradingDays(td)
                    .build();
            regression.calendar(calendar);
        }
        return nser.intValue();
    }

    private int readInternalVariables(BufferedReader reader, RegressionSpec.Builder regression) throws IOException {
        Number n = takeInt(Item.ILONG, elements);
        Number nser = takeInt(Item.NSER, elements);
        Number regeffect = takeInt(Item.REGEFF, elements);
        if (n == null || nser == null) {
            return 0;
        }
        int nvars = nser.intValue();
        String input = reader.readLine();
        NameManager<TsDataSuppliers> vmgr = context.getTsVariableManagers();
        TsDataSuppliers vars = vmgr.get(INTERNAL);
        String[] names = new String[nvars];
        for (int i = 0; i < nvars; ++i) {
            names[i] = vars.nextName();
        }

        Matrix M = readMatrix(n.intValue(), nser.intValue(), input);
        if (M != null) {
            for (int i = 0; i < nvars; ++i) {
                if (!vars.contains(names[i])) {
                    StaticTsDataSupplier cvar = new StaticTsDataSupplier(TsData.of(domain.getStartPeriod(), M.column(i)));
                    vars.set(names[i], cvar);
                }
            }
        } else {
            return nser.intValue();
        }

        ArrayList<String> cdesc = new ArrayList<>();
        for (int i = 0; i < nvars; ++i) {
            if (regeffect != null && regeffect.intValue() == 6) {
                cdesc.add(InformationSet.item(INTERNAL, names[i]));
            } else {
                TsContextVariable s = new TsContextVariable(InformationSet.item(INTERNAL, names[i]));
                Variable<TsContextVariable> v = Variable.<TsContextVariable>builder()
                        .core(s)
                        .name(s.getId())
                        .attribute(SaVariable.REGEFFECT, convert(regeffect == null ? 0 : regeffect.intValue()).name())
                        .build();
                regression.userDefinedVariable(v);
            }
        }
        if (!cdesc.isEmpty()) {
            CalendarSpec calendar = regression.build().getCalendar();

            String[] ntd = new String[cdesc.size()];
            ntd = cdesc.toArray(ntd);
            String[] oldtd = calendar.getTradingDays().getUserVariables();
            if (oldtd != null) {
                ntd = Arrays2.concat(oldtd, ntd);
            }
            TradingDaysSpec td = TradingDaysSpec.userDefined(ntd, RegressionTestType.None);
            calendar = calendar.toBuilder()
                    .tradingDays(td)
                    .build();
            regression.calendar(calendar);
        }
        return nser.intValue();
    }

    private int readHolidays(BufferedReader reader, RegressionSpec.Builder regression) throws IOException {
        Number n = takeInt(Item.ILONG, elements);
        Number nser = takeInt(Item.NSER, elements);
        Matrix M = readExternalMatrix(n.intValue(), nser.intValue(), reader.readLine().trim());
        if (M != null) {
            return nser.intValue();
        } else {
            return 0;
        }
    }
    
    private int readIntervention(BufferedReader reader, RegressionSpec.Builder regression) throws IOException {
        Number seq = takeInt(Item.ISEQ, elements);
        if (seq == null) {
            return 0;
        }
        Number delta = takeDouble(Item.DELTA, elements);
        Number deltas = takeDouble(Item.DELTAS, elements);
        Number id1ds = takeInt(Item.ID1DS, elements);

        InterventionVariable.Builder var = InterventionVariable.builder();
        int[] params = nextIntParameters(reader);
        for (int i = 0; i < params.length; i += 2) {
            int i0=params[i] - 1, n=params[i+1];
            LocalDateTime start = domain.get(i0).start(), end = domain.get(i0+n-1).start();
            var.sequence(Range.of(start, end));
        }
        if (delta != null) {
            var.delta(delta.doubleValue());
        }
        if (deltas != null) {
            var.deltaSeasonal(deltas.doubleValue());
        }
        if (id1ds != null && id1ds.intValue() == 1) {
            var.delta(1).deltaSeasonal(1);
        }
        InterventionVariable iv = var.build();
        Variable<InterventionVariable> v = Variable.<InterventionVariable>builder()
                .core(iv)
                .name("iv")
                .attribute(SaVariable.REGEFFECT, SaVariable.defaultComponentTypeOf(iv).name())
                .build();
        regression.interventionVariable(v);
        return 1;
    }

    private int[] nextIntParameters(BufferedReader reader) throws IOException {
        String input = reader.readLine();

        Tokenizer tokenizer = new Tokenizer(input);
        String token;

        // Params : lines, year, period, frequency
        final IntList params = new IntList();

        while (tokenizer.hasNextToken()) {
            token = tokenizer.nextToken();
            int i = Integer.parseInt(token);
            params.add(i);
        }
        return params.toArray();
    }

    private Matrix readExternalMatrix(int n, int nser, String file) {
        try {
            File f = path == null ? Path.of(file).toFile() : path.toPath().resolve(file).toFile();
            Reader reader = Files.newBufferedReader(f.toPath(), Charset.defaultCharset());
            StringBuilder builder = new StringBuilder();
            char[] data = new char[1024];
            int nr;
            do {
                nr = reader.read(data);
                if (nr > 0) {
                    builder.append(data, 0, nr);
                }
            } while (nr > 0);
            Tokenizer tokenizer = new Tokenizer(builder.toString());
            FastMatrix m = FastMatrix.make(n, nser);
            int r = 0, c = -1;
            while (tokenizer.hasNextToken()) {
                String token = tokenizer.nextToken();
                try {
                    double val = Double.parseDouble(token);
                    if (++c == nser) {
                        c = 0;
                        ++r;
                    }
                    if (r >= n) {
                        break;
                    }
                    m.set(r, c, val);
                } catch (NumberFormatException err) {
                    return null;
                }
            }
            return m;
        } catch (IOException err) {
            return null;
        }
    }

    private Matrix readMatrix(int n, int nser, String buffer) {
        Tokenizer tokenizer = new Tokenizer(buffer);
        FastMatrix m = FastMatrix.make(n, nser);
        int r = 0, c = -1;
        while (tokenizer.hasNextToken()) {
            String token = tokenizer.nextToken();
            try {
                double val = Double.parseDouble(token);
                if (++c == nser) {
                    c = 0;
                    ++r;
                }
                if (r >= n) {
                    break;
                }
                m.set(r, c, val);
            } catch (NumberFormatException err) {
                return null;
            }
        }
        return m;
    }

    static ComponentType convert(int intValue) {
        return switch (intValue) {
            case 1 ->
                ComponentType.Trend;
            case 2 ->
                ComponentType.Seasonal;
            case 3 ->
                ComponentType.Irregular;
            default ->
                ComponentType.Undefined;
        };
    }

    /**
     * Handles IATIP, AIO, VA, IMVX,
     *
     * @author Jean Palate
     */
    class OutliersDecoder implements TramoSpecDecoder {

        OutliersDecoder() {
        }

        @Override
        public TramoSpec process(Map<String, String> dictionary, TramoSpec spec) {
            OutlierSpec.Builder outliers = spec.getOutliers().toBuilder();
            boolean processed = false;
            Number iatip = takeDouble(Item.IATIP, dictionary);
            outliers.ao(false);
            outliers.ls(false);
            outliers.tc(false);
            if (iatip != null && iatip.intValue() != 0) {
                processed = true;
                Number aio = takeInt(Item.AIO, dictionary);
                if (aio == null) {
                    outliers.ao(true);
                    outliers.ls(true);
                    outliers.tc(true);
                } else {
                    switch (aio.intValue()) {
                        case 0, 2 -> {
                            outliers.ao(true);
                            outliers.ls(true);
                            outliers.tc(true);
                        }
                        case 1 -> {
                            outliers.ao(true);
                            outliers.tc(true);
                        }
                        case 3 -> {

                        }
                    }
                }
            }
            Number va  = takeDouble(Item.VA, dictionary);
            if (va  != null) {
                processed = true;
                outliers.criticalValue(va.doubleValue());
            }
            Number imvx = takeInt(Item.IMVX, dictionary);
            if (imvx != null) {
                processed = true;
                outliers.maximumLikelihood(imvx.intValue() == 1);
            }
            if (readSpan(dictionary, outliers)) {
                processed = true;
            }
            if (processed) {
                return spec.toBuilder()
                        .outliers(outliers.build())
                        .build();
            } else {
                return spec;
            }
        }

        private boolean readSpan(Map<String, String> dictionary, OutlierSpec.Builder outliers) {
            Number int1 = takeInt(Item.INT1, dictionary);
            Number int2 = takeInt(Item.INT2, dictionary);
            if (int1 == null && int2 == null) {
                return false;
            } else {
                if (domain != null) {
                    int i1 = int1 == null ? 0 : int1.intValue() - 1;
                    int i2 = int2 == null ? domain.getLength() - 1 : int2.intValue() - 1;
                    if (i1 < 0 || i1 >= i2 || i2 >= domain.getLength()) {
                        TsPeriod start = domain.get(i1), end = domain.get(i2);
                        outliers.span(TimeSelector.between(start.start(), end.start()));
                    }
                }
                return true;
            }
        }
    }

}
