module jdplus.experimentalsa.base.r {

    requires static lombok;
    requires static nbbrd.design;
    requires static nbbrd.service;
    requires static org.jspecify;

    requires transitive jdplus.experimentalsa.base.api;
    requires jdplus.experimentalsa.base.core;
    requires jdplus.toolkit.base.core;
    requires jdplus.sa.base.api;

//    exports jdplus.experimentalsa.base.r;
}