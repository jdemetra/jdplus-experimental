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
public interface IProcessingHook<I> {

    public static final String EMPTY = "";

    public static class HookInformation<I> {

        public HookInformation(I info) {
            this.information = info;
            cancel = false;
        }

        @lombok.Getter
        private final I information;

        @lombok.Getter
        @lombok.Setter
        private boolean cancel;

        @lombok.Getter
        @lombok.Setter
        private String message = EMPTY;
    }

    void process(HookInformation<I> info, boolean cancancel);
}
