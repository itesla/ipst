/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import eu.itesla_project.modules.wca.WCAClusterNum;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCAClustersResult {

    private WCAClusterNum clusterNum = WCAClusterNum.UNDEFINED;
    private boolean foundViolations = false;
    private int curativeActionIndex = 0;
    private Map<String, Float> injections = new HashMap<String, Float>();

    public WCAClustersResult() {
    }

    public WCAClustersResult(WCAClusterNum clusterNum) {
        this.clusterNum = Objects.requireNonNull(clusterNum);
    }

    public WCAClusterNum getClusterNum() {
        return clusterNum;
    }

    public void setClusterNum(WCAClusterNum clusterNum) {
        this.clusterNum = Objects.requireNonNull(clusterNum);
    }
    
    public boolean foundViolations() {
        return foundViolations;
    }

    public void setFoundViolations(boolean foundViolations) {
        this.foundViolations = foundViolations;
    }
    
    public void setCurativeActionIndex(int actionIndex) {
        this.curativeActionIndex = actionIndex;
    }

    public int getCurativeActionIndex() {
        return curativeActionIndex;
    }

    public Map<String, Float> getInjections() {
        return injections;
    }

    public void setInjections(Map<String, Float> injections) {
        this.injections = Objects.requireNonNull(injections);
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
