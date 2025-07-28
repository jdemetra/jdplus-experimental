import jdplus.calendars.base.core.ChineseMovingHolidays;
import jdplus.calendars.base.core.GregorianMovingHolidays;
import jdplus.calendars.base.core.IslamicMovingHolidays;
import jdplus.calendars.base.core.JulianMovingHolidays;
import jdplus.toolkit.base.core.modelling.regression.MovingHolidayProvider;

module jdplus.calendars.base.core {

    requires static lombok;
    requires static nbbrd.design;
    requires static nbbrd.service;
    requires static org.jspecify;

    requires transitive jdplus.calendars.base.api;
    requires jdplus.toolkit.base.core;

    exports jdplus.calendars.base.core;
    exports jdplus.calendars.base.core.internal;

    provides MovingHolidayProvider with
            JulianMovingHolidays.EasterProvider,
            IslamicMovingHolidays.RasElAmProvider,
            ChineseMovingHolidays.NewYear,
            GregorianMovingHolidays.EasterProvider;
}