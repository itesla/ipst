/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import eu.itesla_project.computation.ExecutionReport;
import eu.itesla_project.cta.converter.AmplModelFactory;
import eu.itesla_project.cta.converter.ConversionContext;
import eu.itesla_project.cta.converter.MappingBetweenIidmIdAndIpsoEquipment;
import eu.itesla_project.cta.converter.NetworkModelConverter;
import eu.itesla_project.cta.model.*;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;
import eu.itesla_project.modules.optimizer.PostContingencyState;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static eu.itesla_project.cta.service.IpsoOutputListingMessageType.INVALID_COMPONENTS;
import static eu.itesla_project.cta.service.IpsoOutputListingMessageType.VIOLATED_CONSTRAINTS;
import static org.slf4j.LoggerFactory.getLogger;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class IpsoComputationService {

    private static final Logger LOG = getLogger(IpsoComputationService.class);

    public IpsoComputationService() {}

    /**
     * create an Scopf Ipso problem definition from post contingency iidm network and Ipso network model
     * @param ipsoNetworkState
     * @param option
     * @return
     * @throws IOException
     */
    public IpsoProblemDefinition createScopfIpsoProblemDefinition(IpsoNetworkState ipsoNetworkState, IpsoOptions option, IpsoOutputListing outputListing) throws IOException {
        IpsoProblemDefinition problemDefinition = createIpsoProblemDefinitionFrom(ipsoNetworkState, option);
        outputListing.addToListing(INVALID_COMPONENTS, problemDefinition.getInvalidComponents());
        return problemDefinition;
    }

    /**
     * @return Ipso network state resulting the conversion of a iidm network
     */
    public IpsoNetworkState convertIidmNetworkToIpsoNetworkState(String optimizationId, Network network) {
        ConversionContext conversionContext = new ConversionContext(optimizationId);
        NetworkModelConverter networkModelConverter = new NetworkModelConverter();
        return networkModelConverter.convert(network, conversionContext);
    }

    /**
     *
     * @return Ipso problem definition for a given Ipso network state
     */
    public IpsoProblemDefinition createIpsoProblemDefinitionFrom(IpsoNetworkState ipsoNetworkState, IpsoOptions option) {
        IpsoProblemDefinitionFactory problemDefinitionFactory = new IpsoProblemDefinitionFactory();
        return problemDefinitionFactory.createProblemDefinitionFor(ipsoNetworkState, option);
    }


    public IpsoOutputListing createNewOutputListing(MappingBetweenIidmIdAndIpsoEquipment mappingBetweenIidmIdAndIpsoEquipment) {
        return new IpsoOutputListing(mappingBetweenIidmIdAndIpsoEquipment);

    }

    public CorrectiveActions createCorrectiveActions(PostContingencyState postContingencyState, List<IpsoConstraint> violatedConstraints, ContingenciesAndActionsDatabaseClient client, MappingBetweenIidmIdAndIpsoEquipment mappingBetweenIidmIdAndIpsoEquipment, IpsoOutputListing outputListing) {
        IpsoCorrectiveActionService correctiveActionManager = new IpsoCorrectiveActionService(postContingencyState, client, mappingBetweenIidmIdAndIpsoEquipment);
        return correctiveActionManager.createCorrectiveActionsToResolve(violatedConstraints, outputListing);
    }

    public IpsoOptimizationResults createOptimizationResultsFrom(ExecutionReport report, Path path) {
        return new IpsoOptimizationResultsService().createIpsoOptimizationResults(report, path);
    }

    public AmplModel computeNewTopologyFor(IpsoNetworkState networkState, IpsoProblemDefinition problemDefinition, List<TopologicalAction> topologicalActions, Path unpluggedLogFile) {
        checkArgument(networkState != null, "networkState must not be null");
        checkArgument(problemDefinition != null, "problemDefinition must not be null");

        return new AmplModelFactory().createAmplModelFrom(networkState, problemDefinition, topologicalActions, unpluggedLogFile);
    }

    public List<IpsoConstraint> findViolatedConstraintsFor(IpsoProblemDefinition problemDefinition, IpsoOutputListing outputListing) {
        checkArgument(problemDefinition != null, "problemDefinition must not be null");
        checkArgument(outputListing != null, "outputListing must not be null");

        List<IpsoConstraint> violatedConstraints = new IpsoProblemDefinitionService().findViolatedConstraintsIn(problemDefinition);
        addViolatedConstraintsTo(outputListing, violatedConstraints);

        return violatedConstraints;
    }

    private void addViolatedConstraintsTo(IpsoOutputListing outputListing, List<IpsoConstraint> violatedConstraints) {
        outputListing.addToListing(VIOLATED_CONSTRAINTS, violatedConstraints);
        outputListing.addSeparator();
    }
}
