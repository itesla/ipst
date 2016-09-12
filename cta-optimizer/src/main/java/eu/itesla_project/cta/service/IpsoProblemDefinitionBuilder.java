/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import com.google.common.collect.Lists;
import eu.itesla_project.cta.model.*;

import java.util.List;

import static eu.itesla_project.cta.model.ValidationType.DELTA_Q_LIMIT_LOWER_THAN_ONE_MVAR;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
final class IpsoProblemDefinitionBuilder {

    private final IpsoConstraintFactory ipsoConstraintFactory;
    private final IpsoControlVariableFactory ipsoControlVariableFactory;
    private final IpsoInvalidIpsoComponentFactory ipsoInvalidComponentFactory;
    private final String name;
    private final int world;
    private final IpsoOptions option;

    private List<IpsoConstraintNodeAcVoltageBounds> constraintNodeAcVoltageBoundses;
    private List<IpsoConstraintNodeAcAngleBounds> constraintNodeAcAngleBounds;
    private List<IpsoConstraintGeneratorPBounds> constraintGeneratorPBounds;
    private List<IpsoConstraintGeneratorQBounds> constraintGeneratorQBounds;
    private List<AbstractIpsoConstraintLineFlow> constraintLineFlow;
    private List<IpsoConstraint2WTransformerTapBounds> constraint2WTransformerTaps;
    private List<IpsoConstraint2WTransformerFlow> constraint2WTransformerFlows;
    private List<IpsoConstraintBankStepBounds> constraintBankStepBounds;
    private List<IpsoControlVariableProductionP> variableGeneratorPs;
    private List<IpsoControlVariableProductionQ> variableGeneratorQs;
    private List<IpsoControlVariable2WTransformerTap> variable2WTransformerTaps;
    private List<IpsoControlVariableGeneratorStatism> variableGeneratorStatisms;
    private List<IpsoControlVariableBankStep> variableBankSteps;
    private List<IpsoInvalidComponent> invalidIpsoComponents;

    /**
     * @return a new IpsoProblemDefinitionBuilder
     */
    private IpsoProblemDefinitionBuilder(String name, int world, IpsoConstraintFactory ipsoConstraintFactory, IpsoControlVariableFactory ipsoControlVariableFactory, IpsoInvalidIpsoComponentFactory ipsoInvalidComponentFactory, IpsoOptions option) {
        this.name = name;
        this.world = world;
        this.ipsoConstraintFactory = ipsoConstraintFactory;
        this.ipsoControlVariableFactory = ipsoControlVariableFactory;
        this.ipsoInvalidComponentFactory = ipsoInvalidComponentFactory;
        this.invalidIpsoComponents = Lists.newArrayList();
        this.option = option;
    }

    public static IpsoProblemDefinitionBuilder create(String name, int world, IpsoOptions option) {
        IpsoProblemComponentFactory ipsoProblemComponentFactory = new IpsoProblemComponentFactory();
        return new IpsoProblemDefinitionBuilder(
                name,
                world,
                new IpsoConstraintFactory(ipsoProblemComponentFactory),
                new IpsoControlVariableFactory(ipsoProblemComponentFactory),
                new IpsoInvalidIpsoComponentFactory(),
                option);
    }

    public IpsoProblemDefinitionBuilder addVoltageBoundsConstraints(IpsoNetworkState networkState) {
        constraintNodeAcVoltageBoundses = ipsoConstraintFactory.createVoltageBoundsConstraints(networkState, option);
        return this;
    }

    public IpsoProblemDefinitionBuilder addAngleBoundsConstraints(IpsoNetworkState networkState) {
        constraintNodeAcAngleBounds = ipsoConstraintFactory.createNodeAcAngleBoundConstraints(networkState);
        return this;
    }

    public IpsoProblemDefinitionBuilder addActivePowerBoundsConstraints(IpsoNetworkState networkState) {
        constraintGeneratorPBounds = ipsoConstraintFactory.createActivePowerBoundsConstraints(networkState);
        return this;
    }

    public IpsoProblemDefinitionBuilder addReactivePowerBoundsConstraints(IpsoNetworkState networkState) {
        constraintGeneratorQBounds = ipsoConstraintFactory.createReactivePowerBoundsConstraints(networkState);
        invalidIpsoComponents.addAll(ipsoInvalidComponentFactory.createInvalidComponentsThatHave(DELTA_Q_LIMIT_LOWER_THAN_ONE_MVAR, IpsoConstraintGeneratorQBounds.class, networkState));
        return this;
    }

    public IpsoProblemDefinitionBuilder addLineFlowConstraints(IpsoNetworkState networkState) {
        constraintLineFlow = ipsoConstraintFactory.createLineFlowConstraints(networkState, option);
        return this;
    }

    public IpsoProblemDefinitionBuilder addTwoWindingTransformerTapBoundsConstraints(IpsoNetworkState networkState) {
        constraint2WTransformerTaps = ipsoConstraintFactory.createTwoWindingTransformerTapBoundsConstraints(networkState, option);
        return this;
    }

    public IpsoProblemDefinitionBuilder addTwoWindingTransformerFowConstraints(IpsoNetworkState networkState) {
        constraint2WTransformerFlows = ipsoConstraintFactory.createTwoWindingTransformerFlowConstraints(networkState, option);
        return this;
    }

    public IpsoProblemDefinitionBuilder addBankStepBoundsConstraints(IpsoNetworkState networkState) {
        constraintBankStepBounds = ipsoConstraintFactory.createBankStepBoundsConstraints(networkState);
        return this;
    }

    public IpsoProblemDefinitionBuilder addTwoWindingTransformerTapVariables(IpsoNetworkState networkState) {
        variable2WTransformerTaps = ipsoControlVariableFactory.createTwoWindingTransformerTapVariables(networkState, option);
        return this;
    }

    public IpsoProblemDefinitionBuilder addActiveProductionVariables(IpsoNetworkState networkState) {
        variableGeneratorPs = ipsoControlVariableFactory.createActiveProductionVariables(networkState);
        return this;
    }

    public IpsoProblemDefinitionBuilder addReactiveProductionVariables(IpsoNetworkState networkState) {
        variableGeneratorQs = ipsoControlVariableFactory.createReactiveProductionVariables(networkState);
        invalidIpsoComponents.addAll(ipsoInvalidComponentFactory.createInvalidComponentsThatHave(DELTA_Q_LIMIT_LOWER_THAN_ONE_MVAR, IpsoControlVariableProductionQ.class, networkState));
        return this;
    }

    public IpsoProblemDefinitionBuilder addStatismGeneratorVariables(IpsoNetworkState networkState) {
        variableGeneratorStatisms = ipsoControlVariableFactory.createGeneratorStatismVariables(networkState);
        return this;
    }

    public IpsoProblemDefinitionBuilder addBankStepVariables(IpsoNetworkState networkState) {
        variableBankSteps = ipsoControlVariableFactory.createBankStepVariables(networkState);
        return this;
    }

    public IpsoProblemDefinition createIpsoProblemDefinition() {
        return new IpsoProblemDefinition(
                this.name,
                this.world,
                variableGeneratorPs,
                variableGeneratorQs,
                variableGeneratorStatisms,
                variableBankSteps,
                variable2WTransformerTaps,
                constraintLineFlow,
                constraint2WTransformerFlows,
                constraintNodeAcVoltageBoundses,
                constraintNodeAcAngleBounds,
                constraintGeneratorPBounds,
                constraintGeneratorQBounds,
                constraint2WTransformerTaps,
                constraintBankStepBounds,
                invalidIpsoComponents);
    }

}

