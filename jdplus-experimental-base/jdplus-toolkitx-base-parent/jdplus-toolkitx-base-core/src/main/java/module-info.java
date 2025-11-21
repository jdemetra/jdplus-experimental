module jdplus.toolkitx.base.core {

    requires static lombok;
    requires static nbbrd.design;
    requires static nbbrd.service;
    requires static org.jspecify;

    requires jdplus.toolkit.base.api;
    requires jdplus.toolkit.base.core;
    requires jdplus.sts.base.api;
    requires jdplus.sts.base.core;
    requires jdplus.toolkitx.base.api;


    exports jdplus.toolkitx.base.core.arima;
    exports jdplus.toolkitx.base.core.bsm;
}