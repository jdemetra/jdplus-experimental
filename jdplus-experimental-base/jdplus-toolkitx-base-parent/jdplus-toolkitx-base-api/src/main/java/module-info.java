module jdplus.toolkitx.base.api {

    requires static lombok;
    requires static nbbrd.design;
    requires static nbbrd.service;
    requires static org.jspecify;
    
    requires jdplus.toolkit.base.api;
    
    exports jdplus.toolkitx.base.api.processing;
}