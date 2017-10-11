package eu.itesla_project.online.rest.api.test.mock;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import eu.itesla_project.cases.CaseType;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.NetworkFactory;
import eu.itesla_project.modules.contingencies.ActionParameters;
import eu.itesla_project.modules.online.OnlineDb;
import eu.itesla_project.modules.online.OnlineProcess;
import eu.itesla_project.modules.online.OnlineStep;
import eu.itesla_project.modules.online.OnlineWorkflowDetails;
import eu.itesla_project.modules.online.OnlineWorkflowParameters;
import eu.itesla_project.modules.online.OnlineWorkflowResults;
import eu.itesla_project.modules.online.OnlineWorkflowRulesResults;
import eu.itesla_project.modules.online.OnlineWorkflowWcaResults;
import eu.itesla_project.modules.online.StateProcessingStatus;
import eu.itesla_project.modules.online.TimeHorizon;
import eu.itesla_project.modules.optimizer.CCOFinalStatus;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationType;

public class OnlineDbMock implements OnlineDb {

    private Map<String, OnlineProcess> processMap;
    private Map<String, DateTime> workflowMap;
    private final Network network;

    public OnlineDbMock() {
        network = NetworkFactory.create("test", "test");
        network.newSubstation().setId("sub1").setName("substation1").setCountry(Country.FR).add();

        processMap = new HashMap<String, OnlineProcess>();
        DateTime dt = new DateTime(2016, 1, 15, 01, 0, 0, 0);
        OnlineProcess p = new OnlineProcess("1111", "name1", "owner1", CaseType.FO.toString(), dt,
                dt.plusMinutes(10));
        p.addWorkflow("2016-01-10T01:00:00.000+00:00", "1122");
        p.addWorkflow("2016-01-10T02:00:00.000+00:00", "1123");
        dt = new DateTime(2016, 1, 16, 01, 0, 0, 0);
        OnlineProcess p2 = new OnlineProcess("2222", "name2", "owwer2", CaseType.FO.toString(), dt,
                dt.plusMinutes(10));
        p2.addWorkflow("2016-01-11T02:00:00.000+00:00", "2233");

        processMap.put(p.getId(), p);
        processMap.put(p2.getId(), p2);

        workflowMap = new HashMap<String, DateTime>();

        DateTime wdt = new DateTime(2016, 1, 10, 01, 0, 0, 0);
        workflowMap.put("1122", wdt);
        DateTime wdt12 = new DateTime(2016, 1, 10, 02, 0, 0, 0);
        workflowMap.put("1123", wdt12);

        DateTime wdt2 = new DateTime(2016, 1, 11, 02, 0, 0, 0);
        workflowMap.put("2233", wdt2);

    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public List<OnlineWorkflowDetails> listWorkflows() {
        return null;
    }

    @Override
    public List<OnlineWorkflowDetails> listWorkflows(DateTime basecaseDate) {
        return null;
    }

    @Override
    public List<OnlineWorkflowDetails> listWorkflows(Interval basecaseInterval) {
        return null;
    }

    @Override
    public OnlineWorkflowDetails getWorkflowDetails(String workflowId) {
        if (workflowMap.containsKey(workflowId))
            return new OnlineWorkflowDetails(workflowId);

        return null;
    }

    @Override
    public void storeResults(String workflowId, OnlineWorkflowResults results) {

    }

    @Override
    public OnlineWorkflowResults getResults(String workflowId) {
        if (!"1122".equals(workflowId))
            return null;
        OnlineWorkflowResults res = new OnlineWorkflowResults() {

            @Override
            public String getWorkflowId() {
                return workflowId;
            }

            @Override
            public TimeHorizon getTimeHorizon() {
                return null;
            }

            @Override
            public Collection<String> getContingenciesWithActions() {
                return null;
            }

            @Override
            public Collection<String> getUnsafeContingencies() {
                List<String> res = new ArrayList<>();
                res.add("test_contingency");
                return res;
            }

            @Override
            public Map<Integer, Boolean> getUnsafeStatesWithActions(String contingencyId) {
                return null;
            }

            @Override
            public List<Integer> getUnstableStates(String contingencyId) {
                List<Integer> res = new ArrayList<>();
                res.add(0);
                return res;
            }

            @Override
            public CCOFinalStatus getStateStatus(String contingencyId, Integer stateId) {
                return null;
            }

            @Override
            public String getCause(String contingencyId, Integer stateId) {
                return null;
            }

            @Override
            public String getActionPlan(String contingencyId, Integer stateId) {
                return null;
            }

            @Override
            public List<String> getActionsIds(String contingencyId, Integer stateId) {
                return null;
            }

            @Override
            public Map<String, Boolean> getIndexesData(String contingencyId, Integer stateId) {
                Map<String, Boolean> res = new HashMap<>();
                res.put("contingencyId", Boolean.TRUE);
                return res;
            }

            @Override
            public List<String> getEquipmentsIds(String contingencyId, Integer stateId, String actionId) {
                return null;
            }

            @Override
            public Map<String, ActionParameters> getEquipmentsWithParameters(String contingencyId, Integer stateId,
                    String actionId) {
                return null;
            }

            @Override
            public ActionParameters getParameters(String contingencyId, Integer stateId, String actionId,
                    String equipmentId) {
                return null;
            }
        };
        return res;
    }

    @Override
    public void storeMetrics(String workflowId, OnlineStep step, Map<String, String> metrics) {

    }

    @Override
    public void storeMetrics(String workflowId, Integer stateId, OnlineStep step, Map<String, String> metrics) {

    }

    @Override
    public Map<String, String> getMetrics(String workflowId, OnlineStep step) {
        return null;
    }

    @Override
    public Map<String, String> getMetrics(String workflowId, Integer stateId, OnlineStep step) {
        return null;
    }

    @Override
    public void storeRulesResults(String workflowId, OnlineWorkflowRulesResults results) {

    }

    @Override
    public OnlineWorkflowRulesResults getRulesResults(String workflowId) {
        return null;
    }

    @Override
    public void storeWcaResults(String workflowId, OnlineWorkflowWcaResults results) {

    }

    @Override
    public OnlineWorkflowWcaResults getWcaResults(String workflowId) {
        return null;
    }

    @Override
    public void storeWorkflowParameters(String workflowId, OnlineWorkflowParameters parameters) {

    }

    @Override
    public OnlineWorkflowParameters getWorkflowParameters(String workflowId) {
        DateTime dt = workflowMap.get(workflowId);
        OnlineWorkflowParameters param = new OnlineWorkflowParameters(dt, 0, new Interval(0, 0),
                "OfflineworkflowId", TimeHorizon.DACF, workflowId, 0, false, false, false, null, CaseType.FO,
                Collections.emptySet(), false, 0, false, 0);

        return param;
    }

    @Override
    public void storeStatesProcessingStatus(String workflowId,
            Map<Integer, ? extends StateProcessingStatus> statesProcessingStatus) {

    }

    @Override
    public Map<Integer, ? extends StateProcessingStatus> getStatesProcessingStatus(String workflowId) {

        Map<Integer, StateProcessingStatus> res = new HashMap<>();
        res.put(0, new StateProcessingStatus() {

            @Override
            public Map<String, String> getStatus() {
                Map<String, String> res = new HashMap<>();
                res.put("LOAD_FLOW", "SUCCESS");
                return res;
            }

            @Override
            public String getDetail() {
                return null;
            }
        });

        return res;
    }

    @Override
    public List<Integer> listStoredStates(String workflowId) {
        return null;
    }

    @Override
    public Network getState(String workflowId, Integer stateId) {
        return null;
    }

    @Override
    public void exportState(String workflowId, Integer stateId, Path folder) {

    }

    @Override
    public void exportStates(String workflowId, Path file) {

    }

    @Override
    public boolean deleteWorkflow(String workflowId) {
        return false;
    }

    @Override
    public boolean deleteStates(String workflowId) {
        return false;
    }

    @Override
    public void storeViolations(String workflowId, Integer stateId, OnlineStep step,
            List<LimitViolation> violations) {

    }

    @Override
    public List<LimitViolation> getViolations(String workflowId, Integer stateId, OnlineStep step) {
        return null;
    }

    @Override
    public Map<OnlineStep, List<LimitViolation>> getViolations(String workflowId, Integer stateId) {
        return null;
    }

    @Override
    public Map<Integer, List<LimitViolation>> getViolations(String workflowId, OnlineStep step) {
        if (!"1122".equals(workflowId) && !"1123".equals(workflowId))
            return null;
        Map<Integer, List<LimitViolation>> res = new HashMap<Integer, List<LimitViolation>>();
        List<LimitViolation> viols = new ArrayList<>();
        LimitViolation lv = new LimitViolation("sub1", LimitViolationType.CURRENT, 100, "HIGH_CURRENT", 0, 110, Country.FR, 240);
        viols.add(lv);
        res.put(0, viols);
        return res;
    }

    @Override
    public Map<Integer, Map<OnlineStep, List<LimitViolation>>> getViolations(String workflowId) {
        return null;
    }

    @Override
    public void storeWcaRulesResults(String workflowId, OnlineWorkflowRulesResults results) {
    }

    @Override
    public OnlineWorkflowRulesResults getWcaRulesResults(String workflowId) {
        return null;
    }

    @Override
    public void storePostContingencyViolations(String workflowId, Integer stateId, String contingencyId,
            boolean loadflowConverge, List<LimitViolation> violations) {
    }

    @Override
    public List<LimitViolation> getPostContingencyViolations(String workflowId, Integer stateId,
            String contingencyId) {
        return null;
    }

    @Override
    public Map<String, List<LimitViolation>> getPostContingencyViolations(String workflowId, Integer stateId) {
        return null;
    }

    @Override
    public Map<Integer, List<LimitViolation>> getPostContingencyViolations(String workflowId,
            String contingencyId) {
        return null;
    }

    @Override
    public Map<Integer, Map<String, List<LimitViolation>>> getPostContingencyViolations(String workflowId) {
        if (!"1122".equals(workflowId) && !"1123".equals(workflowId))
            return null;
        Map<Integer, Map<String, List<LimitViolation>>> res = new HashMap<>();
        Map<String, List<LimitViolation>> mmap = new HashMap<>();
        List<LimitViolation> viols = new ArrayList<>();
        LimitViolation lv = new LimitViolation("sub1", LimitViolationType.CURRENT, 100, "HIGH_CURRENT", 0, 200, Country.FR, 120);
        viols.add(lv);
        mmap.put("test_contingency", viols);
        res.put(0, mmap);
        return res;
    }

    @Override
    public Map<String, Boolean> getPostContingencyLoadflowConvergence(String workflowId, Integer stateId) {
        return null;
    }

    @Override
    public Map<Integer, Boolean> getPostContingencyLoadflowConvergence(String workflowId, String contingencyId) {
        return null;
    }

    @Override
    public Map<Integer, Map<String, Boolean>> getPostContingencyLoadflowConvergence(String workflowId) {
        if (!"1122".equals(workflowId))
            return null;
        Map<Integer, Map<String, Boolean>> res = new HashMap<>();
        Map<String, Boolean> mmap = new HashMap<>();
        mmap.put("test_contingency", Boolean.TRUE);
        res.put(0, mmap);
        return res;
    }

    @Override
    public void storeProcess(OnlineProcess proc) throws Exception {
    }

    @Override
    public List<OnlineProcess> listProcesses() throws IOException {
        return processMap.values().stream().collect(Collectors.toList());
    }

    @Override
    public OnlineProcess getProcess(String processId) throws IOException {
        if("error".equals(processId))
            throw new IOException();
        return processMap.get(processId);
    }

    @Override
    public List<String[]> getAllMetrics(String workflowId, OnlineStep step) {
        return null;
    }

    @Override
    public void storeState(String workflowId, Integer stateId, Network network, String contingencyId) {
        
    }

    @Override
    public Map<Integer, Set<String>> listStoredPostContingencyStates(String workflowId) {
        return null;
    }

    @Override
    public Network getState(String workflowId, Integer stateId, String contingencyId) {
        return null;
    }

}