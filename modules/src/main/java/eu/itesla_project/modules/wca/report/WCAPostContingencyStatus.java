/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.wca.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import eu.itesla_project.security.LimitViolation;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCAPostContingencyStatus {

    private final String contingencyId;
    private final WCALoadflowResult postContingencyLoadflowResult;
    private Collection<LimitViolation> postContingencyViolationsWithoutUncertainties = new ArrayList<>();
    private WCALoadflowResult postContingencyWithUncertaintiesLoadflowResult;
    private Collection<LimitViolation> postContingencyViolationsWithUncertainties = new ArrayList<>();
    private boolean curativeActionsAvailable;
    private Collection<WCAActionApplication> curativeActionsApplication = new ArrayList<>();

    public WCAPostContingencyStatus(String contingencyId, WCALoadflowResult postContingencyLoadflowResult) {
        this.contingencyId = Objects.requireNonNull(contingencyId);
        this.postContingencyLoadflowResult = Objects.requireNonNull(postContingencyLoadflowResult);
    }

    public String getContingencyId() {
        return contingencyId;
    }

    public WCALoadflowResult getPostContingencyLoadflowResult() {
        return postContingencyLoadflowResult;
    }

    public Collection<LimitViolation> getPostContingencyViolationsWithoutUncertainties() {
        return postContingencyViolationsWithoutUncertainties;
    }

    public void setPostContingencyViolationsWithoutUncertainties(Collection<LimitViolation> postContingencyViolations) {
        this.postContingencyViolationsWithoutUncertainties = Objects.requireNonNull(postContingencyViolations);
    }
    
    public WCALoadflowResult getPostContingencyWithUncertaintiesLoadflowResult() {
        return postContingencyWithUncertaintiesLoadflowResult;
    }

    public void setPostContingencyWithUncertaintiesLoadflowResult(WCALoadflowResult postContingencyLoadflowResult) {
        this.postContingencyWithUncertaintiesLoadflowResult = Objects.requireNonNull(postContingencyLoadflowResult);
    }
    
    public Collection<LimitViolation> getPostContingencyViolationsWithUncertainties() {
        return postContingencyViolationsWithUncertainties;
    }

    public void setPostContingencyViolationsWithUncertainties(Collection<LimitViolation> postContingencyViolations) {
        this.postContingencyViolationsWithUncertainties = Objects.requireNonNull(postContingencyViolations);
    }

    public boolean areCurativeActionsAvailable() {
        return curativeActionsAvailable;
    }

    public void setCurativeActionsAvailable(boolean curativeActionsAvailable) {
        this.curativeActionsAvailable = curativeActionsAvailable;
    }

    public Collection<WCAActionApplication> getCurativeActionsApplication() {
        return curativeActionsApplication;
    }

    public void setCurativeActionsApplication(Collection<WCAActionApplication> curativeActionsApplication) {
        this.curativeActionsApplication = Objects.requireNonNull(curativeActionsApplication);
    }

}
