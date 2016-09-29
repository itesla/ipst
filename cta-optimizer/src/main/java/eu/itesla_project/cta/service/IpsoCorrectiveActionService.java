/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import com.google.common.base.Strings;
import eu.itesla_project.cta.converter.MappingBetweenIidmIdAndIpsoEquipment;
import eu.itesla_project.cta.model.*;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.modules.contingencies.*;
import eu.itesla_project.modules.optimizer.PostContingencyState;

import java.math.BigInteger;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Lists.newArrayList;
import static eu.itesla_project.cta.service.IpsoOutputListingMessageType.*;
import static eu.itesla_project.modules.contingencies.ActionElementType.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.toList;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class IpsoCorrectiveActionService {

    private static final List<ActionElementType> TYPE_OF_IIDM_ACTIONS_SUPPORTED_BY_IPSO = newArrayList(SWITCH_OPEN, SWITCH_CLOSE, LINE_TRIPPING, TAP_CHANGE, GENERATION_REDISPATCHING);
    private static final List<Class<? extends IpsoConstraint>> FLOW_CONSTRAINT_TYPES = newArrayList(
            IpsoConstraint2WTransformerFlow.class,
            IpsoConstraintLineFlowSide1.class,
            IpsoConstraintLineFlowSide2.class,
            AbstractIpsoConstraintLineFlow.class);

    private final ContingenciesAndActionsDatabaseClient client;
    private final IpsoCorrectiveActionsFactory correctiveActionsFactory;
    private final MappingBetweenIidmIdAndIpsoEquipment mappingIidmIpso;
    private PostContingencyState postContingencyState;

    public IpsoCorrectiveActionService(PostContingencyState postContingencyState, ContingenciesAndActionsDatabaseClient client,
                                       MappingBetweenIidmIdAndIpsoEquipment mappingIidmIpso) {
        checkArgument(client != null, "client must not be null");
        checkArgument(postContingencyState != null, "postContingencyState must not be null");
        checkArgument(mappingIidmIpso != null, "mappingIidmIpso must not be null");
        this.client = client;
        this.postContingencyState = postContingencyState;
        this.mappingIidmIpso = mappingIidmIpso;
        this.correctiveActionsFactory = new IpsoCorrectiveActionsFactory();
    }

    /**
     * Create corrective actions to resolve violated constraints
     * @return a list of corrective actions
     */
    public CorrectiveActions createCorrectiveActionsToResolve(List<IpsoConstraint> violatedConstraints, IpsoOutputListing outputListing) {
        checkArgument(violatedConstraints != null, "violatedConstraints must not be null");

        // find associations corresponding to violated constraints
        final List<ActionsContingenciesAssociation> associations = findAssociationsWhichContain(violatedConstraints);
        checkState(associations != null, "associations must not be null");

        if (!associations.isEmpty()) {

            // For the moment we consider only one contingency and so only one assocation (fixme)
            ActionsContingenciesAssociation association = getFirst(associations, null);
            addAssoctiationIdsTo(outputListing, associations);

            // find action plans
            List<ActionPlan> actionPlans = findActionPlansIn(association); // return always one action plan for the moment (fixme)
            addActionPlanIdsTo(outputListing, actionPlans);

            String actionPlanId = getCandidateActionPlanIdFor(actionPlans);

            // find all actions from action plan or directly from the association if no action plan is defined
            List<Action> actions = !actionPlans.isEmpty()
                    ? findLowerPriorityActionsFor(actionPlans)
                    : findActionFrom(association);

            addActionIdsTo(outputListing, actions);

            // create corrective actions
            return createCorrectiveActionsFrom(actions, outputListing);

        } else {
            outputListing.addToListing(NO_ASSOCIATION_FOUND);
            return new CorrectiveActions();
        }
    }

    private String getCandidateActionPlanIdFor(List<ActionPlan> actionPlans) {
        return actionPlans.isEmpty() ? "" : actionPlans.stream().map(action -> action.getName()).findFirst().orElse("");
    }

    // Find action if action not in an ActionPlan
    private List<Action> findActionFrom(ActionsContingenciesAssociation association) {
        List<Action> actions = new ArrayList<>();
        Optional<String> actionId = ofNullable(getFirst(association.getActionsId(), null));
        if (actionId.isPresent()) {
            actions.add(client.getAction(actionId.get(), getPostContingencyNetwork()));
        }
        return actions;
    }

    // TODO Action plan Id, List<Action>, ...
    private CorrectiveActions createCorrectiveActionsFrom(List<Action> actions, IpsoOutputListing outputListing) {
        List<IpsoProblemDefinitionElement> elements = createIpsoProblemDefinitionElementsFrom(actions);

        List<IpsoControlVariable> variables = elements.stream()
                .filter(IpsoProblemDefinitionElement::isVariable)
                .map(IpsoControlVariable.class::cast)
                .collect(toList());

        List<IpsoConstraint> constraints = elements.stream()
                .filter(IpsoProblemDefinitionElement::isConstraint)
                .map(IpsoConstraint.class::cast)
                .collect(toList());

        List<TopologicalAction> topologicalActions =
                actions.stream()
                        .map(this::createTopologicalActionsFrom)
                        .flatMap(List::stream)
                        .collect(toList());

        // complete output listing
        addCorrectivesActionsTo(outputListing, variables, constraints, topologicalActions);
        return new CorrectiveActions(variables, constraints, topologicalActions);
    }

    private List<ActionPlan> findActionPlansIn(ActionsContingenciesAssociation association) {
        checkArgument(association != null, "association must not be null");
        return association.getActionsId().stream()
                .map(client::getActionPlan)
                .filter(Objects::nonNull)
                .limit(1)
                .collect(toList());
    }

    private List<Action> findLowerPriorityActionsFor(List<ActionPlan> actionPlans) {
        return actionPlans.stream()
                .filter(Objects::nonNull)
                .flatMap(actionPlan -> findLowerPriorityActionsFor(actionPlan).stream())
                .collect(toList());
    }


    private List<Action> findLowerPriorityActionsFor(ActionPlan actionPlan) {
        checkArgument(actionPlan != null, "actionPlan must not be null");
        Optional<ActionPlanOption> actionPlanOption = findActionPlanOptionOfLowerPriority(actionPlan);
        return actionPlanOption
                .map(option -> option.getActions().values()
                        .stream()
                        .map(id -> client.getAction(id, getPostContingencyNetwork()))
                        .collect(toList()))
                .orElse(newArrayList());
    }

    private Optional<ActionPlanOption> findActionPlanOptionOfLowerPriority(ActionPlan actionPlan) {
        checkArgument(actionPlan != null, "action plan must not be null");
        final Map<BigInteger, ActionPlanOption> actionPlanOptionMap = actionPlan.getPriorityOption();
        if ( actionPlanOptionMap.size() > 0 ) {
            // get lower priority
            final BigInteger key = Collections.min(actionPlanOptionMap.keySet());
            return of(actionPlanOptionMap.get(key));
        }
        else {
            return empty();
        }
    }

    private List<IpsoProblemDefinitionElement> createIpsoProblemDefinitionElementsFrom(List<Action> actions) {
        checkArgument(actions != null, "actions must not be null");
        return actions.stream()
                .map(Action::getElements)
                .flatMap(Collection::stream)
                .map(this::createIpsoProblemDefinitionElementsFrom)
                .flatMap(Collection::stream)
                .collect(toList());
    }

    /**
     *
     * @return list of new Ipso problem definition elements (such as IpsoVariable or IpsoConstraint)
     */
    private List<IpsoProblemDefinitionElement> createIpsoProblemDefinitionElementsFrom(ActionElement actionElement) {
        checkArgument(actionElement != null, "actionElement must not be null");

        if (actionElement instanceof  GenerationRedispatching ) {
            // potentially more than one generator are concerned
            GenerationRedispatching generationRedispatching = (GenerationRedispatching) actionElement;
            return generationRedispatching.getGeneratorIds().stream()
                    .filter(mappingIidmIpso::containsIpsoEquipmentFor)
                    .map(id -> mappingIidmIpso.getIpsoEquipmentFor(id).get())
                    .map(correctiveActionsFactory::createProblemDefinitionElementsFor)
                    .flatMap(List::stream)
                    .collect(toList());
        } else if (actionElement instanceof TapChangeAction) {
            return mappingIidmIpso.getIpsoEquipmentFor(actionElement.getEquipmentId())
                    .map(correctiveActionsFactory::createProblemDefinitionElementsFor)
                    .orElse(newArrayList());
        } else {
            // action element not supported by Ipso
            return newArrayList();
        }
    }

    private List<TopologicalAction> createTopologicalActionsFrom(Action action) {
        checkArgument(action != null, "actionElement must not be null");

        List<TopologicalAction> topologicalActions = action.getElements().stream()
                .filter(this::thatCorrespondsToABranchOpeningAction)
                .filter(this::thatReferencesAValidEquipment)
                .map(this::findRelatedIpsoEquipmentId)
                .map(id -> new TopologicalAction(id, SwitchAction.OPEN, action.getId()))
                .collect(toList());

        topologicalActions.addAll(
                action.getElements().stream()
                        .filter(this::thatCorrespondsToABranchClosingAction)
                        .filter(this::thatReferencesAValidEquipment)
                        .map(this::findRelatedIpsoEquipmentId)
                        .map(id -> new TopologicalAction(id, SwitchAction.CLOSE, action.getId()))
                        .collect(toList())
        );

        return topologicalActions;
    }

    private boolean thatCorrespondsToABranchClosingAction(ActionElement actionElement) {
        return actionElement instanceof SwitchClosingAction;
    }

    private boolean thatCorrespondsToABranchOpeningAction(ActionElement actionElement) {
        return actionElement instanceof SwitchOpeningAction ||
                actionElement instanceof LineTrippingAction;
    }

    private String findRelatedIpsoEquipmentId(ActionElement actionElement) {
        checkArgument(actionElement != null, "actionElement must not be null");
        checkArgument(!Strings.isNullOrEmpty(actionElement.getEquipmentId()), "actionElement.getEquipmentId() must not be null or empty");
        return mappingIidmIpso.getIpsoEquipmentFor(actionElement.getEquipmentId())
                .map(IpsoComponent::getId)
                .orElse("unknownIpsoId");
    }

    /**
     * @return true if the Iidm equipment id is existing in the mapping Iidm-Ipso
     */
    private boolean thatReferencesAValidEquipment(ActionElement actionElement) {
        return mappingIidmIpso.containsIpsoEquipmentFor(actionElement.getEquipmentId());
    }

    private void addUnsupportedActionsTo(IpsoOutputListing outputListing, List<Action> actions) {
        List<ActionElement> unSupportedElements = actions.stream()
                .flatMap(action -> action.getElements().stream())
                .filter(IpsoCorrectiveActionService::isNotSupportedByIpso)
                .collect(toList());
        outputListing.addToListing(UNSUPPORTED_CORRECTIVE_ACTIONS, unSupportedElements.stream().map(this::stringDescriptionOf).collect(toList()));
    }

    private List<ActionsContingenciesAssociation> findAssociationsWhichContain(List<IpsoConstraint> violatedConstraints) {
        checkArgument(violatedConstraints != null, "violatedConstraints must not be null");
        return client.getActionsCtgAssociations().stream()
                .filter(association -> thatContainsContingency(getContingency(), association))
                .filter(association -> thatMatchConstraintsWith(violatedConstraints, association))
                .collect(toList());
    }

    private Contingency getContingency() {

        return postContingencyState.getContingency();
    }

    private boolean thatContainsContingency(Contingency contingency, ActionsContingenciesAssociation actionsContingenciesAssociation) {
        if (contingency != null ) {
            return actionsContingenciesAssociation.getContingenciesId().stream()
                    .anyMatch(id -> id.equals(contingency.getId()));
        } else {
            return false;
        }
    }

    /**
     * @return true if the association matches withe the violated constraints of Ipso
     */
    private boolean thatMatchConstraintsWith(List<IpsoConstraint> violatedConstraints, ActionsContingenciesAssociation association) {

        if (association.getConstraints().size() == 0 ) {
            // if no constraint defined int the association we consider the first violated flow constraint
            return violatedConstraints.stream()
                    .anyMatch(c -> isAFlowConstraint(c));
        }
        else {
            // if constraints are defined in the association, we check if one of them is found in the list of ipsoConstraint
            List<String> iidmEquipmentIds = association.getConstraints().stream()
                    .map(Constraint::getEquipment)
                    .collect(toList());

            return violatedConstraints.stream()
                    .anyMatch(constraint -> containsId(constraint.getRelatedIpsoEquipment().getIidmId(), iidmEquipmentIds));
        }
    }

    private boolean isAFlowConstraint(final IpsoConstraint constraint) {
        return FLOW_CONSTRAINT_TYPES.stream()
                .anyMatch( aClass -> aClass.equals(constraint.getClass()));
    }

    private boolean containsId(String iidmId, List<String> iidmIds) {
        return iidmIds.contains(iidmId);
    }

    private static boolean isNotSupportedByIpso(ActionElement actionElement) {
        return !TYPE_OF_IIDM_ACTIONS_SUPPORTED_BY_IPSO.contains(actionElement.getType());
    }

    private String stringDescriptionOf(ActionElement n) {
        return String.format("%s - %s", n.getEquipmentId(), n.getType().name());
    }

    /*** Output listing ***/

    private void addAssoctiationIdsTo(IpsoOutputListing outputListing, List<ActionsContingenciesAssociation> associations) {
        outputListing.addToListing(ASSOCIATION_FOUND,
                associations.stream()
                        .map(ActionsContingenciesAssociation::getActionsId)
                        .collect(toList())
        );
    }


    private void addActionPlanIdsTo(IpsoOutputListing outputListing, List<ActionPlan> actionPlans) {
        outputListing.addToListing(ACTION_PLAN_FOUND,
                actionPlans.stream()
                        .filter(Objects::nonNull)
                        .map(ActionPlan::getName)
                        .collect(toList())
        );
    }

    private void addActionIdsTo(IpsoOutputListing outputListing, List<Action> actions) {
        outputListing.addToListing(ACTION_FOUND,
                actions.stream()
                        .map(Action::getId)
                        .collect(toList())
        );
    }

    private void addCorrectivesActionsTo(IpsoOutputListing outputListing, List<IpsoControlVariable> variables, List<IpsoConstraint> constraints, List<TopologicalAction> topologicalActions) {
        outputListing.addToListing(NEW_CORRECTIVE_CONTROLVARIABLES, variables);
        outputListing.addToListing(NEW_CORRECTIVE_CONSTRAINTS, constraints);
        outputListing.addToListing(NEW_TOPOLOGICAL_ACTIONS, topologicalActions);
    }

    private Network getPostContingencyNetwork() {
        return postContingencyState.getNetwork();
    }
}
