module jdplus.experimentalsa.base.r {

    requires static lombok;
    requires static nbbrd.design;
    requires static nbbrd.service;
    requires static org.checkerframework.checker.qual;

    requires transitive jdplus.experimentalsa.base.api;
    requires jdplus.experimentalsa.base.core;
    requires jdplus.toolkit.base.core;
    requires jdplus.sa.base.api;

    exports demetra.saexperimental.r;
}