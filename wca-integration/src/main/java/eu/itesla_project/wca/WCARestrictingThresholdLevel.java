/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public enum WCARestrictingThresholdLevel {
    ZERO(0, "NO RESTRICTION"),
    ONE(1, "No flow thresholds on the HV part"),
    TWO(2, "No flow thresholds on the foreign part"),
    THREE(3, "No flow thresholds on the HV and foreign parts");

    private final int level;
    private final String description;

    WCARestrictingThresholdLevel(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return "" + level + " (" + description + ")";
    }

    public static WCARestrictingThresholdLevel fromLevel(int level) {
        return Arrays.stream(values())
                .filter(restrLevel -> ( restrLevel.level == level))
                .findFirst()
                .orElse(null);
    }
}
