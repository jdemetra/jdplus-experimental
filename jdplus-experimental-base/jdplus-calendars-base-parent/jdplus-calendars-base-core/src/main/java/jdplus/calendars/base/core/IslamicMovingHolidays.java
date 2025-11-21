/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdplus.calendars.base.core;

import jdplus.calendars.base.core.internal.Islamic;
import jdplus.calendars.base.core.internal.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import jdplus.toolkit.base.core.modelling.regression.MovingHolidayProvider;
import nbbrd.service.ServiceProvider;

/**
 *
 * @author palatej
 */
@lombok.experimental.UtilityClass
public class IslamicMovingHolidays {

    public LocalDate[] rasElAm(LocalDate start, LocalDate end) {
       return holidays(start, end, y->Islamic.rasElAm(y));
    }

    public LocalDate[] aidelAdha(LocalDate start, LocalDate end) {
       return holidays(start, end, y->Islamic.aidelAdha(y));
    }

    public LocalDate[] aidelFitr(LocalDate start, LocalDate end) {
       return holidays(start, end, y->Islamic.aidelFitr(y));
    }

    public LocalDate[] achoura(LocalDate start, LocalDate end) {
        return holidays(start, end, y->Islamic.achoura(y));
    }
    
    public LocalDate[] magaldeTouba(LocalDate start, LocalDate end) {
        return holidays(start, end, y->Islamic.magaldeTouba(y));
    }
    
    public LocalDate[] mawlidAnNabi(LocalDate start, LocalDate end) {
        return holidays(start, end, y->Islamic.mawlidAnNabi(y));
    }
    
    public LocalDate[] startRamadan(LocalDate start, LocalDate end) {
        return holidays(start, end, y->Islamic.startRamadan(y));
    }
    
    public LocalDate[] holidays(LocalDate start, LocalDate end, IntFunction<long[]> fn) {
        int y0 = start.getYear();
        int y1 = end.getYear();
        List<LocalDate> all = new ArrayList<>();
        long[] l = fn.apply(y0);
        if (y0 == y1) {
            for (int i = 0; i < l.length; ++i) {
                LocalDate d = Date.fixedToLocalDate(l[i]);
                if (!d.isBefore(start) && d.isBefore(end)) {
                    all.add(d);
                }
            }
        } else {
            for (int i = 0; i < l.length; ++i) {
                LocalDate d = Date.fixedToLocalDate(l[i]);
                if (!d.isBefore(start)) {
                    all.add(d);
                }
            }
            for (int y = y0 + 1; y < y1; ++y) {
                l = fn.apply(y);
                for (int i = 0; i < l.length; ++i) {
                    all.add(Date.fixedToLocalDate(l[i]));
                }
            }
            l = fn.apply(y1);
            for (int i = 0; i < l.length; ++i) {
                LocalDate d = Date.fixedToLocalDate(l[i]);
                if (d.isBefore(end)) {
                    all.add(d);
                }
            }
        }
        return all.toArray(LocalDate[]::new);
    }
 
    private final String RASELAM = "islamic.raselam";

    @ServiceProvider(MovingHolidayProvider.class)
    public static class RasElAmProvider implements MovingHolidayProvider {

        @Override
        public String identifier() {
            return RASELAM;
        }

        @Override
        public LocalDate[] holidays(LocalDate start, LocalDate end) {
            return rasElAm(start, end);
        }
    }

}
