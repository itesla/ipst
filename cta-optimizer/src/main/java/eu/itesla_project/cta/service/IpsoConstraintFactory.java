/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import com.google.common.collect.Lists;
import eu.itesla_project.cta.model.*;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class IpsoConstraintFactory {

    private static final Logger LOG = getLogger(IpsoConstraintFactory.class);

    private final IpsoProblemComponentFactory ipsoProblemComponentFactory;

    public IpsoConstraintFactory(IpsoProblemComponentFactory ipsoProblemComponentFactory) {
        checkArgument(ipsoProblemComponentFactory != null, "ipsoProblemComponentFactory must not be null");
        this.ipsoProblemComponentFactory = ipsoProblemComponentFactory;
    }

    /**
     * @return  create voltage AC node constraints in according to the options
     */
    public List<IpsoConstraintNodeAcVoltageBounds> createVoltageBoundsConstraints(IpsoNetworkState networkState, IpsoOptions option) {
        checkArgument(networkState != null, "networkState must not be null");
        List<IpsoConstraintNodeAcVoltageBounds> constraints = Lists.newArrayList();

        // get the list of node id that are not directly concerned by a constraint
        final List<IpsoNode> nodesRegulatedByGenerators = networkState.getConnectedAndRegulatingGenerators()
                .map(IpsoGenerator::getConnectedNode)
                .collect(Collectors.toList());

        final List<IpsoNode> nodesRegulatedByTransformers = networkState.getConnectedAndRegulatingRatioTapChangerTransformer()
                .map(transformer -> transformer.getRegulationParameters().getRegulatedNode())
                .collect(Collectors.toList());

        if (option.isGeneratorSetpointTakenIntoAccount()) {

            List<IpsoConstraintNodeAcVoltageBounds> constraintsFromGenerators = createVoltageConstraintsForNodesControlledByGenerators(networkState);
            // add voltage setpoint constraints for nodes controlled by generators
            constraints.addAll(constraintsFromGenerators);

            List<IpsoConstraintNodeAcVoltageBounds> constraintsFromTransformers = createVoltageConstraintsForNodesControlledByTransformer(networkState);
            // add voltage setpoint constraints for nodes controlled by transformer
            constraints.addAll(constraintsFromTransformers);
        }

        if (option.isVoltageLimitsTakenIntoAccount()) {
            // create voltage bound constraints for no regulated nodes
            List<IpsoConstraintNodeAcVoltageBounds> constraintsFromNodes = createVoltageConstraintsForNotRegulatedNodes(
                    networkState, nodesRegulatedByGenerators, nodesRegulatedByTransformers);
            // add voltage bound constraint
            constraints.addAll(constraintsFromNodes);
        }

        return constraints;
    }

    private List<IpsoConstraintNodeAcVoltageBounds> createVoltageConstraintsForNotRegulatedNodes(IpsoNetworkState networkState, List<IpsoNode> nodesRegulatedByGenerators, List<IpsoNode> nodesRegulatedByTransformers) {
        return networkState.getIpsoNodes()
                .stream()
                .filter(node -> !nodesRegulatedByGenerators.contains(node))
                .filter(node -> !nodesRegulatedByTransformers.contains(node))
                .map(node -> ipsoProblemComponentFactory.createConstraintNodeAcVoltageBounds(
                        node,
                        VoltageUnit.PU,
                        node.getMinVoltageLimit(),
                        node.getMaxVoltageLimit(),
                        networkState.getWorld()))
                .collect(toList());
    }

    private List<IpsoConstraintNodeAcVoltageBounds> createVoltageConstraintsForNodesControlledByTransformer(IpsoNetworkState networkState) {
        return networkState.getConnectedAndRegulatingRatioTapChangerTransformer()
                .map(transformer -> ipsoProblemComponentFactory.createConstraintNodeAcVoltageBounds(
                        transformer.getRegulationParameters().getRegulatedNode(),
                        VoltageUnit.PU,
                        transformer.getRegulationParameters().getSetpoint(),
                        transformer.getRegulationParameters().getSetpoint(),
                        networkState.getWorld()))
                .collect(toList());
    }

    private List<IpsoConstraintNodeAcVoltageBounds> createVoltageConstraintsForNodesControlledByGenerators(IpsoNetworkState networkState) {
        return networkState.getConnectedAndRegulatingGenerators()
                .map(generator -> ipsoProblemComponentFactory.createConstraintNodeAcVoltageBounds(
                        generator.getConnectedNode(),
                        VoltageUnit.PU,
                        generator.getVoltageSetpointPu(),
                        generator.getVoltageSetpointPu(),
                        networkState.getWorld()))
                .collect(toList());
    }

    /**
     * @return  angle constraints on AC node
     */
    public List<IpsoConstraintNodeAcAngleBounds> createNodeAcAngleBoundConstraints(IpsoNetworkState networkState) {
        IpsoNode sb = findSlackBusIn(networkState);
        IpsoConstraintNodeAcAngleBounds constraint = ipsoProblemComponentFactory.createConstraintNodeAcAngleBounds(sb, sb.getAngle(), sb.getAngle(), networkState.getWorld());
        return Lists.newArrayList(constraint);
    }

    /**
     * @return  Active Power constraints on generators
     */
    public List<IpsoConstraintGeneratorPBounds> createActivePowerBoundsConstraints(IpsoNetworkState networkState) {

        IpsoNode slackbusNode = findSlackBusIn(networkState);

        IpsoGenerator generatorConnectedToSlackbus = networkState.getIpsoGenerators()
                .stream()
                .filter(generator -> areConnected(generator, slackbusNode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("There is no generatorConnectedToSlackbus linked to the Node [slackbus]"));

        IpsoConstraintGeneratorPBounds constraintProductionPBounds = ipsoProblemComponentFactory.createConstraintProductionPBounds(
                generatorConnectedToSlackbus,
                generatorConnectedToSlackbus.getMinActivePower(),
                generatorConnectedToSlackbus.getMaxActivePower(),
                networkState.getWorld());

        return Lists.newArrayList(constraintProductionPBounds);
    }

    /**
     * @return  Reactive Power constraints on generators
     */
    public List<IpsoConstraintGeneratorQBounds> createReactivePowerBoundsConstraints(IpsoNetworkState networkState) {
        checkArgument(networkState != null, "networkState must not be null");
        return  networkState.getConnectedAndRegulatingGenerators()
                .filter(IpsoGenerator::isDeltaQLimitUpperThanOneMvar)
                .map(generator -> ipsoProblemComponentFactory.createConstraintProductionQBounds(
                        generator,
                        generator.getMinReactivePower(),
                        generator.getMaxReactivePower(),
                        networkState.getWorld()))
                .collect(toList());
    }

    /**
     * @return  Flow constraints on lines
     */
    public List<AbstractIpsoConstraintLineFlow> createLineFlowConstraints(IpsoNetworkState networkState, IpsoOptions option) {
        checkArgument(networkState != null, "networkState must not be null");

        if (option.isVoltageLimitsTakenIntoAccount()) {
            return networkState.getIpsoLines().stream()
                    .filter(IpsoLine::hasMaxCurrentPermanentLimitDefined)
                    .flatMap(line -> createConstraintLineFlowOnBothSides(line, networkState.getWorld()).stream())
                    .collect(toList());
        }else {
            return Lists.newArrayList();
        }
    }

    private List<AbstractIpsoConstraintLineFlow> createConstraintLineFlowOnBothSides(IpsoLine line, int world) {
        AbstractIpsoConstraintLineFlow constraintSide1 = ipsoProblemComponentFactory.createConstraintLineFlow(line, FlowType.SIDE1, world);
        AbstractIpsoConstraintLineFlow constraintSide2 = ipsoProblemComponentFactory.createConstraintLineFlow(line, FlowType.SIDE2, world);
        //return Lists.newArrayList(constraintSide1, constraintSide2);
        // TODO Don't forget to restore 'constraintSide2' when Ipso API will support it
        return Lists.newArrayList(constraintSide1);
    }

    public List<IpsoConstraint2WTransformerTapBounds> createTwoWindingTransformerTapBoundsConstraints(IpsoNetworkState networkState, IpsoOptions option) {
        checkArgument(networkState != null, "networkState must not be null");

        if (option.isTransformerRegulateTakenIntoAccount()) {
            return networkState.getIpsoTwoWindingsTransformers().stream()
                    .filter(IpsoTwoWindingsTransformer::isConnectedOnBothSides)
                    .filter(IpsoTwoWindingsTransformer::isRegulating)
                    .filter(IpsoTwoWindingsTransformer::hasMoreThanOneStep)
                    .map(ipsoTransformer -> ipsoProblemComponentFactory.createConstraint2WTfoTap(
                            ipsoTransformer,
                            ipsoTransformer.getLowStep(),
                            ipsoTransformer.getHighStep(),
                            networkState.getWorld()))
                    .collect(toList());
        }
        else {
            return Lists.newArrayList();
        }
    }

    public List<IpsoConstraint2WTransformerFlow> createTwoWindingTransformerFlowConstraints(IpsoNetworkState networkState, IpsoOptions option) {
        checkArgument(networkState != null, "networkState must not be null");
        List<IpsoConstraint2WTransformerFlow> constraints = Lists.newArrayList();

        // get all regulating phase tap transformer with setpoint defined ..
        List<IpsoTwoWindingsTransformer> transformerPtcWithSetpoint = networkState.getConnectedAndRegulatingPhaseTapChangerTransformer()
                .filter(transformer -> transformer.getRegulationParameters().hasSetpointDefined())
                .collect(toList());

        if (option.isTransformerSetpointTakenIntoAccount()) {
            // and create a constraint for all of them
            List<IpsoConstraint2WTransformerFlow> setpointConstraints = transformerPtcWithSetpoint.stream()
                    .map(ipsoTransformer -> ipsoProblemComponentFactory.createConstraint2WTransformerFlow(
                            ipsoTransformer,
                            FlowUnit.AMPERE,
                            ipsoTransformer.getRegulationParameters().getSetpoint(),
                            ipsoTransformer.getRegulationParameters().getSetpoint(),
                            networkState.getWorld()))
                    .collect(toList());

            constraints.addAll(setpointConstraints);
        }

        if (option.isVoltageLimitsTakenIntoAccount()) {
            // create flow limit constraint for all other transformers
            List<IpsoConstraint2WTransformerFlow> maxCurrentLimitconstraints = networkState.getIpsoTwoWindingsTransformers().stream()
                    .filter(IpsoTwoWindingsTransformer::isConnectedOnBothSides)
                            //.filter(IpsoTwoWindingsTransformer::hasMoreThanOneStep)
                    .filter(transformer -> !transformerPtcWithSetpoint.contains(transformer))
                    .map(ipsoTransformer -> ipsoProblemComponentFactory.createConstraint2WTransformerFlow(
                            ipsoTransformer,
                            FlowUnit.AMPERE,
                            IpsoProblemComponentFactory.DEFAULT_BRANCH_FLOW_MIN,
                            ipsoTransformer.getMaxCurrentPermanentLimit(),
                            networkState.getWorld()))
                    .collect(toList());

            constraints.addAll(maxCurrentLimitconstraints);
        }


        return constraints;
    }

    public List<IpsoConstraintBankStepBounds> createBankStepBoundsConstraints(IpsoNetworkState networkState) {
        return Lists.newArrayList();
    }

    private IpsoNode findSlackBusIn(IpsoNetworkState networkState) {
        return networkState.getIpsoNodes()
                .stream()
                .filter(IpsoNode::isSlackBus)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("There is no slack bus"));
    }

    /**
     * @return true iff the generator is connected to the node
     */
    protected boolean areConnected(IpsoGenerator ipsoGenerator, IpsoNode ipsoNode) {
        checkArgument(ipsoGenerator != null, "ipsoGenerator must not be null");
        checkArgument(ipsoNode != null, "ipsoNode must not be null");
        checkArgument(ipsoGenerator.getConnectedNode() != null, "ipsoGenerator.getConnectedNode() must not be null");
        return ipsoGenerator.getConnectedNode().equals(ipsoNode);
    }
}

