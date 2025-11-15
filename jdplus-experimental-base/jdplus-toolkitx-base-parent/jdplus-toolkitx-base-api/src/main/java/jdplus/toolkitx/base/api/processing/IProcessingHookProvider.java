/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdplus.toolkitx.base.api.processing;

/**
 *
 * @author Jean
 */
public interface IProcessingHookProvider<I> {

    void setHookMessage(String msg);

    String getHookMessage();

    boolean hasHooks();

    void register(IProcessingHook<I> hook);

    void unregister(IProcessingHook<I> hook);

    void processHooks(IProcessingHook.HookInformation<I> info, boolean cancancel);

}
