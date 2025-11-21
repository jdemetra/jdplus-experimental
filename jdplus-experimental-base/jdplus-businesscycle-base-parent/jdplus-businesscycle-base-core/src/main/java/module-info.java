module jdplus.businesscycle.base.core {

    requires static lombok;
    requires static nbbrd.design;
    requires static nbbrd.service;
    requires static org.jspecify;

    requires transitive jdplus.businesscycle.base.api;
    requires jdplus.toolkit.base.api;
    requires jdplus.toolkit.base.core;

    exports jdplus.businesscycle.base.core;
    exports jdplus.businesscycle.base.core.regular;
}