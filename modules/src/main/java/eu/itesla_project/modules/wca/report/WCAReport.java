/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.wca.report;

import java.nio.file.Path;
import java.util.Collection;

import eu.itesla_project.security.LimitViolation;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public interface WCAReport {
    
    String getBasecase();

    WCALoadflowResult getBaseStateLoadflowResult();

    Collection<LimitViolation> getPreContingencyViolationsWithoutUncertainties();
    
    WCALoadflowResult getBaseStateWithUncertaintiesLoadflowResult();

    Collection<LimitViolation> getPreContingencyViolationsWithUncertainties();

    Collection<WCAActionApplication> getPreventiveActionsApplication();

    Collection<LimitViolation> getPostPreventiveActionsViolationsWithUncertainties();

    Collection<LimitViolation> getBaseStateRemainingViolations();

    Collection<WCASecurityRuleApplication> getSecurityRulesApplication();

    Collection<WCAPostContingencyStatus> getPostContingenciesStatus();

    boolean exportCsv(Path folder);

}