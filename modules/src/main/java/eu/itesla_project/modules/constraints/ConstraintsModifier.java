/**
 * Copyright (c) 2016-2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.constraints;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.StateManager;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationFilter;
import com.powsybl.security.Security;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class ConstraintsModifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintsModifier.class);

    private Network network;
    private ConstraintsModifierConfig config;

    public ConstraintsModifier(Network network) {
        this(network, ConstraintsModifierConfig.load());
    }

    public ConstraintsModifier(Network network, ConstraintsModifierConfig config) {
        LOGGER.info(config.toString());
        this.network = network;
        this.config = config;
    }

    public void looseConstraints(String stateId) {
        looseConstraints(stateId, 0f, false);
    }

    public void looseConstraints(String stateId, float margin) {
        looseConstraints(stateId, margin, false);
    }

    public void looseConstraints(String stateId, float margin, boolean applyToBaseCase) {
        if (network.getStateManager().getStateIds().contains(stateId)) {
            String workingStateId = network.getStateManager().getWorkingStateId();
            network.getStateManager().setWorkingState(stateId);
            List<LimitViolation> violations = Security.checkLimits(network);
            looseConstraints(stateId, violations, margin, applyToBaseCase);
            network.getStateManager().setWorkingState(workingStateId);
        } else {
            throw new RuntimeException("No " + stateId + " in network " + network.getId() + ": cannot loose constraints");
        }
    }

    public void looseConstraints(String stateId, List<LimitViolation> violations) {
        looseConstraints(stateId, violations, 0f, false);
    }

    public void looseConstraints(String stateId, List<LimitViolation> violations, float margin) {
        looseConstraints(stateId, violations, margin, false);
    }

    public void looseConstraints(String stateId, List<LimitViolation> violations, float margin, boolean applyToBaseCase) {
        Objects.requireNonNull(stateId, "state id is null");
        Objects.requireNonNull(violations, "violations is null");
        if (network.getStateManager().getStateIds().contains(stateId)) {
            String workingStateId = network.getStateManager().getWorkingStateId();
            network.getStateManager().setWorkingState(stateId);
            LOGGER.info("Loosening constraints of network {}, state {}, using margin {}",
                    network.getId(),
                    network.getStateManager().getWorkingStateId(),
                    margin);
            LimitViolationFilter violationsFilter = new LimitViolationFilter(config.getViolationsTypes(), 0);
            List<LimitViolation> filteredViolations = violationsFilter.apply(violations, network);
            String report = Security.printLimitsViolations(violations, network, violationsFilter);
            if (report != null) {
                LOGGER.debug("Fixing constraints of network {}, state {}, causing the following {} violations:\n{}",
                        network.getId(),
                        network.getStateManager().getWorkingStateId(),
                        filteredViolations.size(),
                        report);
            }
            for (LimitViolation violation : filteredViolations) {
                LOGGER.debug("Fixing the constraints causing the {} violation on equipment {}",
                        violation.getLimitType(),
                        violation.getSubjectId());
                switch (violation.getLimitType()) {
                    case CURRENT:
                        setNewCurrentLimit(stateId, violation, margin, applyToBaseCase);
                        break;
                    case HIGH_VOLTAGE:
                        setNewHighVoltageLimit(stateId, violation, margin, applyToBaseCase);
                        break;
                    case LOW_VOLTAGE:
                        setNewLowVoltageLimit(stateId, violation, margin, applyToBaseCase);
                        break;
                }
            }
            network.getStateManager().setWorkingState(workingStateId);
        } else {
            throw new RuntimeException("No " + stateId + " in network " + network.getId() + ": cannot loose constraints");
        }
    }

    private void setNewCurrentLimit(String stateId, LimitViolation violation, float margin, boolean applyToBaseCase) {
        Branch branch = network.getBranch(violation.getSubjectId());
        if (branch != null) {
            float newLimit = getNewUpperLimit(violation, margin);
            if (branch.getTerminal1().getI() == violation.getValue()) {
                LOGGER.debug("State {}: changing current limit 1 of branch {}: {} -> {}",
                        stateId,
                        branch.getId(),
                        violation.getLimit(),
                        newLimit);
                branch.newCurrentLimits1().setPermanentLimit(newLimit).add();
                if (applyToBaseCase && !StateManager.INITIAL_STATE_ID.equals(stateId)) { // change the limit also to basecase
                    network.getStateManager().setWorkingState(StateManager.INITIAL_STATE_ID);
                    branch = network.getBranch(violation.getSubjectId());
                    LOGGER.debug("State {}: changing current limit 1 of branch {}: {} -> {}",
                                StateManager.INITIAL_STATE_ID,
                                branch.getId(),
                                violation.getLimit(),
                                newLimit);
                    branch.newCurrentLimits1().setPermanentLimit(newLimit).add();
                    network.getStateManager().setWorkingState(stateId);
                }
            } else if (branch.getTerminal2().getI() == violation.getValue()) {
                LOGGER.debug("State {}: changing current limit 2 of branch {}: {} -> {}",
                        stateId,
                        branch.getId(),
                        violation.getLimit(),
                        newLimit);
                branch.newCurrentLimits2().setPermanentLimit(newLimit).add();
                if (applyToBaseCase && !StateManager.INITIAL_STATE_ID.equals(stateId)) { // change the limit also to basecase
                    network.getStateManager().setWorkingState(StateManager.INITIAL_STATE_ID);
                    branch = network.getBranch(violation.getSubjectId());
                    LOGGER.debug("State {}: changing current limit 2 of branch {}: {} -> {}",
                                StateManager.INITIAL_STATE_ID,
                                branch.getId(),
                                violation.getLimit(),
                                newLimit);
                    branch.newCurrentLimits2().setPermanentLimit(newLimit).add();
                    network.getStateManager().setWorkingState(stateId);
                }
            }
        } else {
            LOGGER.warn("State {}: cannot change current limit of branch {}: no branch with this id in the network",
                    stateId,
                    violation.getSubjectId());
        }
    }

    private void setNewHighVoltageLimit(String stateId, LimitViolation violation, float margin, boolean applyToBaseCase) {
        VoltageLevel voltageLevel = network.getVoltageLevel(violation.getSubjectId());
        if (voltageLevel != null) {
            if (violation.getValue() > voltageLevel.getHighVoltageLimit()) { // it could already have been fixed
                float newLimit = getNewUpperLimit(violation, margin);
                LOGGER.debug("State {}: changing high voltage limit of voltage level {}: {} -> {}",
                        stateId,
                        voltageLevel.getId(),
                        violation.getLimit(),
                        newLimit);
                voltageLevel.setHighVoltageLimit(newLimit);
                if (applyToBaseCase && !StateManager.INITIAL_STATE_ID.equals(stateId)) { // change the limit also to basecase
                    network.getStateManager().setWorkingState(StateManager.INITIAL_STATE_ID);
                    voltageLevel = network.getVoltageLevel(violation.getSubjectId());
                    LOGGER.debug("State {}: changing high voltage limit of voltage level {}: {} -> {}",
                                StateManager.INITIAL_STATE_ID,
                                voltageLevel.getId(),
                                violation.getLimit(),
                                newLimit);
                    voltageLevel.setHighVoltageLimit(newLimit);
                    network.getStateManager().setWorkingState(stateId);
                }
            }
        } else {
            LOGGER.warn("State {}: cannot change high voltage limit of voltage level {}: no voltage level with this id in the network",
                    stateId,
                    violation.getSubjectId());
        }
    }

    private void setNewLowVoltageLimit(String stateId, LimitViolation violation, float margin, boolean applyToBaseCase) {
        VoltageLevel voltageLevel = network.getVoltageLevel(violation.getSubjectId());
        if (voltageLevel != null) {
            if (violation.getValue() < voltageLevel.getLowVoltageLimit()) { // it could already have been fixed
                float newLimit = getNewLowerLimit(violation, margin);
                LOGGER.debug("State {}: changing low voltage limit of voltage level {}: {} -> {}",
                        stateId,
                        voltageLevel.getId(),
                        violation.getLimit(),
                        newLimit);
                voltageLevel.setLowVoltageLimit(newLimit);
                if (applyToBaseCase && !StateManager.INITIAL_STATE_ID.equals(stateId)) { // change the limit also to basecase
                    network.getStateManager().setWorkingState(StateManager.INITIAL_STATE_ID);
                    voltageLevel = network.getVoltageLevel(violation.getSubjectId());
                    LOGGER.debug("State {}: changing low voltage limit of voltage level {}: {} -> {}",
                                StateManager.INITIAL_STATE_ID,
                                voltageLevel.getId(),
                                violation.getLimit(),
                                newLimit);
                    voltageLevel.setLowVoltageLimit(newLimit);
                }
            }
        } else {
            LOGGER.warn("State {}: cannot change low voltage limit of voltage level {}: no voltage level with this id in the network",
                    stateId,
                    violation.getSubjectId());
        }
    }

    private float getNewUpperLimit(LimitViolation violation, float margin) {
        float newLimit = 9999;
        if (config.isInAreaOfInterest(violation, network)) {
            newLimit = violation.getValue() * (1.0f + margin / 100.0f);
        }
        return newLimit;
    }

    private float getNewLowerLimit(LimitViolation violation, float margin) {
        float newLimit = -9999;
        if (config.isInAreaOfInterest(violation, network)) {
            newLimit = violation.getValue() * (1.0f - margin / 100.0f);
        }
        return newLimit;
    }

}
