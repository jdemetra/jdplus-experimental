module jdplus.calendars.base.core {

    requires static lombok;
    requires static nbbrd.design;
    requires static nbbrd.service;
    requires static org.checkerframework.checker.qual;

    requires transitive jdplus.calendars.base.api;
    requires jdplus.toolkit.base.core;

    exports jdplus.specialcalendar;
    exports jdplus.specialcalendar.internal;

    provides jdplus.modelling.regression.MovingHolidayProvider with
            jdplus.specialcalendar.JulianMovingHolidays.EasterProvider,
            jdplus.specialcalendar.IslamicMovingHolidays.RasElAmProvider,
            jdplus.specialcalendar.ChineseMovingHolidays.NewYear,
            jdplus.specialcalendar.GregorianMovingHolidays.EasterProvider;
}