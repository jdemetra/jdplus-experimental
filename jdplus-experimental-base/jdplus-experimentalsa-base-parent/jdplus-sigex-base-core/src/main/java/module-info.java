module jdplus.sigex.base.core {

    requires static lombok;
    requires static nbbrd.design;
    requires static nbbrd.service;
    requires static org.checkerframework.checker.qual;

    requires transitive jdplus.experimentalsa.base.api;
    requires jdplus.toolkit.base.core;

    exports jdplus.sigex.base.core;
}