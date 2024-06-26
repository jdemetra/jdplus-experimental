/*
 * Copyright 2024 JDemetra+.
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *      https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package jdplus.tramoseatsx.base.core;

import java.io.BufferedReader;
import jdplus.toolkit.base.api.timeseries.TsData;
import jdplus.toolkit.base.api.timeseries.regression.ModellingContext;
import jdplus.tramoseats.base.api.tramoseats.TramoSeatsSpec;

/**
 *
 * @author Jean Palate
 */
public interface Decoder {
    
    @lombok.Getter
    public static class Document{
        String name;
        TsData series;
        ModellingContext context;
        
        public Document(String name,TsData series,ModellingContext context){
            this.name=name;
            this.series=series;
            this.context=context;
        }
        
        @lombok.Setter
        TramoSeatsSpec spec;
    }
    
    TramoSeatsSpec decodeSpec(BufferedReader reader);

    Document decodeDocument(BufferedReader reader);
    
    ModellingContext getContext();
}
