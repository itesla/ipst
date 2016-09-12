/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import eu.itesla_project.computation.CommandExecutor;
import eu.itesla_project.computation.ComputationManager;
import eu.itesla_project.cta.model.AmplModel;
import eu.itesla_project.cta.model.IpsoConstraint;
import eu.itesla_project.cta.model.IpsoNetworkState;
import eu.itesla_project.cta.model.IpsoProblemDefinition;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;
import eu.itesla_project.modules.optimizer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static eu.itesla_project.cta.service.IpsoOptimizerConfiguration.createOptimizationConfiguration;
import static java.util.stream.Collectors.toList;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class CorrectiveControlIpsoOptimizer implements CorrectiveControlOptimizer {

	Logger logger = LoggerFactory.getLogger(CorrectiveControlIpsoOptimizer.class);
	
    private final ContingenciesAndActionsDatabaseClient client;
    private final ComputationManager computationManager;
    private IpsoComputationService computationService;
    private IpsoOptions options;
    private IpsoOptimizerConfiguration configuration;

    public CorrectiveControlIpsoOptimizer(ContingenciesAndActionsDatabaseClient client, ComputationManager computationManager) {
        this.client = client;
        this.computationManager = computationManager;
    }

    public void init(CorrectiveControlOptimizerParameters parameters, IpsoOptimizerConfiguration configuration) {

        this.computationService = new IpsoComputationService();
        this.options = IpsoOptions.createOptionsForScOpf();
        this.configuration = configuration;
    }

    @Override
    public void init(CorrectiveControlOptimizerParameters parameters) {
        this.init(parameters, createOptimizationConfiguration());
    }

    @Override
    public CorrectiveControlOptimizerResult run(PostContingencyState postContingencyState) throws Exception {
        checkArgument(postContingencyState != null, "postContingencyState must not be null");
        checkArgument(postContingencyState.getStateId() != null, "postContingencyState.getStateId() must not be null");
        checkArgument(postContingencyState.getContingency() != null, "postContingencyState.getContingency() must not be null");
        checkArgument(postContingencyState.getNetwork() != null, "postContingencyState.getNetwork() must not be null");

        logger.info("Running IPSO on contingency {} and state {} of {} network", 
        			postContingencyState.getContingency().getId(), 
        			postContingencyState.getStateId(),
        			postContingencyState.getNetwork().getId());
        
        // Conversion from iidm network into ipso network
        IpsoNetworkState ipsoNetworkState = computationService
                .convertIidmNetworkToIpsoNetworkState(
                        postContingencyState.getNetwork().getName(),
                        postContingencyState.getNetwork());

        // create the onput listing
        IpsoOutputListing outputListing = createOutputListing(ipsoNetworkState);
        outputListing.addContingency(postContingencyState.getContingency());

        IpsoProblemDefinition problemDefinition = computationService.createScopfIpsoProblemDefinition(
                ipsoNetworkState,
                options,
                outputListing);

        // Looking for violated constraints
        List<IpsoConstraint> violatedConstraints = computationService.findViolatedConstraintsFor(problemDefinition, outputListing);
        // Remove voltage constraints
        violatedConstraints = violatedConstraints.stream()
                .filter(IpsoConstraint::isCurrentViolation)
                .collect(toList());

        OptimizerExecutionService executionService = new OptimizerExecutionService();

        String contingencyId = postContingencyState.getContingency().getId();
        if (!violatedConstraints.isEmpty() && options.isActionCorrectivesTakenIntoAccount()) {
        	logger.info("{} constraints violated for contingency {} and state {} of {} network",
        				violatedConstraints.size(),
        				contingencyId, 
        				postContingencyState.getStateId(),
        				postContingencyState.getNetwork().getId());

            // Generate corrective actions
            CorrectiveActions correctiveActions = computationService.createCorrectiveActions(
                    postContingencyState,
                    violatedConstraints,
                    client,
                    ipsoNetworkState.getMappingBetweenIidmIdAndIpsoEquipment(),
                    outputListing);

            // Update problem definition
            problemDefinition.mergeControlVariables(correctiveActions.getControlVariables());
            problemDefinition.mergeConstraints(correctiveActions.getConstraints());

            AmplExecutionResults amplResult = null;

            try {

                if (correctiveActions.hasTopologicalActions()) {
                	logger.info("Running AMPL on contingency {} and state {} of {} network", 
                				contingencyId, 
                				postContingencyState.getStateId(),
                				postContingencyState.getNetwork().getId());
                    AmplModel amplModel = computationService.computeNewTopologyFor(
                            ipsoNetworkState,
                            problemDefinition,
                            correctiveActions.getTopologicalActions(),
                            Files.createTempFile(computationManager.getLocalDir(),
                                    AmplConstants.AMPL_UNPLUGGED_EQUIPMENT_PREFIX,
                                    AmplConstants.AMPL_UNPLUGGED_EQUIPMENT_SUFFIX));

                    // run Ampl
                    amplResult = executionService.runAmpl(postContingencyState, amplModel, configuration, computationManager, outputListing);
                    if ( amplResult.getStatus().equals(AmplStatus.EXECUTION_ERROR) ) {
                    	logger.error("Error running AMPL on contingency {} and state {} of {} network",
                    				 contingencyId, 
		                   			 postContingencyState.getStateId(),
		                   			 postContingencyState.getNetwork().getId());
                    }
                } else
                	logger.warn("No topological actions for contingency {} and state {} of {} network", 
                				contingencyId, 
                				postContingencyState.getStateId(),
                				postContingencyState.getNetwork().getId());

            }
            catch(IllegalArgumentException e) {
                outputListing.addAmplError(e);
                logger.error("Error running AMPL on contingency {} and state {} of {} network: {}",
                			 contingencyId, 
                			 postContingencyState.getStateId(),
                			 postContingencyState.getNetwork().getId(),
                			 e.getMessage());
                e.printStackTrace();
            }

            CorrectiveControlOptimizerResult correctiveControlOptimizerResult = executionService.createCorrectiveControlOptimizerResult(amplResult, contingencyId, ipsoNetworkState.getMappingBetweenIidmIdAndIpsoEquipment(), correctiveActions);
            outputListing.addResultCode(correctiveControlOptimizerResult.getFinalStatus());
            try (CommandExecutor executor = computationManager.newCommandExecutor(new HashMap<>(), "itesla_CCO_", true)) {
                final Path workingDir = executor.getWorkingDir();
                outputListing.addLoadFlowResultsFor(postContingencyState.getNetwork());
                outputListing.write(workingDir, postContingencyState.getContingency().getId() + '_' + IpsoConstants.OUTPUT_LISTING_FILENAME);
            }
            return correctiveControlOptimizerResult;
        }
        else {
        	logger.warn("No violated constraints for contingency {} and state {} of {} network",
        				contingencyId, 
        				postContingencyState.getStateId(),
        				postContingencyState.getNetwork().getId());

            final CorrectiveControlOptimizerResult result = new CorrectiveControlOptimizerResult(contingencyId, false);
            result.setFinalStatus(CCOFinalStatus.NO_CONSTRAINT_VIOLATED);
            outputListing.addResultCode(CCOFinalStatus.NO_CONSTRAINT_VIOLATED);
            try (CommandExecutor executor = computationManager.newCommandExecutor(new HashMap<>(), "itesla_CCO_", true)) {
                final Path workingDir = executor.getWorkingDir();
                outputListing.addLoadFlowResultsFor(postContingencyState.getNetwork());
                outputListing.write(workingDir, postContingencyState.getContingency().getId() + '_' + IpsoConstants.OUTPUT_LISTING_FILENAME);
            }
            return result;
        }
    }

    @Override
    public void close() throws Exception {
    }

    private IpsoOutputListing createOutputListing(IpsoNetworkState ipsoNetworkState) {
        return new IpsoComputationService()
                .createNewOutputListing(ipsoNetworkState.getMappingBetweenIidmIdAndIpsoEquipment());
    }

}
