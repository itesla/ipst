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

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCADomainsResult {

    private boolean foundBasicViolations = false;
    private boolean rulesViolated = false;
    private int preventiveActionIndex = 0;
    private Map<String, Float> injections = new HashMap<String, Float>();

    public boolean foundBasicViolations() {
        return foundBasicViolations;
    }

    public void setFoundBasicViolations(boolean foundBasicViolations) {
        this.foundBasicViolations = foundBasicViolations;
    }

    public boolean areRulesViolated() {
        return rulesViolated;
    }

    public void setRulesViolated(boolean rulesViolated) {
        this.rulesViolated = rulesViolated;
    }

    public void setPreventiveActionIndex(int actionIndex) {
        this.preventiveActionIndex = actionIndex;
    }

    public int getPreventiveActionIndex() {
        return preventiveActionIndex;
    }

    public Map<String, Float> getInjections() {
        return injections;
    }

    public void setInjections(Map<String, Float> injections) {
        this.injections = Objects.requireNonNull(injections);
    }

    @Override
    public String toString() {
        return "WCADomainsResult["
                + "foundBasicViolations=" + foundBasicViolations
                + ",rulesViolated=" + rulesViolated
                + ",preventiveActionIndex=" + preventiveActionIndex
                + "]";
    }

}
