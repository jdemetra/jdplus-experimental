module jdplus.businesscycle.base.io {

    requires static lombok;
    requires static nbbrd.design;
    requires static nbbrd.service;
    requires static org.jspecify;

    requires transitive jdplus.businesscycle.base.api;
}