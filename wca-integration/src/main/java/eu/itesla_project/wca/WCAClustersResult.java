/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import eu.itesla_project.modules.wca.WCAClusterNum;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCAClustersResult {

    private static final WCAClusterNum CLUSTER_NUM_DEFAULT = WCAClusterNum.UNDEFINED;
    private static final boolean FOUND_VIOLATIONS_DEFAULT = false;
    private static final int CURATIVE_ACTION_INDEX_DEFAULT = 0;
    private static final Map<String, Float> INJECTIONS_DEFAULT = Collections.emptyMap();

    private final WCAClusterNum clusterNum;
    private final boolean foundViolations;
    private final int curativeActionIndex;
    private final Map<String, Float> injections;

    public WCAClustersResult() {
        this(CLUSTER_NUM_DEFAULT, FOUND_VIOLATIONS_DEFAULT, CURATIVE_ACTION_INDEX_DEFAULT, INJECTIONS_DEFAULT);
    }

    public WCAClustersResult(WCAClusterNum clusterNum, boolean foundViolations,
                             int curativeActionIndex, Map<String, Float> injections) {
        this.clusterNum = Objects.requireNonNull(clusterNum);
        this.foundViolations = foundViolations;
        this.curativeActionIndex = curativeActionIndex;
        this.injections = Objects.requireNonNull(injections);
    }

    public WCAClusterNum getClusterNum() {
        return clusterNum;
    }

    public boolean foundViolations() {
        return foundViolations;
    }

    public int getCurativeActionIndex() {
        return curativeActionIndex;
    }

    public Map<String, Float> getInjections() {
        return injections;
    }

    @Override
    public String toString() {
        return "WCAClustersResult["
                + "clusterNum=" + clusterNum
                + ",foundViolations=" + foundViolations
                + ",curativeActionIndex=" + curativeActionIndex
                + "]";
    }

}
