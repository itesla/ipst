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

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCADomainsResult {

    private static final boolean FOUND_BASIC_VIOLATIONS_DEFAULT = false;
    private static final boolean RULES_VIOLATED_DEFAULT = false;
    private static final int PREVENTIVE_ACTION_INDEX_DEFAULT = 0;
    private static final Map<String, Float> INJECTIONS_DEFAULT = Collections.emptyMap();

    private final boolean foundBasicViolations;
    private final boolean rulesViolated;
    private final int preventiveActionIndex;
    private final Map<String, Float> injections;

    public WCADomainsResult() {
        this(FOUND_BASIC_VIOLATIONS_DEFAULT, RULES_VIOLATED_DEFAULT, PREVENTIVE_ACTION_INDEX_DEFAULT, INJECTIONS_DEFAULT);
    }

    public WCADomainsResult(boolean foundBasicViolations, boolean rulesViolated,
                            int preventiveActionIndex, Map<String, Float> injections) {
        this.foundBasicViolations = foundBasicViolations;
        this.rulesViolated = rulesViolated;
        this.preventiveActionIndex = preventiveActionIndex;
        this.injections = Objects.requireNonNull(injections);
    }

    public boolean foundBasicViolations() {
        return foundBasicViolations;
    }

    public boolean areRulesViolated() {
        return rulesViolated;
    }

    public int getPreventiveActionIndex() {
        return preventiveActionIndex;
    }

    public Map<String, Float> getInjections() {
        return injections;
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
