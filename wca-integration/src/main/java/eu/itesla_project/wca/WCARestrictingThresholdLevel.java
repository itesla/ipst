/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import java.util.Objects;
import java.util.Set;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public enum WCARestrictingThresholdLevel {
    NO_HV_THRESHOLDS("No flow thresholds on the HV part", 1),
    NO_FOREIGN_THRESHOLDS("No flow thresholds on the foreign part", 2);

    private final String description;

    private final int level;

    WCARestrictingThresholdLevel(String description, int level) {
        this.description = description;
        this.level = level;
    }

    private int getLevel() {
        return level;
    }

    public static int getLevel(Set<WCARestrictingThresholdLevel> levels) {
        Objects.requireNonNull(levels);
        return levels.stream()
                .map(WCARestrictingThresholdLevel::getLevel)
                .reduce(0, (a, b) -> (a | b));
    }
}

