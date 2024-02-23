/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdplus.calendars.base.core;

import jdplus.calendars.base.core.internal.Chinese;
import jdplus.calendars.base.core.internal.Date;
import jdplus.toolkit.base.core.modelling.regression.MovingHolidayProvider;

import java.time.LocalDate;
import java.util.function.IntToLongFunction;
import nbbrd.service.ServiceProvider;

/**
 *
 * @author palatej
 */
@lombok.experimental.UtilityClass
public class ChineseMovingHolidays {

    private final String NY = "chinese.newyear";

    public LocalDate[] newYear(LocalDate start, LocalDate end) {
        return festivals(start, end, y -> Chinese.newYear(y));
    }

    public LocalDate[] qingMing(LocalDate start, LocalDate end) {
        return festivals(start, end, y -> Chinese.qingMing(y));
    }

    private LocalDate[] festivals(LocalDate start, LocalDate end, IntToLongFunction fn) {
        int y0 = start.getYear();
        int y1 = end.getYear();
        int n = y1 - y0 + 1;
        LocalDate e0 = Date.fixedToLocalDate(fn.applyAsLong(y0));
        LocalDate e1 = Date.fixedToLocalDate(fn.applyAsLong(y1));
        boolean use0 = true;
        if (start.isAfter(e0)) {
            --n;
            use0 = false;
        }
        boolean use1 = true;
        if (!e1.isBefore(end)) {
            --n;
            use1 = false;
        }
        if (n == 0) {
            return new LocalDate[0];
        }
        if (e0.equals(e1)) {
            return new LocalDate[]{e0};
        }
        LocalDate[] ee = new LocalDate[n];
        int i = 0;
        if (use0) {
            ee[i++] = e0;
        }
        while (++y0 < y1) {
            ee[i++] = Date.fixedToLocalDate(fn.applyAsLong(y0));
        }
        if (use1) {
            ee[i] = e1;
        }
        return ee;
    }

    @ServiceProvider(MovingHolidayProvider.class)
    public static class NewYear implements MovingHolidayProvider {

        @Override
        public String identifier() {
            return NY;
        }

        @Override
        public LocalDate[] holidays(LocalDate start, LocalDate end) {
            return newYear(start, end);
        }
    }

}
