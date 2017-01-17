/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.api.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.itesla_project.modules.online.OnlineDb;
import eu.itesla_project.modules.online.OnlineDbFactory;
import eu.itesla_project.modules.online.OnlineProcess;
import eu.itesla_project.modules.online.OnlineStep;
import eu.itesla_project.modules.online.OnlineWorkflowParameters;
import eu.itesla_project.modules.online.OnlineWorkflowResults;
import eu.itesla_project.modules.online.StateProcessingStatus;
import eu.itesla_project.online.rest.api.DateTimeParameter;
import eu.itesla_project.online.rest.model.PostContingencyResult;
import eu.itesla_project.online.rest.model.PreContingencyResult;
import eu.itesla_project.online.rest.model.Process;
import eu.itesla_project.online.rest.model.SimulationResult;
import eu.itesla_project.online.rest.model.Violation;
import eu.itesla_project.online.rest.model.WorkflowInfo;
import eu.itesla_project.online.rest.model.WorkflowResult;
import eu.itesla_project.security.LimitViolation;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
 */
public class OnlineDBUtils implements ProcessDBUtils {
    private final OnlineDbFactory fact;
    private static final Logger LOGGER = LoggerFactory.getLogger(OnlineDBUtils.class);

    public OnlineDBUtils(OnlineDbFactory factory) {
        fact = Objects.requireNonNull(factory);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * eu.itesla_project.online.rest.api.util.ProcessDBUtils#getProcessList(
     * java. lang.String, java.lang.String, java.lang.String,
     * eu.itesla_project.online.rest.api.DateTimeParameter,
     * eu.itesla_project.online.rest.api.DateTimeParameter)
     */
    @Override
    public List<Process> getProcessList(String owner, String basecase, String name, DateTimeParameter date,
            DateTimeParameter creationDate) throws Exception {
        List<Process> processes = new ArrayList<>();

        List<OnlineProcess> storedProcesses = null;
        try (OnlineDb onlinedb = fact.create()) {
            storedProcesses = onlinedb.listProcesses();
            if (storedProcesses != null) {
                processes = storedProcesses.stream().filter(p -> (name == null) || name.equals(p.getName()))
                        .filter(p -> (owner == null) || owner.equals(p.getOwner()))
                        .filter(p -> (basecase == null) || p.getWorkflowsMap().containsKey(basecase))
                        .filter(p -> (date == null) || date.getDateTime().getMillis() == p.getDate().getMillis())
                        .filter(p -> (creationDate == null)
                                || creationDate.getDateTime().getMillis() == p.getCreationDate().getMillis())
                        .map(p -> toProcess(p)).collect(Collectors.toList());
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new Exception(e.getMessage());
        }
        return processes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * eu.itesla_project.online.rest.api.util.ProcessDBUtils#getProcess(java.
     * lang.String)
     */
    @Override
    public Process getProcess(String processId) throws Exception {
        Objects.requireNonNull(processId);
        Process proc = null;
        OnlineProcess storedProcess = null;
        try (OnlineDb onlinedb = fact.create()) {
            storedProcess = onlinedb.getProcess(processId);
            if (storedProcess != null) {
                proc = toProcess(storedProcess);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new Exception(e.getMessage());
        }
        return proc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * eu.itesla_project.online.rest.api.util.ProcessDBUtils#getWorkflowResult(
     * java.lang.String, java.lang.String)
     */
    @Override
    public WorkflowResult getWorkflowResult(String processId, String workflowId) throws Exception {
        Objects.requireNonNull(processId);
        Objects.requireNonNull(workflowId);
        OnlineProcess p = null;
        try (OnlineDb onlinedb = fact.create()) {
            p = onlinedb.getProcess(processId);

            if (p != null && p.getWorkflowsMap().containsValue(workflowId)
                    && onlinedb.getWorkflowDetails(workflowId) != null) {

                OnlineWorkflowParameters params = onlinedb.getWorkflowParameters(workflowId);
                WorkflowResult res = new WorkflowResult(processId, workflowId,
                        params.getBaseCaseDate().toDateTimeISO().toString());

                Map<Integer, List<LimitViolation>> violations = onlinedb.getViolations(workflowId,
                        OnlineStep.LOAD_FLOW);
                if (violations != null) {
                    violations.forEach(new BiConsumer<Integer, List<LimitViolation>>() {

                        @Override
                        public void accept(Integer state, List<LimitViolation> viols) {

                            StateProcessingStatus sp = onlinedb.getStatesProcessingStatus(workflowId).get(state);
                            String status = sp.getStatus().get("LOAD_FLOW");
                            PreContingencyResult pcr = new PreContingencyResult(state, viols.isEmpty(),
                                    status != null && "SUCCESS".equals(status));

                            for (LimitViolation lv : viols) {
                                pcr.addViolation(new Violation(lv.getCountry().toString(), lv.getSubject().getId(),
                                        lv.getLimitType().name(), lv.getLimit(), lv.getValue(),
                                        (int) lv.getBaseVoltage()));
                            }
                            res.addPreContingency(pcr);
                        }
                    });
                }

                Map<String, Map<Integer, SimulationResult>> postMap = new HashMap<String, Map<Integer, SimulationResult>>();

                Map<Integer, Map<String, Boolean>> conv = onlinedb.getPostContingencyLoadflowConvergence(workflowId);
                if (conv != null) {
                    conv.forEach(new BiConsumer<Integer, Map<String, Boolean>>() {

                        @Override
                        public void accept(Integer state, Map<String, Boolean> convergenceMap) {

                            for (String cont : convergenceMap.keySet()) {
                                Map<Integer, SimulationResult> srMap = postMap.get(cont);
                                if (srMap == null) {
                                    srMap = new HashMap<Integer, SimulationResult>();
                                    postMap.put(cont, srMap);
                                }
                                SimulationResult sr = srMap.get(state);
                                if (sr == null) {
                                    sr = new SimulationResult(state);
                                    srMap.put(state, sr);
                                }
                                sr.setConvergence(convergenceMap.get(cont));
                            }
                        }
                    });
                }

                Map<Integer, Map<String, List<LimitViolation>>> violsMap = onlinedb
                        .getPostContingencyViolations(workflowId);
                if (violsMap != null) {
                    violsMap.forEach(new BiConsumer<Integer, Map<String, List<LimitViolation>>>() {

                        @Override
                        public void accept(Integer state, Map<String, List<LimitViolation>> contViolMap) {
                            for (String cont : contViolMap.keySet()) {
                                Map<Integer, SimulationResult> srMap = postMap.get(cont);
                                if (srMap == null) {
                                    srMap = new HashMap<Integer, SimulationResult>();
                                    postMap.put(cont, srMap);
                                }
                                SimulationResult sr = srMap.get(state);
                                if (sr == null) {
                                    sr = new SimulationResult(state);
                                    srMap.put(state, sr);
                                }

                                for (LimitViolation lv : contViolMap.get(cont)) {
                                    sr.addViolation(new Violation(lv.getCountry().toString(), lv.getSubject().getId(),
                                            lv.getLimitType().name(), lv.getLimit(), lv.getValue(),
                                            (int) lv.getBaseVoltage()));
                                }
                            }
                        }
                    });
                }

                OnlineWorkflowResults wfResults = onlinedb.getResults(workflowId);
                if (wfResults != null) {
                    for (String contingencyId : wfResults.getUnsafeContingencies()) {
                        for (Integer stateId : wfResults.getUnstableStates(contingencyId)) {
                            Map<Integer, SimulationResult> srMap = postMap.get(contingencyId);
                            SimulationResult sr = srMap.get(stateId);
                            if (sr == null) {
                                sr = new SimulationResult(stateId);
                                srMap.put(stateId, sr);
                            }
                            sr.setSafe(!wfResults.getIndexesData(contingencyId, stateId).containsValue(Boolean.FALSE));
                        }
                    }
                }

                postMap.forEach(new BiConsumer<String, Map<Integer, SimulationResult>>() {

                    @Override
                    public void accept(String cont, Map<Integer, SimulationResult> resultsMap) {
                        res.addPostContingency(new PostContingencyResult(cont,
                                resultsMap.values().stream().collect(Collectors.toList())));
                    }
                });
                return res;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new Exception(e.getMessage());
        }
        return null;
    }

    private Process toProcess(OnlineProcess p) {
        Process proc = new Process(p.getId(), p.getName(), p.getOwner(), p.getDate().toDate(),
                p.getCreationDate().toDate());
        p.getWorkflowsMap().forEach(new BiConsumer<String, String>() {

            @Override
            public void accept(String bscase, String workflowId) {
                try {
                    proc.addWorkflowInfo(
                            new WorkflowInfo(workflowId, bscase, getWorkflowResult(p.getId(), workflowId)));
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        });
        return proc;
    }

}
