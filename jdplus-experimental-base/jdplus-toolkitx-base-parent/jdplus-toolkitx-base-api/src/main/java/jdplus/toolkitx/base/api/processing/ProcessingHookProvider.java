/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdplus.toolkitx.base.api.processing;

import java.util.ArrayList;
import jdplus.toolkit.base.api.information.InformationSet;
import jdplus.toolkitx.base.api.processing.IProcessingHook.HookInformation;

/**
 *
 * @author Jean
 * @param <I> Information dispatched in hooking
 */
public class ProcessingHookProvider<I> implements IProcessingHookProvider<I> {

    private final ArrayList<IProcessingHook<I>> hooks = new ArrayList<>();
    private volatile String message = IProcessingHook.EMPTY;

    @Override
    public void setHookMessage(String msg) {
        message = msg;
    }

    @Override
    public String getHookMessage() {
        return message;
    }

    @Override
    public synchronized boolean hasHooks() {
        return !hooks.isEmpty();
    }

    @Override
    public synchronized void register(IProcessingHook<I> hook) {
        hooks.add(hook);
    }

    @Override
    public synchronized void unregister(IProcessingHook<I> hook) {
        for (IProcessingHook<I> cur : hooks) {
            if (hook == cur) {
                hooks.remove(cur);
                return;
            }
        }
    }

    @Override
    public synchronized void processHooks(HookInformation<I> info, boolean cancancel) {
        info.setMessage(InformationSet.item(message, info.getMessage()));
        for (IProcessingHook<I> cur : hooks) {
            cur.process(info, cancancel);
            if (cancancel && info.isCancel()) {
                return;
           }
        }
    }
 
    public void copyHooks(final ProcessingHookProvider<I> ph) {
        synchronized (ph.hooks) {
            hooks.addAll(ph.hooks);
            message = ph.message;
        }
    }

}
