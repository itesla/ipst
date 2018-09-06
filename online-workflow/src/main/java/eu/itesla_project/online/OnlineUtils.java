/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017-2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.security.LimitViolationType;
import com.powsybl.simulation.securityindexes.SecurityIndex;

import eu.itesla_project.modules.histo.HistoDbAttr;
import eu.itesla_project.modules.histo.HistoDbNetworkAttributeId;

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

    public static DateTime toCetDate(DateTime date) {
        DateTimeZone cet = DateTimeZone.forID("CET");
        if (!date.getZone().equals(cet)) {
            return date.toDateTime(cet);
        }
        return date;
    }

    public static LinkedHashMap<String, Double> getBranchesData(Network network) {
        LinkedHashMap<String, Double> branchesData = new LinkedHashMap<>();
        network.getBranchStream().forEach(branch -> {
            addBranchSideData(branchesData, branch.getId(), branch.getTerminal1(), branch.getCurrentLimits1() == null ? Double.NaN : branch.getCurrentLimits1().getPermanentLimit());
            addBranchSideData(branchesData, branch.getId(), branch.getTerminal2(), branch.getCurrentLimits2() == null ? Double.NaN : branch.getCurrentLimits2().getPermanentLimit());
        });
        return branchesData;
    }

    private static void addBranchSideData(LinkedHashMap<String, Double> branchesData, String branchId, Terminal terminal, double currentLimit) {
        branchesData.put(getAttributeKey(branchId, terminal.getVoltageLevel().getId(), HistoDbAttr.I.name()), terminal.getI());
        branchesData.put(getAttributeKey(branchId, terminal.getVoltageLevel().getId(), HistoDbAttr.P.name()), terminal.getP());
        branchesData.put(getAttributeKey(branchId, terminal.getVoltageLevel().getId(), "IMAX"), currentLimit);
    }

    private static String getAttributeKey(String branchId, String voltageLevelId, String attributeId) {
        return branchId + HistoDbNetworkAttributeId.SIDE_SEPARATOR + voltageLevelId + "_" + attributeId;
    }

}
