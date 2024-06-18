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

import java.time.LocalDate;
import jdplus.sa.base.api.ComponentType;
import static jdplus.sa.base.api.ComponentType.Irregular;
import static jdplus.sa.base.api.ComponentType.Seasonal;
import static jdplus.sa.base.api.ComponentType.Trend;
import jdplus.toolkit.base.api.timeseries.TsData;
import jdplus.toolkit.base.api.timeseries.TsPeriod;
import jdplus.toolkit.base.api.timeseries.TsUnit;
import jdplus.tramoseats.base.api.tramo.OutlierSpec;

/**
 *
 * @author Jean Palate
 */
@lombok.experimental.UtilityClass
public class Utility {

    public TsData seriesOf(int freq, int year, int start, double[] data) {
        switch (freq) {
            case 1 -> {
                return TsData.ofInternal(TsPeriod.yearly(year), data);
            }
            case 12 -> {
                return TsData.ofInternal(TsPeriod.monthly(year, start), data);
            }
            default -> {
                int c = 12 / freq;
                TsPeriod pstart = TsPeriod.of(TsUnit.ofAnnualFrequency(freq), LocalDate.of(year, (start - 1) * c + 1, 1));
                return TsData.ofInternal(pstart, data);
            }
        }
    }

    public TsPeriod periodOf(int freq, int year, int start) {
        switch (freq) {
            case 1 -> {
                return TsPeriod.yearly(year);
            }
            case 12 -> {
                return TsPeriod.monthly(year, start);
            }
            default -> {
                int c = 12 / freq;
                return TsPeriod.of(TsUnit.ofAnnualFrequency(freq), LocalDate.of(year, (start - 1) * c + 1, 1));
            }
        }
    }

    public int aio(OutlierSpec spec) {
        if (!spec.isUsed()) {
            return 0;
        }
        boolean ao = spec.isAo();
        boolean ls = spec.isLs();
        boolean tc = spec.isTc();
        if (ao && ls && !tc) {
            return 3;
        } else if (ao && tc && !ls) {
            return 1;
        } else {
            return 2;
        }
    }

    public void setAIO(int value, OutlierSpec.Builder builder) {
        switch (value) {
            case 1 ->
                builder.ao(true)
                        .tc(true)
                        .ls(false);
            case 2 ->
                builder.ao(true)
                        .tc(true)
                        .ls(true);
            case 3 ->
                builder.ao(true)
                        .tc(false)
                        .ls(true);
            default ->
                builder.ao(false)
                        .tc(false)
                        .ls(false);
        }
    }

    public int convert(ComponentType cmp) {
        return switch (cmp) {
            case Trend ->
                1;
            case Seasonal ->
                2;
            case Irregular ->
                3;
            default ->
                0;
        };
    }

}
