module jdplus.toolkitx.base.core {

    requires static lombok;
    requires static nbbrd.design;
    requires static nbbrd.service;
    requires static org.jspecify;

    requires jdplus.toolkit.base.api;
    requires jdplus.toolkit.base.core;


    exports jdplus.toolkitx.base.core.arima;
    exports jdplus.toolkitx.base.core.tarima;
}