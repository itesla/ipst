/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.wca;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public enum WCAClusterNum {
    UNDEFINED(-1),
    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4);

    private final int intValue;
    
    static Map<Integer, WCAClusterNum> mapping = new HashMap<>();
    static {
        Arrays.stream(values()).forEach(clusterNum -> mapping.put(clusterNum.intValue, clusterNum));
    }

    WCAClusterNum(int intValue) {
        this.intValue = intValue;
    }

    public int toIntValue() {
        return intValue;
    }
    
    public static WCAClusterNum fromInt(int intNum) {
        if ( mapping.containsKey(intNum) ) {
            return mapping.get(intNum);
        }
        throw new RuntimeException("Undefined cluster number " + intNum);
    }
}
