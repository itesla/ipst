/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online;

import eu.itesla_project.contingency.Contingency;
import eu.itesla_project.security.LimitViolationType;
import eu.itesla_project.simulation.securityindexes.SecurityIndex;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Quinary <itesla@quinary.com>
 */
public final class OnlineUtils {

    private OnlineUtils() {
    }

    public static Collection<Contingency> filterContingencies(List<Contingency> contingencies, List<String> contingenciesIds) {
        Objects.requireNonNull(contingencies, "contingencies list is null");
        Objects.requireNonNull(contingenciesIds, "contingenciesIds list is null");
        return contingencies
                .stream()
                .filter(contingency -> contingenciesIds.contains(contingency.getId()))
                .collect(Collectors.toList());
    }

    public static Set<String> getContingencyIds(List<Contingency> contingencies) {
        Objects.requireNonNull(contingencies, "contingencies list is null");
        return contingencies.stream().map(Contingency::getId).collect(Collectors.toSet());
    }

    public static boolean isSafe(Collection<SecurityIndex> securityIndexes) {
        Objects.requireNonNull(securityIndexes, "security indexes collection is null");
        for (SecurityIndex index : securityIndexes) {
            if (!index.isOk()) {
                return false;
            }
        }
        return true;
    }

    public static String getPostContingencyId(String stateId, String contingencyId) {
        Objects.requireNonNull(stateId, "state id is null");
        Objects.requireNonNull(contingencyId, "contingency id is null");
        return stateId + "-post-" + contingencyId;
    }

    public static UnitEnum getUnit(LimitViolationType type) {
        Objects.requireNonNull(type);
        switch (type) {
        case CURRENT:
            return UnitEnum.MW;
        case LOW_VOLTAGE:
        case HIGH_VOLTAGE:
            return UnitEnum.KV;
        default:
            throw new IllegalArgumentException();
        }
    }

}
