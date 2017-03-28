/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.wca.report;

import java.util.Objects;

import eu.itesla_project.modules.rules.SecurityRule;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCASecurityRuleApplication {

    private final String contingencyId;
    private final SecurityRule securityRule;
    private final boolean isRuleViolated;
    private final WCARuleViolationType ruleViolationType;
    private final String cause;

    public WCASecurityRuleApplication(String contingencyId, SecurityRule securityRule, boolean isRuleViolated, 
            WCARuleViolationType ruleViolationType, String cause) {
        this.contingencyId = Objects.requireNonNull(contingencyId);
        this.securityRule = securityRule;
        this.isRuleViolated = isRuleViolated;
        this.ruleViolationType = Objects.requireNonNull(ruleViolationType);
        this.cause = cause;
    }

    public String getContingencyId() {
        return contingencyId;
    }

    public SecurityRule getSecurityRule() {
        return securityRule;
    }

    public boolean isRuleViolated() {
        return isRuleViolated;
    }

    public WCARuleViolationType getRuleViolationType() {
        return ruleViolationType;
    }

    public String getCause() {
        return cause;
    }

}
