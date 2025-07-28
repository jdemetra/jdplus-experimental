module jdplus.toolkitx.base.core {

    requires static lombok;
    requires static nbbrd.design;
    requires static nbbrd.service;
    requires static org.jspecify;

    requires jdplus.toolkit.base.api;
    requires jdplus.toolkit.base.core;
    requires jdplus.tramoseats.base.api;
    requires jdplus.tramoseats.base.core;
    
    exports jdplus.tramoseatsx.base.core;

}