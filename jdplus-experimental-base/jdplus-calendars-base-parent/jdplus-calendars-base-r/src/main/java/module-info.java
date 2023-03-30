module jdplus.calendars.base.r {

    requires static lombok;
    requires static nbbrd.design;
    requires static nbbrd.service;
    requires static org.checkerframework.checker.qual;

    requires transitive jdplus.calendars.base.api;
    requires jdplus.toolkit.base.api;
    requires jdplus.toolkit.base.core;
    requires jdplus.calendars.base.core;

    exports jdplus.calendars.base.r;
}