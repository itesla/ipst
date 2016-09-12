/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import eu.itesla_project.computation.ExecutionError;
import eu.itesla_project.computation.ExecutionReport;
import eu.itesla_project.modules.contingencies.ActionParameterBooleanValue;
import eu.itesla_project.modules.contingencies.ActionParameters;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.System.getProperty;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class AmplOptimizationResultService {

    protected static final String SOLVED = "solved";
    protected static final String INFEASIBLE = "infeasible";

    public static AmplExecutionResults createAmplResultFrom(Path resultPath, ExecutionReport report, IpsoOutputListing outputListing) {

        Map<String, ActionParameters> results = new HashMap<>();
        AmplStatus status = AmplStatus.SUCCEEDED;

        try {
            if (!report.getErrors().isEmpty()) {
                status = AmplStatus.EXECUTION_ERROR;
                throw new AmplExecutionError("Error during Ampl process: Result file not generated." + findErrorMessageIn(report));

            } else if (!Files.exists(resultPath)) {
                status = AmplStatus.EXECUTION_ERROR;
                throw new AmplExecutionError("Error during Ampl process: Result file not generated.");

            } else {
                // XML parsing
                DocumentBuilderFactory parserFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder parser = parserFactory.newDocumentBuilder();
                Document resultDocument = parser.parse(resultPath.toFile());
                resultDocument.normalizeDocument();

                NodeList childNodes = resultDocument.getElementsByTagName(AmplConstants.RESULTS_TAG).item(0).getChildNodes();

                for (int i = 0; i < childNodes.getLength(); i++) {
                    if (isEquipmentsTag(childNodes, i)) {
                        results = readResults(childNodes.item(i).getChildNodes());
                    }
                    if (isStatusTag(childNodes, i)) {
                        status = readStatus(childNodes.item(i).getChildNodes(), i);
                    }
                }
                writeResultsTo(outputListing, status, results);
            }
        } catch (Exception e) {
        	e.printStackTrace();
            outputListing.addAmplError(e);
            status = AmplStatus.EXECUTION_ERROR;
        }
        finally {
            return new AmplExecutionResults(results, status);
        }
    }

    private static void writeResultsTo(IpsoOutputListing outputListing, AmplStatus status, Map<String, ActionParameters> results) {
        List<String> actions = getActionDescriptionsFrom(results);
        outputListing.addAmplResult(status, actions);
    }

    private static List<String> getActionDescriptionsFrom(Map<String, ActionParameters> results) {
        return results.entrySet().stream()
                        .map(entry -> formatResult(entry.getKey(), entry.getValue()))
                        .collect(toList());
    }

    private static AmplStatus readStatus(NodeList childNodes, int i) {
        String status = childNodes.item(i).getTextContent();
        if(status.contains(SOLVED)) {
            return AmplStatus.SUCCEEDED;
        }
        else if (status.contains(INFEASIBLE)) {
            return AmplStatus.UNFEASIBLE;
        } else {
            return AmplStatus.ERROR;
        }
    }

    private static boolean isStatusTag(NodeList childNodes, int i) {
        return childNodes.item(i).getNodeName().equals(AmplConstants.AMPL_STATUS_TAG);
    }

    private static boolean isEquipmentsTag(NodeList childNodes, int i) {
        return childNodes.item(i).getNodeName().equals(AmplConstants.AMPL_EQUIPMENT_TAG);
    }

    /**
     * Generate a hash map containing the equipment id as key and
     * relatd ActionParameters as value
     *
     * @param equipments : xml data related to the equipments
     * @return
     */
    private static Map<String, ActionParameters> readResults(NodeList equipments) {
        Map<String, ActionParameters> results = new HashMap<>();

        for (int i = 0; i < equipments.getLength(); i++) {
            addResult(equipments.item(i).getChildNodes(), results);
        }

        return results;
    }

    /**
     * Add an entry into the results Hash Map
     * @param equipmentData : data corresponding to the corrective actions of a given equipment
     * @param results : hashmap containing all the results already computed
     */
    private static void addResult(NodeList equipmentData, Map<String, ActionParameters> results) {
        String name = "";
        ActionParameters parameters = null;
        for(int i = 0; i < equipmentData.getLength(); i++) {
            if(equipmentData.item(i).getNodeName().equals(AmplConstants.ID_TAG)) {
                name = equipmentData.item(i).getTextContent();
            }
            else if (equipmentData.item(i).getNodeName().equals(AmplConstants.PARAMETERS_TAG)) {
                parameters = findParametersFrom(equipmentData.item(i).getChildNodes());
            }
        }

        if(!name.isEmpty() && parameters != null) {
            results.put(name, parameters);
        }
    }

    /**
     * Create an ActionParameters instance corresponding to the parameters
     * contained into the xml
     *
     * @param xmlNodes : xml data of the parameters
     * @return : the parameters corresponding to the xml
     */
    private static ActionParameters findParametersFrom(NodeList xmlNodes) {

        ActionParameters actionParameters = new ActionParameters();

        for(int i = 0; i < xmlNodes.getLength(); i++) {
            String parameterName = xmlNodes.item(i).getNodeName();
            String parameterValue = xmlNodes.item(i).getTextContent();
            if(!parameterName.isEmpty() && !parameterValue.isEmpty()) {
                if(parameterName.equals(AmplConstants.SWITCH_TAG))
                    actionParameters.addParameter(parameterName, new ActionParameterBooleanValue(parameterValue.equals("1")));
            }
        }
        return actionParameters;
    }

    private static String findErrorMessageIn(ExecutionReport report) {
        return report.getErrors().stream()
                .map(ExecutionError::toString)
                .collect(
                        joining(getProperty("line.separator"))
                );
    }

    private static String formatResult(String key, ActionParameters value) {
        checkArgument(value != null, "value must not be null");
        StringBuilder pararameters = new StringBuilder();
        for(String parameterName : value.getNames()) {
            pararameters.append(parameterName + " : ");
            Object parameterValue = value.getValue(parameterName);
            if(parameterName.equals(AmplConstants.SWITCH_TAG) && parameterValue instanceof Boolean) {
                pararameters.append(((boolean) parameterValue) ? "close" : "open");
            }
            else {
                pararameters.append(parameterValue.toString());
            }
            pararameters.append(", ");
        }
        return new StringBuilder()
                .append(" ")
                .append(key)
                .append(" ")
                .append(pararameters.toString())
                .append(" ")
                .toString();
    }

}
