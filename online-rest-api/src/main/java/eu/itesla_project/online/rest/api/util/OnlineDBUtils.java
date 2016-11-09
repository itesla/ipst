/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.api.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import java.util.function.Predicate;

import eu.itesla_project.modules.online.OnlineDb;
import eu.itesla_project.modules.online.OnlineProcess;
import eu.itesla_project.modules.online.OnlineStep;
import eu.itesla_project.modules.online.OnlineWorkflowDetails;
import eu.itesla_project.modules.online.OnlineWorkflowParameters;
import eu.itesla_project.modules.online.OnlineWorkflowResults;
import eu.itesla_project.modules.online.StateProcessingStatus;

import eu.itesla_project.online.db.OnlineDbMVStoreFactory;
import eu.itesla_project.online.rest.model.PostContingencyResult;
import eu.itesla_project.online.rest.model.PreContingencyResult;
import eu.itesla_project.online.rest.model.SimulationResult;
import eu.itesla_project.online.rest.model.Violation;
import eu.itesla_project.online.rest.model.WorkflowInfo;
import eu.itesla_project.online.rest.model.WorkflowResult;
import eu.itesla_project.security.LimitViolation;
import eu.itesla_project.online.rest.model.Process;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class OnlineDBUtils {
	OnlineDbMVStoreFactory fact = new OnlineDbMVStoreFactory();

	public List<Process> listProcesses(String user, String basecase, String name, Date date, Date creationDate) {
		List<Process> processes = new ArrayList<Process>();

		OnlineDb onlinedb = fact.create();
		List<OnlineProcess> storedProcesses = null;
		try {

			storedProcesses = onlinedb.listProcesses();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				onlinedb.close();
			} catch (Exception e) {
			}
		}
		if (storedProcesses != null) {

			processes = storedProcesses.stream().filter(new Predicate<OnlineProcess>() {

				@Override
				public boolean test(OnlineProcess p) {
					boolean res = true;
					if (user != null)
						res = res && user.equals(p.getOwner());
					if (basecase != null)
						res = res && p.getWorkflowsMap().containsKey(basecase);
					if (date != null)
						res = res && date.equals(p.getDate());
					if (creationDate != null)
						res = res && creationDate.equals(p.getCreationDate());
					return res;
				}
			}).map(p -> toProcess(p)).collect(Collectors.toList());
		}
		return processes;
	}

	public Process getProcess(String processId) {
		Process proc = null;
		OnlineDb onlinedb = fact.create();
		OnlineProcess storedProcess = null;
		try {

			storedProcess = onlinedb.getProcess(processId);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				onlinedb.close();
			} catch (Exception e) {
			}
		}
		if (storedProcess != null) {
			proc = toProcess(storedProcess);
		}
		return proc;
	}

	public WorkflowResult getWorkflowResult(String processId, String workflowId) {

		OnlineDb onlinedb = fact.create();
		OnlineProcess p = null;
		try {
			p = onlinedb.getProcess(processId);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		if (p == null)
			return null;
		WorkflowResult res = null;
		if (!p.getWorkflowsMap().containsValue(workflowId))
			return null;

		OnlineWorkflowDetails det = onlinedb.getWorkflowDetails(workflowId);
		if (det != null) {
			res = new WorkflowResult();
			res.setProcessId(processId);

			OnlineWorkflowParameters params = onlinedb.getWorkflowParameters(workflowId);

			res.setBasecase(params.getBaseCaseDate().toDateTimeISO().toString());
			res.setProcessId(processId);
			res.setWorkflowId(workflowId);

			List<PreContingencyResult> preList = new ArrayList<PreContingencyResult>();

			Map<Integer, List<LimitViolation>> violations = onlinedb.getViolations(workflowId, OnlineStep.LOAD_FLOW);
			if (violations != null) {
				violations.forEach(new BiConsumer<Integer, List<LimitViolation>>() {

					@Override
					public void accept(Integer state, List<LimitViolation> viols) {

						PreContingencyResult pcr = new PreContingencyResult();
						pcr.setState(state);

						pcr.setSafe(viols.isEmpty());
						List<Violation> vlist = new ArrayList<Violation>();
						for (LimitViolation lv : viols) {
							Violation v = new Violation();
							v.setCountry(lv.getCountry().toString());
							v.setEquipment(lv.getSubject().getId());
							v.setLimit(lv.getLimit());
							v.setValue(lv.getValue());
							v.setType(lv.getLimitType().name());
							v.setVoltageLevel((int) lv.getBaseVoltage());

							vlist.add(v);
						}
						pcr.setViolations(vlist);

						StateProcessingStatus sp = onlinedb.getStatesProcessingStatus(workflowId).get(state);
						String status = sp.getStatus().get("LOAD_FLOW");
						pcr.setConvergence(status != null && "SUCCESS".equals(status));
						preList.add(pcr);
					}
				});
				res.setPreContingency(preList);
			}

			List<PostContingencyResult> postList = new ArrayList<PostContingencyResult>();
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
								sr = new SimulationResult();
								sr.setState(state);
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
								sr = new SimulationResult();
								sr.setState(state);
								srMap.put(state, sr);
							}

							List<Violation> vlist = new ArrayList<Violation>();
							for (LimitViolation lv : contViolMap.get(cont)) {
								Violation v = new Violation();
								v.setCountry(lv.getCountry().toString());
								v.setEquipment(lv.getSubject().getId());
								v.setLimit(lv.getLimit());
								v.setValue(lv.getValue());
								v.setType(lv.getLimitType().name());
								v.setVoltageLevel((int) lv.getBaseVoltage());

								vlist.add(v);
							}
							sr.setViolations(vlist);

						}

					}
				});
			}

			OnlineWorkflowResults wfResults = onlinedb.getResults(workflowId);
			for (String contingencyId : wfResults.getUnsafeContingencies()) {
				for (Integer stateId : wfResults.getUnstableStates(contingencyId)) {

					Map<String, Boolean> idxData = wfResults.getIndexesData(contingencyId, stateId);
					boolean safe = false;
					if (!idxData.values().contains(Boolean.FALSE))
						safe = true;

					Map<Integer, SimulationResult> srMap = postMap.get(contingencyId);
					SimulationResult sr = srMap.get(stateId);
					if (sr == null) {
						sr = new SimulationResult();
						sr.setState(stateId);
						srMap.put(stateId, sr);
					}
					sr.setSafe(safe);

				}
			}

			postMap.forEach(new BiConsumer<String, Map<Integer, SimulationResult>>() {

				@Override
				public void accept(String cont, Map<Integer, SimulationResult> resultsMap) {
					PostContingencyResult pcr = new PostContingencyResult();
					pcr.setContingency(cont);
					List<SimulationResult> srs = new ArrayList<SimulationResult>();
					srs.addAll(resultsMap.values());
					pcr.setResults(srs);
					postList.add(pcr);

				}
			});
			res.setPostContingency(postList);
		}

		try {
			onlinedb.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	private Process toProcess(OnlineProcess p) {
		Process proc = new Process();
		proc.setId(p.getId());
		proc.setDate(p.getDate().toDate());
		proc.setCreationDate(p.getCreationDate().toDate());
		proc.setName(p.getName());
		proc.setOwner(p.getOwner());
		List<WorkflowInfo> wfList = new ArrayList<WorkflowInfo>();
		p.getWorkflowsMap().forEach(new BiConsumer<String, String>() {

			@Override
			public void accept(String bscase, String workflowId) {
				WorkflowInfo wfi = new WorkflowInfo();
				wfi.setBaseCase(bscase);
				wfi.setId(workflowId);
				wfi.setWorkflowResult(getWorkflowResult(p.getId(), workflowId));
				wfList.add(wfi);
			}
		});
		proc.setWorkflows(wfList);
		return proc;
	}

}
