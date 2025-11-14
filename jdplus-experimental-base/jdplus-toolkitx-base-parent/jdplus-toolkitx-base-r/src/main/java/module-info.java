module jdplus.toolkitx.base.r {

    requires static lombok;
    requires static nbbrd.design;
    requires static nbbrd.service;
    requires static org.jspecify;

    requires jdplus.toolkit.base.api;
    requires jdplus.toolkit.base.core;
    requires jdplus.sts.base.api;
    requires jdplus.sts.base.core;
    requires jdplus.sts.base.r;
    requires jdplus.toolkitx.base.api;
    requires jdplus.toolkitx.base.core;

    exports jdplus.toolkitx.base.r;
}
