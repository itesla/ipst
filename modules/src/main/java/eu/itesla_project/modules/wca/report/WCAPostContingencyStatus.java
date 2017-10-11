/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.wca.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.powsybl.security.LimitViolation;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCAPostContingencyStatus {

    private final String contingencyId;
    private final WCALoadflowResult postContingencyLoadflowResult;
    private List<LimitViolation> postContingencyViolationsWithoutUncertainties = new ArrayList<>();
    private WCALoadflowResult postContingencyWithUncertaintiesLoadflowResult;
    private List<LimitViolation> postContingencyViolationsWithUncertainties = new ArrayList<>();
    private boolean curativeActionsAvailable;
    private List<WCAActionApplication> curativeActionsApplication = new ArrayList<>();

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

    public List<LimitViolation> getPostContingencyViolationsWithoutUncertainties() {
        return postContingencyViolationsWithoutUncertainties;
    }

    public void setPostContingencyViolationsWithoutUncertainties(List<LimitViolation> postContingencyViolations) {
        this.postContingencyViolationsWithoutUncertainties = Objects.requireNonNull(postContingencyViolations);
    }

    public WCALoadflowResult getPostContingencyWithUncertaintiesLoadflowResult() {
        return postContingencyWithUncertaintiesLoadflowResult;
    }

    public void setPostContingencyWithUncertaintiesLoadflowResult(WCALoadflowResult postContingencyLoadflowResult) {
        this.postContingencyWithUncertaintiesLoadflowResult = Objects.requireNonNull(postContingencyLoadflowResult);
    }

    public List<LimitViolation> getPostContingencyViolationsWithUncertainties() {
        return postContingencyViolationsWithUncertainties;
    }

    public void setPostContingencyViolationsWithUncertainties(List<LimitViolation> postContingencyViolations) {
        this.postContingencyViolationsWithUncertainties = Objects.requireNonNull(postContingencyViolations);
    }

    public boolean areCurativeActionsAvailable() {
        return curativeActionsAvailable;
    }

    public void setCurativeActionsAvailable(boolean curativeActionsAvailable) {
        this.curativeActionsAvailable = curativeActionsAvailable;
    }

    public List<WCAActionApplication> getCurativeActionsApplication() {
        return curativeActionsApplication;
    }

    public void setCurativeActionsApplication(List<WCAActionApplication> curativeActionsApplication) {
        this.curativeActionsApplication = Objects.requireNonNull(curativeActionsApplication);
    }

}
