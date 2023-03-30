module jdplus.experimentalsa.base.core {

    requires static lombok;
    requires static nbbrd.design;
    requires static nbbrd.service;
    requires static org.checkerframework.checker.qual;

    requires transitive jdplus.experimentalsa.base.api;
    requires jdplus.toolkit.base.api;
    requires jdplus.toolkit.base.core;
    requires jdplus.sa.base.api;

    exports jdplus.experimentalsa.base.core.dfa;
    exports jdplus.experimentalsa.base.core.filters;
    exports jdplus.experimentalsa.base.core.rkhs;
    exports jdplus.experimentalsa.base.core.x11plus;
}