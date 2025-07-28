module jdplus.businesscycle.base.r {

    requires static lombok;
    requires static nbbrd.design;
    requires static nbbrd.service;
    requires static org.jspecify;

    requires transitive jdplus.businesscycle.base.api;
    requires jdplus.toolkit.base.api;
    requires jdplus.businesscycle.base.core;

    exports jdplus.businesscycle.base.r;
}