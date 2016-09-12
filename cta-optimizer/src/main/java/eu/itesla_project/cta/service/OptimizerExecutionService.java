/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import eu.itesla_project.computation.*;
import eu.itesla_project.cta.converter.MappingBetweenIidmIdAndIpsoEquipment;
import eu.itesla_project.cta.io.AmplModelWriter;
import eu.itesla_project.cta.model.AmplModel;
import eu.itesla_project.cta.model.TopologicalAction;
import eu.itesla_project.iidm.datasource.FileDataSource;
import eu.itesla_project.iidm.export.Exporter;
import eu.itesla_project.iidm.export.Exporters;
import eu.itesla_project.iidm.network.util.Networks;
import eu.itesla_project.modules.contingencies.ActionParameters;
import eu.itesla_project.modules.optimizer.CorrectiveControlOptimizerResult;
import eu.itesla_project.modules.optimizer.PostContingencyState;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

import static com.google.common.base.Preconditions.checkArgument;
import static eu.itesla_project.cta.service.AmplConstants.*;
import static eu.itesla_project.modules.optimizer.CCOFinalStatus.*;
import static java.util.stream.Collectors.toList;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class OptimizerExecutionService {

    /**
     * Start a optimization on the given ampl model
     *
     * @param postContingencyState : network to optimize
     * @param amplModel : model corresponding to the network
     * @param configuration : configuration to use during the optimization
     * @param computationManager : manage the process execution
     * @param outputListing
     * @return : a list of action to apply to optimize the network
     * @throws Exception
     */
    public AmplExecutionResults runAmpl(PostContingencyState postContingencyState, AmplModel amplModel, IpsoOptimizerConfiguration configuration, ComputationManager computationManager, IpsoOutputListing outputListing) throws Exception {
        checkArgument(configuration != null, "configuration cannot be null");
        checkArgument(configuration.getAmplPath() != null && Files.exists(configuration.getAmplPath()), "'ampl.path' property is not correctly defined");

        try (CommandExecutor executor = computationManager.newCommandExecutor(createEnvironmentVariablesFrom(configuration),
                configuration.getAmplWorkingDirPrefix(),
                configuration.isDebug())) {
        	
        	if (configuration.isDebug()) {
                // dump state info for debugging
                Networks.dumpStateId(executor.getWorkingDir(), postContingencyState.getNetwork());

                Exporter exporter = Exporters.getExporter("XML");
                if (exporter != null) {
                    Properties parameters = new Properties();
                    parameters.setProperty("iidm.export.xml.indent", "true");
                    parameters.setProperty("iidm.export.xml.with-branch-state-variables", "true");
                    parameters.setProperty("iidm.export.xml.with-breakers", "true");
                    parameters.setProperty("iidm.export.xml.with-properties", "true");
                    try {
                        exporter.export(postContingencyState.getNetwork(), parameters, new FileDataSource(executor.getWorkingDir(), "network"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            final Path datPath = executor.getWorkingDir().resolve(AmplConstants.DAT_FILE_NAME);
            AmplModelWriter datWriter = new AmplModelWriter();
            datWriter.write(amplModel, datPath.toString(), configuration.getSolverRessourcePath().toString());
            generateAmplInputFile(executor.getWorkingDir(), configuration);

            Command amplCommand = generateAmplCommand(configuration.getAmplPath());

            ExecutionReport report = executor.start(new CommandExecution(
                    amplCommand,
                    1,
                    configuration.getPriority(),
                    Networks.getExecutionTags(postContingencyState.getNetwork())));

            Path outputFile = executor.getWorkingDir().resolve(AmplConstants.XML_FILE_NAME);
            return AmplOptimizationResultService.createAmplResultFrom(outputFile, report, outputListing);

        }
        catch (Exception e ) {
        	e.printStackTrace();
            outputListing.addAmplError(e);
            return new AmplExecutionResults(AmplStatus.EXECUTION_ERROR);
        }
    }

    /**
     * Copy all the files required by Ampl to the right directory
     * @param workingDir : directory use during the Ampl run
     * @param configuration
     * @throws IOException : Can't write the files
     */

    void generateAmplInputFile(Path workingDir, IpsoOptimizerConfiguration configuration) throws IOException {

        Path runFile = configuration.getSolverRessourcePath().resolve(RUN_FILE_NAME);
        Path tabFile = configuration.getSolverRessourcePath().resolve(TAB_FILE_NAME);
        Path modelsDirectory = configuration.getSolverRessourcePath().resolve(MODELS_DIR);

        Files.copy(runFile, workingDir.resolve(RUN_FILE_NAME));
        Files.copy(tabFile, workingDir.resolve(TAB_FILE_NAME));
        FileUtils.copyDirectory(new File(modelsDirectory.toString()),
                new File(workingDir.resolve(MODELS_DIR).toString()));
    }

    private Command generateAmplCommand(Path amplPath) {
        return new SimpleCommandBuilder()
                .id("ampl")
                .program(amplPath.resolve(AmplConstants.APPLICATION_NAME).toString())
                        // DO NOT WORK ALTHOUGH THE VARIABLE $PATH CONTAINS WELL THE APPLICATION PATH
                        //.program(IpsoConstants.APPLICATION_NAME)
                .args(RUN_FILE_NAME)
                .timeout(60)
                .inputFiles()
                .outputFiles()
                .build();
    }

    /**
     * Generate a map containing the LD_LIBRARY_PATH and PATH
     * environment variable
     *
     * @return a Map with the variable as the key to the corresponding value
     */
    private Map<String, String> createEnvironmentVariablesFrom(IpsoOptimizerConfiguration configuration) {
        Map<String, String> env = new HashMap<>();

        env.put("LD_LIBRARY_PATH", configuration.getSolverPath().toString()
                + ":" + configuration.getIpsoPath().toString()
                + ":" + configuration.getSolverPath().resolve("lib").toString());

        env.put("PATH", configuration.getIpsoPath().toString()
                + ":" + configuration.getSolverPath().resolve("bin").toString());
        
        env.put("XPRESS", configuration.getSolverPath().toString());
        return env;
    }


    public CorrectiveControlOptimizerResult createCorrectiveControlOptimizerResult(AmplExecutionResults amplResults,
                                                                                   String contingencyId,
                                                                                   MappingBetweenIidmIdAndIpsoEquipment dictionary,
                                                                                   CorrectiveActions clientCorrectiveActions) {

        if (isSuccessFor(amplResults)) {
            CorrectiveControlOptimizerResult optimizerResult;
            // get all the actions
            List<TopologicalAction> topologicalActions = amplResults.getActionElements().entrySet().stream()
                    .map(action -> findCorrespondingTopologicalAction(action, clientCorrectiveActions))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(toList());

            // check if topological actions have been found
            if(topologicalActions.isEmpty()) {
                optimizerResult = new CorrectiveControlOptimizerResult(contingencyId, false);
                optimizerResult.setFinalStatus(AUTOMATIC_CORRECTIVE_ACTION_FOUND);
            }
            else {
                optimizerResult = new CorrectiveControlOptimizerResult(contingencyId, true);
                topologicalActions.stream()
                        .forEach(action -> optimizerResult.addEquipment(
                                action.getElementaryActionId(),
                                dictionary.getIidmIdFor(action.getEquipmentId()),
                                amplResults.getActionElements().get(action.getEquipmentId().trim())));
                optimizerResult.setFinalStatus(MANUAL_CORRECTIVE_ACTION_FOUND);

            }
            optimizerResult.setActionPlan(clientCorrectiveActions.getActionPlanId());
            return optimizerResult;

        } else {
            CorrectiveControlOptimizerResult controlOptimizerResult = new CorrectiveControlOptimizerResult(contingencyId, false);
            if(amplResults == null) {
                controlOptimizerResult.setFinalStatus(NO_SUPPORTED_CORRECTIVE_ACTION_AVAILABLE_IN_THE_DATABASE);
            }
            else {
                switch (amplResults.getStatus()) {
                    case UNFEASIBLE:
                        controlOptimizerResult.setFinalStatus(NO_CORRECTIVE_ACTION_FOUND);
                        break;
                    case ERROR:
                        controlOptimizerResult.setFinalStatus(OPTIMIZER_INTERNAL_ERROR);
                        break;
                    case EXECUTION_ERROR:
                        controlOptimizerResult.setFinalStatus(OPTIMIZER_EXECUTION_ERROR);
                        break;
                }
            }
            return controlOptimizerResult;
        }
    }

    private boolean isSuccessFor(AmplExecutionResults amplResults) {
        return isNotNullOrEmpty(amplResults) && amplResults.getStatus() == AmplStatus.SUCCEEDED;
    }

    Optional<TopologicalAction> findCorrespondingTopologicalAction(Entry<String, ActionParameters> correction, CorrectiveActions correctiveActions) {
        return correctiveActions.getTopologicalActions().stream()
                .filter(topologicalAction -> topologicalAction.getEquipmentId().trim().equals(correction.getKey()))
                .filter(topologicalAction -> topologicalAction.isEquivalentTo(correction.getValue()))
                .findFirst();
    }

    private boolean isNotNullOrEmpty(AmplExecutionResults results) {
        return results != null && !results.getActionElements().isEmpty();
    }
}
