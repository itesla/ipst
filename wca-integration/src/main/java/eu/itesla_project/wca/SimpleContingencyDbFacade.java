/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016-2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import eu.itesla_project.contingency.Contingency;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.modules.contingencies.*;
import eu.itesla_project.security.LimitViolation;
import eu.itesla_project.security.LimitViolationType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SimpleContingencyDbFacade implements ContingencyDbFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleContingencyDbFacade.class);

    private final ContingenciesAndActionsDatabaseClient contingenciesActionsDbClient;

    private final Network network;

    public SimpleContingencyDbFacade(ContingenciesAndActionsDatabaseClient contingenciesActionsDbClient, Network network) {
        this.contingenciesActionsDbClient = Objects.requireNonNull(contingenciesActionsDbClient, "contingencies and actions db client is null");
        this.network = Objects.requireNonNull(network, "network is null");
    }

    @Override
    public synchronized List<Contingency> getContingencies() {
        LOGGER.info("Network {}: getting contingencies", network.getId());
        return contingenciesActionsDbClient.getContingencies(network);
    }

    private static boolean constraintsMatch(ActionsContingenciesAssociation association, List<LimitViolation> limitViolations) {
        for (Constraint constraint : association.getConstraints()) {
            switch (constraint.getType()) {
                case BRANCH_OVERLOAD:
                    if (limitViolations == null) {
                        return true;
                    } else {
                        for (LimitViolation limitViolation : limitViolations) {
                            if (limitViolation.getLimitType() == LimitViolationType.CURRENT
                                    && limitViolation.getSubjectId().equals(constraint.getEquipment())) {
                                return true;
                            }
                        }
                        return false;
                    }

                default:
                    throw new AssertionError();
            }
        }
        return true;
    }

    @Override
    public synchronized List<List<Action>> getCurativeActions(Contingency contingency, List<LimitViolation> limitViolations) {
        Objects.requireNonNull(contingency, "contingency is null");
        LOGGER.info("Network {}: getting curative actions for contingency {}", network.getId(), contingency.getId());
        List<List<Action>> curativeActions = new ArrayList<>();
        for (ActionsContingenciesAssociation association : contingenciesActionsDbClient.getActionsCtgAssociations(network)) {
            if (!association.getContingenciesId().contains(contingency.getId())) {
                continue;
            }
            if (!constraintsMatch(association, limitViolations)) {
                continue;
            }
            for (String actionId : association.getActionsId()) {
                Action action = contingenciesActionsDbClient.getAction(actionId, network);
                if (action != null) {
                    curativeActions.add(Collections.singletonList(action));
                } else {
                    ActionPlan actionPlan = contingenciesActionsDbClient.getActionPlan(actionId);
                    if (actionPlan != null) {
                        for (ActionPlanOption option : actionPlan.getPriorityOption().values()) {
                            if (option.getLogicalExpression().getOperator() instanceof UnaryOperator) {
                                UnaryOperator op = (UnaryOperator)  option.getLogicalExpression().getOperator();
                                curativeActions.add(Collections.singletonList(contingenciesActionsDbClient.getAction(op.getActionId(), network)));
                            } else {
                                throw new AssertionError("Operator " + option.getLogicalExpression().getOperator().getClass() + " not yet supported");
                            }
                        }
                    } else {
                        LOGGER.error("Network {}: action {} not found for contingency {}", network.getId(), actionId , contingency.getId());
                    }
                }
            }
        }
        LOGGER.info("Network {}: found {} curative actions for contingency {}", network.getId(), curativeActions.size(), contingency.getId());
        return curativeActions;
    }

    @Override
    public synchronized List<List<Action>> getPreventiveActions(LimitViolation limitViolation) {
        Objects.requireNonNull(limitViolation, "limit violation is null");
        LOGGER.info("Network {}: getting preventive actions for {} violation on equipment {}", 
                    network.getId(), limitViolation.getLimitType(), limitViolation.getSubjectId());
        List<List<Action>> preventiveActions = new ArrayList<>();
        if( !limitViolation.getLimitType().equals(LimitViolationType.CURRENT) ) { // just branch overload is handled, so far
            LOGGER.warn("Network {}: no preventive actions found for {} violation on equipment {}, as just branch overload is handled, so far", 
                        network.getId(), limitViolation.getLimitType(), limitViolation.getSubjectId());
            return preventiveActions;
        }
        for ( ActionsContingenciesAssociation association : contingenciesActionsDbClient.getActionsCtgAssociationsByConstraint(limitViolation.getSubjectId(), 
                                                                                                                               ConstraintType.BRANCH_OVERLOAD) ) {
            if ( !association.getContingenciesId().isEmpty() ) { // getting only actions not associated to a contingency
                continue;
            }
            for (String actionId : association.getActionsId()) {
                Action action = contingenciesActionsDbClient.getAction(actionId, network);
                if (action != null) {
                    preventiveActions.add(Collections.singletonList(action));
                } else {
                    ActionPlan actionPlan = contingenciesActionsDbClient.getActionPlan(actionId);
                    if (actionPlan != null) {
                        for (ActionPlanOption option : actionPlan.getPriorityOption().values()) {
                            if (option.getLogicalExpression().getOperator() instanceof UnaryOperator) {
                                UnaryOperator op = (UnaryOperator)  option.getLogicalExpression().getOperator();
                                preventiveActions.add(Collections.singletonList(contingenciesActionsDbClient.getAction(op.getActionId(), network)));
                            } else {
                                throw new AssertionError("Operator " + option.getLogicalExpression().getOperator().getClass() + " not yet supported");
                            }
                        }
                    } else {
                        LOGGER.error("Network {}: action {} not found for {} violation on equipment {}", 
                                     network.getId(), actionId , limitViolation.getLimitType(), limitViolation.getSubjectId());
                    }
                }
            }
        }
        LOGGER.info("Network {}: found {} preventive actions for {} violation on equipment {}", 
                    network.getId(), preventiveActions.size(), limitViolation.getLimitType(), limitViolation.getSubjectId());
        return preventiveActions;
    }
}
