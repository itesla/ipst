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
import java.util.Optional;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
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
import eu.itesla_project.online.rest.model.Indicator;
import eu.itesla_project.online.rest.model.IndicatorEnum;
import eu.itesla_project.online.rest.model.PostContingencyResult;
import eu.itesla_project.online.rest.model.PreContingencyResult;
import eu.itesla_project.online.rest.model.Process;
import eu.itesla_project.online.rest.model.ProcessSynthesis;
import eu.itesla_project.online.rest.model.SimulationResult;
import eu.itesla_project.online.rest.model.StateSynthesis;
import eu.itesla_project.online.rest.model.TimeValue;
import eu.itesla_project.online.rest.model.UnitEnum;
import eu.itesla_project.online.rest.model.Violation;
import eu.itesla_project.online.rest.model.ViolationSynthesis;
import eu.itesla_project.online.rest.model.WorkflowInfo;
import eu.itesla_project.online.rest.model.WorkflowResult;
import eu.itesla_project.security.LimitViolation;
import eu.itesla_project.security.LimitViolationType;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
 */
public class OnlineDBUtils implements ProcessDBUtils {
    private final OnlineDbFactory fact;
    private static final DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
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

        try (OnlineDb onlinedb = fact.create()) {
            List<OnlineProcess> storedProcesses = onlinedb.listProcesses();
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
        try (OnlineDb onlinedb = fact.create()) {
            OnlineProcess storedProcess = onlinedb.getProcess(processId);
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
        try (OnlineDb onlinedb = fact.create()) {
            OnlineProcess p = onlinedb.getProcess(processId);

            if (p != null && p.getWorkflowsMap().containsValue(workflowId)
                    && onlinedb.getWorkflowDetails(workflowId) != null) {

                OnlineWorkflowParameters params = onlinedb.getWorkflowParameters(workflowId);
                WorkflowResult res = new WorkflowResult(processId, workflowId,
                        params.getBaseCaseDate().toDateTimeISO().toString());

                Map<Integer, List<LimitViolation>> violations = onlinedb.getViolations(workflowId,
                        OnlineStep.LOAD_FLOW);
                if (violations != null) {

                    violations.forEach((state, viols) -> {
                        StateProcessingStatus sp = onlinedb.getStatesProcessingStatus(workflowId).get(state);
                        String status = sp.getStatus().get("LOAD_FLOW");
                        PreContingencyResult pcr = new PreContingencyResult(state, viols.isEmpty(),
                                status != null && "SUCCESS".equals(status));
                        viols.forEach(lv -> {
                            pcr.addViolation(new Violation(lv.getCountry().toString(), lv.getSubject().getId(),
                                    lv.getLimitType().name(), lv.getLimit(), lv.getValue(), (int) lv.getBaseVoltage()));
                        });
                        res.addPreContingency(pcr);
                    });
                }

                Map<String, Map<Integer, SimulationResult>> postMap = new HashMap<String, Map<Integer, SimulationResult>>();

                Map<Integer, Map<String, Boolean>> conv = onlinedb.getPostContingencyLoadflowConvergence(workflowId);
                if (conv != null) {
                    conv.forEach((state, convergenceMap) -> {
                        convergenceMap.forEach((cont, convergence) -> {
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
                            sr.setConvergence(convergence);
                        });
                    });
                }

                Map<Integer, Map<String, List<LimitViolation>>> violsMap = onlinedb
                        .getPostContingencyViolations(workflowId);
                if (violsMap != null) {

                    violsMap.forEach((state, contViolMap) -> {
                        contViolMap.forEach((cont, contViols) -> {
                            Map<Integer, SimulationResult> srMap = postMap.get(cont);
                            if (srMap == null) {
                                srMap = new HashMap<Integer, SimulationResult>();
                                postMap.put(cont, srMap);
                            }
                            SimulationResult sr = srMap.containsKey(state) ? srMap.get(state)
                                    : new SimulationResult(state);
                            if (!srMap.containsKey(state))
                                srMap.put(state, sr);

                            contViols.forEach(lv -> {
                                sr.addViolation(new Violation(lv.getCountry().toString(), lv.getSubject().getId(),
                                        lv.getLimitType().name(), lv.getLimit(), lv.getValue(),
                                        (int) lv.getBaseVoltage()));
                            });
                        });

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

                postMap.forEach((cont, resultsMap) -> {
                    res.addPostContingency(
                            new PostContingencyResult(cont, resultsMap.values().stream().collect(Collectors.toList())));
                });
                return res;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new Exception(e.getMessage());
        }
        return null;
    }

    @Override
    public ProcessSynthesis getSynthesis(String processId) throws Exception {
        Objects.requireNonNull(processId);
        try (OnlineDb onlinedb = fact.create()) {
            OnlineProcess p = onlinedb.getProcess(processId);
            if(p != null)
            {
                ProcessSynthesis result = new ProcessSynthesis(processId);
                Map<Integer, StateSynthesis> statesMap = new HashMap<Integer, StateSynthesis>();
    
                p.getWorkflowsMap().forEach((basecase, workflowId) -> {
                    DateTime dateTime = fmt.parseDateTime(basecase);
                    Map<Integer, List<LimitViolation>> previolsMap = onlinedb.getViolations(workflowId, OnlineStep.LOAD_FLOW);
    
                    if(previolsMap != null)
                    {
                        previolsMap.forEach( (state, limitList) -> {               
                            StateSynthesis statesynt = statesMap.get(state);
                            if (statesynt == null) {
                                statesynt = new StateSynthesis(state);
                                statesMap.put(state, statesynt);
                            }                  
                            fillViolationSynthesis(dateTime, statesynt.getPreContingencyViolations(), limitList);
                        });
                    }
    
                    Map<Integer, Map<String, List<LimitViolation>>> violsMap = onlinedb
                            .getPostContingencyViolations(workflowId);
    
                    if (violsMap != null) {
                        violsMap.forEach((state, contingencyViolationMap) -> {
                            StateSynthesis statesynt = statesMap.get(state);
                            if (statesynt == null) {
                                statesynt = new StateSynthesis(state);
                                statesMap.put(state, statesynt);
                            }
    
                            Map<String, List<ViolationSynthesis>> contingencyMap = statesynt.getPostContingencyViolations();
    
                            contingencyViolationMap.forEach( (cont, limitList) -> {
                                List<ViolationSynthesis> violationList = contingencyMap.get(cont);
                                if (violationList == null) {
                                    violationList = new ArrayList();
                                    contingencyMap.put(cont, violationList);
                                }
                                fillViolationSynthesis(dateTime, violationList, limitList);
                            });
                        });
                    }
                });
                result.addStateSynthesis(new ArrayList<>( statesMap.values()));
                return result;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new Exception(e.getMessage());
        }
        return null;
    }

    private void fillViolationSynthesis( DateTime dateTime, List<ViolationSynthesis> violationList , List<LimitViolation> limitList ){
        limitList.forEach( lv ->{
            LimitViolationType violationType = lv.getLimitType();
            String equipment = lv.getSubject().getId();
            float limit = lv.getLimit();
            float value = lv.getValue();
            ViolationSynthesis synt;
            Optional<ViolationSynthesis> search_synt = violationList.stream()
                    .filter(v -> v.getEquipment().equals(equipment))
                    .filter(v -> v.getType().equals(violationType)).findFirst();
            if (search_synt.isPresent())
                synt = search_synt.get();
            else {
                synt = new ViolationSynthesis(equipment, violationType, limit);
                violationList.add(synt);
            }

            double perc = value / limit;
            TimeValue tv = new TimeValue(dateTime);
            tv.putIndicator(new Indicator(IndicatorEnum.RELATIVE, UnitEnum.PERCENTAGE, perc));
            synt.addTimeValue(tv);
        });
    }

    private Process toProcess(OnlineProcess p) {
        Objects.requireNonNull(p);
        Process proc = new Process(p.getId(), p.getName(), p.getOwner(), p.getDate().toDate(),
                p.getCreationDate().toDate());

        p.getWorkflowsMap().forEach((bscase, workflowId) -> {
            try {
                proc.addWorkflowInfo(new WorkflowInfo(workflowId, bscase, getWorkflowResult(p.getId(), workflowId)));
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        });
        return proc;
    }

}
