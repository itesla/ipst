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
public class WCAActionApplication {

    private final String actionId;
    private final LimitViolation violation;
    private final WCALoadflowResult loadflowResult;
    private final boolean violationsRemoved;
    private boolean actionApplied;
    private final String comment;
    private List<LimitViolation> postActionViolations = new ArrayList<>();

    public WCAActionApplication(String actionId, LimitViolation violation, WCALoadflowResult loadflowResult,
            boolean violationsRemoved, boolean actionApplied, String comment) {
        this.actionId = Objects.requireNonNull(actionId);
        this.violation = violation;
        this.loadflowResult = Objects.requireNonNull(loadflowResult);
        this.violationsRemoved = violationsRemoved;
        this.actionApplied = actionApplied;
        this.comment = comment;
    }

    public String getActionId() {
        return actionId;
    }

    public LimitViolation getViolation() {
        return violation;
    }

    public WCALoadflowResult getLoadflowResult() {
        return loadflowResult;
    }

    public boolean areViolationsRemoved() {
        return violationsRemoved;
    }

    public boolean isActionApplied() {
        return actionApplied;
    }

    public String getComment() {
        return comment;
    }

    public List<LimitViolation> getPostActionViolations() {
        return postActionViolations;
    }

    public void setActionApplied(boolean actionApplied) {
        this.actionApplied = actionApplied;
    }

    public void setPostActionViolations(List<LimitViolation> postActionViolations) {
        this.postActionViolations = Objects.requireNonNull(postActionViolations);
    }

}
