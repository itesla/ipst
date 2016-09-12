/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.cta.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.*;
import static java.util.stream.Collectors.toList;

/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class AmplModelFactory {
    private static final Logger LOG = LoggerFactory.getLogger(AmplModelFactory.class);
    protected static final String LINESEPARATOR = System.getProperty("line.separator");
    protected static final String TABSEPARATOR = "\t";


    public AmplModel createAmplModelFrom(IpsoNetworkState completeNetworkState, IpsoProblemDefinition ipsoProblemDefinition, List<TopologicalAction> topologicalActions, Path unpluggedLogFile) {

        IpsoNetworkState ipsoNetworkState = completeNetworkState.getInterconnectedVersion(topologicalActions);
        logDisconnectedEquipment(completeNetworkState, ipsoNetworkState, unpluggedLogFile);

        AmplModel amplModel = new AmplModel(
                // Sets
                createAmplGenerators(ipsoNetworkState),
                createAmplLoads(ipsoNetworkState),
                createAmplCapacitors(ipsoNetworkState),
                createAmplNodes(ipsoNetworkState),
                createAmplSlackBuses(ipsoNetworkState),
                // Branches
                createAmplTransfBranches(ipsoNetworkState),
                createAmplSimpleBranches(ipsoNetworkState),
                createAmplCouplages(ipsoNetworkState),
                // Parameters
                createAmplNodeParameters(ipsoNetworkState, ipsoProblemDefinition),
                createAmplSlackBusesParameters(ipsoNetworkState),
                createAmplGeneratorParameters(ipsoNetworkState, ipsoProblemDefinition, topologicalActions),
                createAmplLoadParameters(ipsoNetworkState, topologicalActions),
                createAmplCapacitorParameters(ipsoNetworkState, topologicalActions),
                createAmplBranchParameters(ipsoNetworkState, ipsoProblemDefinition, topologicalActions),
                createAmplTransformerParameters(ipsoNetworkState),
                createAmplCouplingParameters(ipsoNetworkState, topologicalActions)
        );

        return amplModel;
    }

    private void logDisconnectedEquipment(IpsoNetworkState completeNetworkState, IpsoNetworkState filteredNetworkState, Path toPath) {

        try {

            Writer writer = new FileWriter(toPath.toFile());

            writeToLog(writer,
                    completeNetworkState.getIpsoNodes().stream()
                            .filter(node -> !filteredNetworkState.getIpsoNodes().contains(node))
                            .collect(toList()),
                    IpsoNode.class);

            writeToLog(writer,
                    completeNetworkState.getIpsoBanks().stream()
                            .filter(node -> !filteredNetworkState.getIpsoBanks().contains(node))
                            .collect(toList()),
                    IpsoBank.class);

            writeToLog(writer,
                    completeNetworkState.getIpsoLoads().stream()
                            .filter(node -> !filteredNetworkState.getIpsoLoads().contains(node))
                            .collect(toList()),
                    IpsoLoad.class);

            writeToLog(writer,
                    completeNetworkState.getIpsoGenerators().stream()
                            .filter(node -> !filteredNetworkState.getIpsoGenerators().contains(node))
                            .collect(toList()),
                    IpsoGenerator.class);

            writeToLog(writer,
                    completeNetworkState.getIpsoTwoWindingsTransformers().stream()
                            .filter(node -> !filteredNetworkState.getIpsoTwoWindingsTransformers().contains(node))
                            .collect(toList()),
                    IpsoTwoWindingsTransformer.class);

            writeToLog(writer,
                    completeNetworkState.getIpsoLines().stream()
                            .filter(node -> !filteredNetworkState.getIpsoLines().contains(node))
                            .collect(toList()),
                    IpsoLine.class);

            writeToLog(writer,
                    completeNetworkState.getIpsoCouplings().stream()
                            .filter(node -> !filteredNetworkState.getIpsoCouplings().contains(node))
                            .collect(toList()),
                    IpsoCoupling.class);

            writer.close();


        } catch (IOException e) {
            LOG.error("Cannot write log for disconnected equipments",e);
        }
    }

    private <T extends IpsoEquipment> void writeToLog(Writer writer, Collection<T> equipmentsToLog, Class<T> classOfEquipmentsToLog) throws IOException {
        StringBuilder builder = new StringBuilder(LINESEPARATOR);
        builder.append(classOfEquipmentsToLog.getSimpleName());
        builder.append(": ");
        builder.append(LINESEPARATOR);

        equipmentsToLog.stream()
                .forEach(equipment -> builder.append(TABSEPARATOR)
                        .append(equipment.getId())
                        .append(LINESEPARATOR));

        writer.write(builder.toString());
    }

    private List<AmplCapacitorParameters> createAmplCapacitorParameters(IpsoNetworkState ipsoNetworkState, List<TopologicalAction> topologicalActions) {
        return ipsoNetworkState.getIpsoBanks().stream()
                .map(bank -> createAmplCapacitorParameter(bank, topologicalActions))
                .collect(Collectors.toList());
    }

    private AmplCapacitorParameters createAmplCapacitorParameter(IpsoBank bank, List<TopologicalAction> topologicalActions) {
        return new AmplCapacitorParameters(
                bank.getId(),
                bank.getConnectedNode().getId(),
                bank.isConnected(),
                findReconfigurationState(bank, topologicalActions),
                0,
                bank.getMaxSteps(),
                bank.getSelectedSteps(),
                bank.getActivePöwerByStep()/100,
                bank.getReactivePowerByStep()/100
        );
    }


    private List<AmplCapacitor> createAmplCapacitors(IpsoNetworkState ipsoNetworkState) {
        return ipsoNetworkState.getIpsoBanks().stream()
                .map(IpsoBank::getId)
                .map(AmplCapacitor::new)
                .collect(Collectors.toList());
    }


    /*
    * Sets creation
    **/
    // Generators set
    private List<AmplGenerator> createAmplGenerators(IpsoNetworkState networkState) {
        return networkState.getIpsoGenerators().stream()
                .map(IpsoGenerator::getId)
                .map(AmplGenerator::new)
                .collect(toList());
    }

    // Loads set
    private List<AmplLoad> createAmplLoads(IpsoNetworkState networkState) {
        return networkState.getIpsoLoads().stream()
                .map(IpsoLoad::getId)
                .map(AmplLoad::new)
                .collect(toList());
    }

    // Nodes Set
    private List<AmplSimpleNode> createAmplNodes(IpsoNetworkState networkState) {
        return networkState.getIpsoNodes().stream()
                .map(node -> new AmplSimpleNode(node.getId()))
                .collect(toList());
    }

    // Slackbus set
    private List<AmplSlackBus> createAmplSlackBuses(IpsoNetworkState networkState) {
        return networkState.getIpsoNodes().stream()
                .filter(IpsoNode::isSlackBus)
                .distinct()
                .map(IpsoNode::getId)
                .map(AmplSlackBus::new)
                .collect(toList());
    }

    /**
     *  Branches sets
     */

    // Transformer branches
    private List<AmplTransformerBranch> createAmplTransfBranches(IpsoNetworkState ipsoNetworkState) {
        return ipsoNetworkState.getIpsoTwoWindingsTransformers().stream()
                .map(IpsoTwoWindingsTransformer::getId)
                .distinct()
                .map(AmplTransformerBranch::new)
                .collect(toList());
    }

    // Couplages
    private List<AmplSimpleBranch> createAmplSimpleBranches(IpsoNetworkState ipsoNetworkState) {
        return ipsoNetworkState.getIpsoLines().stream()
                .map(IpsoLine::getId)
                .distinct()
                .map(AmplSimpleBranch::new)
                .collect(toList());
    }

    // Slackbuses
    private List<AmplCouplage> createAmplCouplages(IpsoNetworkState ipsoNetworkState) {
        return ipsoNetworkState.getIpsoCouplings().stream()
                .map(IpsoCoupling::getId)
                .distinct()
                .map(AmplCouplage::new)
                .collect(toList());
    }

    // Nodes Parameters
    private List<AmplNodeParameter> createAmplNodeParameters(IpsoNetworkState ipsoNetworkState, IpsoProblemDefinition ipsoProblemDefinition) {
        return ipsoNetworkState.getIpsoNodes()
                .stream()
                .distinct()
                .map(ipsoNode -> createAmplNodeParameter(ipsoNode, ipsoProblemDefinition))
                .collect(toList());
    }

    private AmplNodeParameter createAmplNodeParameter(IpsoNode ipsoNode, IpsoProblemDefinition ipsoProblemDefinition) {
        Optional<IpsoConstraintNodeAcVoltageBounds> constraint = ipsoProblemDefinition.getConstraintNodeAcVoltageBounds().stream()
                .filter(containsConstraintOn(ipsoNode))
                .findFirst();

        final float vMin = getBoundMinFor(constraint);

        final float vInit = DataUtil.getSafeValueOf(ipsoNode.getVoltage() / ipsoNode.getBaseVoltage(), 0f);

        final float vMax = getBoundMaxFor(constraint);

        final float vNom = DataUtil.getSafeValueOf(ipsoNode.getBaseVoltage(), 0f);

        final float initialTH = (float)(DataUtil.getSafeValueOf(ipsoNode.getAngle(), 0f) * PI / 180);


        return new AmplNodeParameter(ipsoNode.getId(), vMin, vInit, vMax, vNom, initialTH);
    }

    // Slack Buses Parameters
    private List<AmplSlackBusParameters> createAmplSlackBusesParameters(IpsoNetworkState ipsoNetworkState) {
        return ipsoNetworkState.getIpsoNodes().stream()
                .filter(IpsoNode::isSlackBus)
                .distinct()
                .map(this::createAmplSlackBusParameter)
                .collect(Collectors.toList());
    }


    private AmplSlackBusParameters createAmplSlackBusParameter(IpsoNode sb) {
        return new AmplSlackBusParameters(sb.getId(), (float)(sb.getAngle() * PI / 180), (float)(sb.getAngle() * PI / 180));
    }

    // Generators Parameters
    private List<AmplGeneratorParameters> createAmplGeneratorParameters(IpsoNetworkState ipsoNetworkState, IpsoProblemDefinition ipsoProblemDefinition, List<TopologicalAction> topologicalActions) {
        return ipsoNetworkState.getIpsoGenerators().stream()
                .distinct()
                .map(generator -> createAmplGeneratorParameter(generator, ipsoProblemDefinition, topologicalActions))
                .collect(Collectors.toList());

    }

    private AmplGeneratorParameters createAmplGeneratorParameter(IpsoGenerator generator, IpsoProblemDefinition ipsoProblemDefinition, List<TopologicalAction> topologicalActions) {

        Optional<IpsoConstraintGeneratorQBounds> reactivePowerConstraint = ipsoProblemDefinition.getConstraintGeneratorQBounds().stream()
                .filter(containsConstraintOn(generator))
                .findFirst();

        final IpsoNode connectedNode = generator.getConnectedNode();

        final float pgMin = connectedNode.isSlackBus() ?
                Float.NEGATIVE_INFINITY :
                generator.getActivePower() / 100f;

        final float pgMax = connectedNode.isSlackBus() ?
                Float.POSITIVE_INFINITY :
                generator.getActivePower() / 100f;

        final float qgMin = connectedNode.isSlackBus() ?
                Float.NEGATIVE_INFINITY :
                (reactivePowerConstraint.isPresent() ?
                        reactivePowerConstraint.get().getBoundsMin()/100f :
                        generator.getReactivePower() / 100f);

        final float qgMax = connectedNode.isSlackBus() ?
                Float.POSITIVE_INFINITY :
                (reactivePowerConstraint.isPresent() ?
                        reactivePowerConstraint.get().getBoundsMax()/100f :
                        generator.getReactivePower() / 100f);

        return new AmplGeneratorParameters(
                generator.getId(),
                connectedNode.getId(),
                pgMin, // PGmin
                generator.getActivePower()/100f, // Initial_P
                pgMax, // PGmax
                qgMin,
                generator.getReactivePower()/100f,
                qgMax,
                generator.isConnected(),
                findReconfigurationState(generator, topologicalActions),
                1.0f,
                1000f
        );
    }

    // Loads Parameters
    private List<AmplLoadParameters> createAmplLoadParameters(IpsoNetworkState ipsoNetworkState, List<TopologicalAction> topologicalActions) {
        return ipsoNetworkState.getIpsoLoads().stream()
                .distinct()
                .map(load -> createAmplLoadParameter(load, topologicalActions))
                .collect(Collectors.toList());

    }

    private AmplLoadParameters createAmplLoadParameter(IpsoLoad load, List<TopologicalAction> topologicalActions) {
        final IpsoNode connectedNode = load.getConnectedNode();

        final float initialDL = 0f;

        final float dlMax = 1.0f;

        final float wDL = 100f;

        return new AmplLoadParameters(
                load.getId(),
                connectedNode.getId(),
                load.isConnected(),
                findReconfigurationState(load, topologicalActions),
                load.getActivePower() / 100,
                load.getReactivePower() / 100,
                initialDL,
                dlMax,
                wDL
        );
    }

    /*
    * Branch Parameters
     */
    private List<AmplBranchParameters> createAmplBranchParameters(IpsoNetworkState ipsoNetworkState, IpsoProblemDefinition ipsoProblemDefinition, List<TopologicalAction> topologicalActions) {
        List<AmplBranchParameters> result = createAmplBranchParametersFromLines(ipsoNetworkState, ipsoProblemDefinition, topologicalActions);
        result.addAll(
                createAmplBranchParametersFromTransformers(ipsoNetworkState, ipsoProblemDefinition, topologicalActions)
        );

        return result;
    }

    private List<AmplBranchParameters> createAmplBranchParametersFromTransformers(IpsoNetworkState networkState, IpsoProblemDefinition problemDefinition, List<TopologicalAction> topologicalActions) {
        return networkState.getIpsoTwoWindingsTransformers().stream()
                .distinct()
                .map(transfo -> createAmplBranchParametersFromTransformer(problemDefinition, transfo, topologicalActions))
                .collect(Collectors.toList());
    }

    private AmplBranchParameters createAmplBranchParametersFromTransformer(IpsoProblemDefinition problemDefinition, IpsoTwoWindingsTransformer transformer, List<TopologicalAction> topologicalActions) {
        Optional<IpsoConstraint2WTransformerFlow> constraint = problemDefinition.getConstraint2WTransformerFlows().stream()
                .filter(containsConstraintOn(transformer))
                .findFirst();

        int initialTapIndex = transformer.getIndexes().indexOf(transformer.getInitialTap());

        float uccForInitialTap = transformer.getUccs().get(initialTapIndex);

        float y = abs(100f/uccForInitialTap );

        float zeta = uccForInitialTap > 0 ?
                getZetaFromTransformer(uccForInitialTap, transformer.getCuLosses())
                : (float)-(PI/2);

        return new AmplBranchParameters(
                transformer.getId(),
                transformer.getIpsoNode1().getId(),
                transformer.getIpsoNode2().getId(),
                y,
                zeta,
                transformer.getFeLosses() / 100f, // g_sh
                getBShFromTransformer(transformer.getFeLosses(), transformer.getMagnetizingCurrent()), // b_sh
                transformer.isConnected(), // InitConfig
                findReconfigurationState(transformer, topologicalActions), // CanReconfig
                9999f, // P_MaxFlow
                9999f, // Q_MaxFlow
                9999f, // S_MaxFlow
                getMaxFlow(transformer.getIpsoNode1(), constraint), // I_MaxFlow_OR
                getMaxFlow(transformer.getIpsoNode2(), constraint)  // I_MaxFlow_DE
        );
    }

    private List<AmplBranchParameters> createAmplBranchParametersFromLines(IpsoNetworkState ipsoNetworkState, IpsoProblemDefinition ipsoProblemDefinition, List<TopologicalAction> topologicalActions) {
        return ipsoNetworkState.getIpsoLines().stream()
                .distinct()
                .map(line -> createAmplBranchParametersFromLine(ipsoProblemDefinition, line, topologicalActions))
                .collect(Collectors.toList());
    }

    private AmplBranchParameters createAmplBranchParametersFromLine(IpsoProblemDefinition ipsoProblemDefinition, IpsoLine line, List<TopologicalAction> topologicalActions) {
        Optional<AbstractIpsoConstraintLineFlow> constraint = ipsoProblemDefinition.getConstraintLineFlows().stream()
                .filter(containsConstraintOn(line))
                .findFirst();

        final float y = 100f / (float) sqrt(
                pow((double) line.getResistance(), 2d) +
                        pow((double) line.getReactance(), 2d));

        final float zeta = (float) atan(line.getReactance() / line.getResistance());

        return new AmplBranchParameters(
                line.getId(),
                line.getIpsoNode1().getId(),
                line.getIpsoNode2().getId(),
                y,
                zeta,
                line.getSemiConductance() / 100f, // g_sh
                line.getSemiSusceptance() / 100f, // b_sh
                line.isConnected(),
                findReconfigurationState(line, topologicalActions),
                9999f,
                9999f,
                9999f,
                getMaxFlow(line.getIpsoNode1(), constraint), // I_MaxFlow_OR
                getMaxFlow(line.getIpsoNode2(), constraint) // I_MaxFlow_DE
        );
    }

    // Transformers parameters
    private List<AmplTransformerParameters> createAmplTransformerParameters(IpsoNetworkState networkState) {
        return networkState.getIpsoTwoWindingsTransformers().stream()
                .distinct()
                .map(this::createAmplTransformerParameter)
                .collect(Collectors.toList());
    }

    private AmplTransformerParameters createAmplTransformerParameter(IpsoTwoWindingsTransformer transformer) {

        final float r = getInitialRForTransformer(transformer) * (transformer.getIpsoNode1().getBaseVoltage() / transformer.getIpsoNode2().getBaseVoltage());
        final float phaseMin = getPhaseMinForTransformer(transformer);
        final float tapMin = transformer.getLowStep();
        final float tapMax = transformer.getHighStep();
        final float phaseByTap = getPhaseByTapForTransformer(transformer);

        return new AmplTransformerParameters(
                transformer.getId(),
                r,
                r,
                r,
                transformer.getInitialTap(),
                tapMin,
                tapMax,
                phaseMin,
                phaseByTap
        );
    }

    // Coupling parameters
    private List<AmplCouplingParameters> createAmplCouplingParameters(IpsoNetworkState ipsoNetworkState, List<TopologicalAction> topologicalActions) {
        return ipsoNetworkState.getIpsoCouplings().stream()
                .distinct()
                .map(couplage -> createAmplCouplingParameter(couplage, topologicalActions))
                .collect(Collectors.toList());
    }

    private AmplCouplingParameters createAmplCouplingParameter(IpsoCoupling couplage, List<TopologicalAction> topologicalActions) {
        return new AmplCouplingParameters(
                couplage.getId(),
                couplage.getIpsoNode1().getId(),
                couplage.getIpsoNode2().getId(),
                couplage.isConnected(),
                findReconfigurationState(couplage, topologicalActions)
        );
    }

    /**
     **Auxialiary Methods
     **/

    private float getMaxFlow(IpsoNode node, Optional<? extends AbstractFlowConstraint> constraint) {
        if ( constraint.isPresent()) {
            return constraint.get().getMaxFlow() *
                    (float) sqrt(3d) *
                    DataUtil.getSafeValueOf(node.getBaseVoltage(), 0f)
                    / 100000f;
        }
        else {
            return 0f; // If not present, Vnom = 0f (default value)AbstractIpsoConstraintLineFlow.DEFAULT_BOUND_MAX
        }

    }

    private float getBoundMinFor(Optional<IpsoConstraintNodeAcVoltageBounds> constraint) {
        if ( constraint.isPresent()) {
            return constraint.get().getBoundsMin();
        }
        else {
            return IpsoConstraintNodeAcVoltageBounds.DEFAULT_BOUND_MIN;
        }
    }

    private float getBoundMaxFor(Optional<IpsoConstraintNodeAcVoltageBounds> constraint) {
        if (constraint.isPresent()) {
            return constraint.get().getBoundsMax();
        } else {
            return IpsoConstraintNodeAcVoltageBounds.DEFAULT_BOUND_MAX;
        }
    }

    private Predicate<? super IpsoConstraint> containsConstraintOn(IpsoEquipment equipment) {
        return constraint -> constraint.getRelatedIpsoEquipment().equals(equipment);
    }

    private Predicate<? super IpsoNode> notIn(List<IpsoNode> generatorNodes) {
        return ipsoNode -> !generatorNodes.contains(ipsoNode);
    }

    private float getZetaFromTransformer(Float uccForInitialTap, float cuLosses) {
        return (float) atan(sqrt(pow(uccForInitialTap, 2) - (pow(cuLosses, 2)))/ cuLosses);
    }


    private float getBShFromTransformer(float feLosses, float magnetizingCurrent) {
        return magnetizingCurrent >= feLosses ?
                (float) sqrt(pow(magnetizingCurrent, 2) - pow(feLosses, 2)) / 100f:
                0f;
    }

    private float getPhaseMinForTransformer(IpsoTwoWindingsTransformer transformer) {
        return transformer.getPhases().size() > 0 ?
                (float)(transformer.getPhases().get(0) * PI / 180):
                0f;
    }

    private float getPhaseByTapForTransformer(IpsoTwoWindingsTransformer transformer) {
        return transformer.getPhases().size() > 1 ?
                (float)((transformer.getPhases().get(1) - transformer.getPhases().get(0)) * PI / 180):
                0f;
    }

    private float getInitialRForTransformer(IpsoTwoWindingsTransformer transformer) {
        try {
            int initialTapIndex = transformer.getIndexes().indexOf(transformer.getInitialTap());
            return transformer.getVoltages_side2().get(initialTapIndex) /
                    transformer.getVoltages_side1().get(initialTapIndex);
        }
        catch(Exception e) {
            return 0f;
        }
    }

    private boolean findReconfigurationState(AbstractIpsoBranch branch, List<TopologicalAction> topologicalActions) {
        checkArgument(branch != null, "branch must not be null");
        return topologicalActions.stream()
                .filter(action -> action.getEquipmentId() == branch.getId())
                .anyMatch(isOppositeTo(branch.isConnected()));
    }

    private boolean findReconfigurationState(IpsoOneConnectionEquipment connection, List<TopologicalAction> topologicalActions) {
        checkArgument(connection != null, "connection must not be null");
        return topologicalActions.stream()
                .filter(action -> action.getEquipmentId() == connection.getId())
                .anyMatch(isOppositeTo(connection.isConnected()));
    }

    private Predicate<? super TopologicalAction> isOppositeTo(boolean connected) {
        return topologicalAction -> topologicalAction.getSwitchAction().isOppositeTo(connected);
    }

}
