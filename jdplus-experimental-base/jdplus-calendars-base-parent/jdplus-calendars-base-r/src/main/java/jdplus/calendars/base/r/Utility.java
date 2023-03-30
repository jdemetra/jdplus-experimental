/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdplus.calendars.base.r;

import jdplus.toolkit.base.api.timeseries.TsDomain;
import jdplus.toolkit.base.api.timeseries.TsPeriod;
import jdplus.toolkit.base.api.timeseries.TsUnit;
import jdplus.toolkit.base.api.timeseries.calendars.HolidayPattern;
import jdplus.toolkit.base.api.timeseries.regression.MovingHolidayVariable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import jdplus.toolkit.base.core.modelling.regression.Regression;
import jdplus.calendars.base.core.ChineseMovingHolidays;
import jdplus.calendars.base.core.GregorianMovingHolidays;
import jdplus.calendars.base.core.IslamicMovingHolidays;
import jdplus.calendars.base.core.JulianMovingHolidays;

/**
 *
 * @author PALATEJ
 */
@lombok.experimental.UtilityClass
public class Utility {

    public String[] easter(String start, String end) {
        LocalDate pstart = LocalDate.parse(start, DateTimeFormatter.ISO_DATE);
        LocalDate pend = LocalDate.parse(start, DateTimeFormatter.ISO_DATE);

        return convert(GregorianMovingHolidays.easter(pstart, pend));
    }

    public String[] julianEaster(String start, String end) {
        LocalDate pstart = LocalDate.parse(start, DateTimeFormatter.ISO_DATE);
        LocalDate pend = LocalDate.parse(start, DateTimeFormatter.ISO_DATE);

        return convert(JulianMovingHolidays.easter(pstart, pend));
    }

    public String[] chineseNewYear(String start, String end) {
        LocalDate pstart = LocalDate.parse(start, DateTimeFormatter.ISO_DATE);
        LocalDate pend = LocalDate.parse(start, DateTimeFormatter.ISO_DATE);

        return convert(ChineseMovingHolidays.newYear(pstart, pend));
    }

    public String[] rasElAm(String start, String end) {
        LocalDate pstart = LocalDate.parse(start, DateTimeFormatter.ISO_DATE);
        LocalDate pend = LocalDate.parse(start, DateTimeFormatter.ISO_DATE);

        return convert(IslamicMovingHolidays.rasElAm(pstart, pend));
    }

    String[] convert(LocalDate[] holidays) {
        String[] all = new String[holidays.length];
        for (int i = 0; i < all.length; ++i) {
            all[i] = holidays[i].format(DateTimeFormatter.ISO_DATE);
        }
        return all;
    }

    public double[] mh(String date, int length, String event, int start, String shape0, int l0, String shape1, int l1, String shape2, int l2) {
        HolidayPattern pattern;
        if (shape2 != null) {
            pattern = HolidayPattern.of(start, HolidayPattern.Shape.valueOf(shape0), l0,
                    HolidayPattern.Shape.valueOf(shape1), l1, HolidayPattern.Shape.valueOf(shape2), l2);
        } else if (shape1 != null) {
            pattern = HolidayPattern.of(start, HolidayPattern.Shape.valueOf(shape0), l0,
                    HolidayPattern.Shape.valueOf(shape1), l1);
        } else {
            pattern = HolidayPattern.of(start, HolidayPattern.Shape.valueOf(shape0), l0);
        }
        LocalDate ldate = LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
        MovingHolidayVariable var=new MovingHolidayVariable(event, pattern);
        TsDomain daily=TsDomain.of(TsPeriod.of(TsUnit.DAY, ldate), length);
        return Regression.matrix(daily, var).getStorage();
    }
}
