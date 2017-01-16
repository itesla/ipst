package eu.itesla_project.online.rest.api.test;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.test.TestPortProvider;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import eu.itesla_project.cases.CaseType;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.modules.online.OnlineDb;
import eu.itesla_project.modules.online.OnlineDbFactory;
import eu.itesla_project.modules.online.OnlineProcess;
import eu.itesla_project.modules.online.OnlineStep;
import eu.itesla_project.modules.online.OnlineWorkflowDetails;
import eu.itesla_project.modules.online.OnlineWorkflowParameters;
import eu.itesla_project.modules.online.OnlineWorkflowResults;
import eu.itesla_project.modules.online.OnlineWorkflowRulesResults;
import eu.itesla_project.modules.online.OnlineWorkflowWcaResults;
import eu.itesla_project.modules.online.StateProcessingStatus;
import eu.itesla_project.modules.online.TimeHorizon;
import eu.itesla_project.online.rest.api.RestApplication;
import eu.itesla_project.online.rest.api.factories.ProcessApiServiceFactory;
import eu.itesla_project.online.rest.api.impl.ProcessApiServiceImpl;
import eu.itesla_project.online.rest.api.util.OnlineDBUtils;
import eu.itesla_project.online.rest.api.util.ProcessDBUtils;
import eu.itesla_project.security.LimitViolation;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ProcessApiServiceFactory.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })

public class ProcessApiTest {

    private static UndertowJaxrsServer server;
    private final ProcessDBUtils dbMock = new OnlineDBUtils(new OnlineDbFactoryMock());

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        server = new UndertowJaxrsServer().start();

        server.deploy(RestApplication.class, "online-service");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        server.stop();
    }

    @Test
    public void testGetProcesses() throws UnsupportedEncodingException {
        PowerMockito.mockStatic(ProcessApiServiceFactory.class);
        when(ProcessApiServiceFactory.getProcessApi()).thenReturn(new ProcessApiServiceImpl(dbMock));

        // get all processes
        Client client = ClientBuilder.newClient();
        Response res = client.target(TestPortProvider.generateURL("/online-service/process")).request().get();
        Assert.assertEquals(200, res.getStatus());
        JSONArray arr = new JSONArray(res.readEntity(String.class));
        Assert.assertEquals("Test get all processes", 2, arr.length());
        client.close();
    }

    @Test
    public void testGetByBasecase() throws UnsupportedEncodingException {
        PowerMockito.mockStatic(ProcessApiServiceFactory.class);
        when(ProcessApiServiceFactory.getProcessApi()).thenReturn(new ProcessApiServiceImpl(dbMock));
        Client client = ClientBuilder.newClient();
        Response res = client.target(TestPortProvider.generateURL("/online-service/process"))
                .queryParam("basecase", "2016-01-11T02:00:00.000+00:00").request().get();
        Assert.assertEquals(200, res.getStatus());
        JSONArray arr = new JSONArray(res.readEntity(String.class));
        Assert.assertEquals(1, arr.length());
        JSONObject p = arr.getJSONObject(0);
        JSONArray arr2 = p.getJSONArray("workflows");
        JSONObject wf = arr2.getJSONObject(0);
        Assert.assertEquals("Test filter by basecase", "2016-01-11T02:00:00.000+00:00", wf.get("baseCase"));
        client.close();
    }

    @Test
    public void testGetByName() throws UnsupportedEncodingException {
        PowerMockito.mockStatic(ProcessApiServiceFactory.class);
        when(ProcessApiServiceFactory.getProcessApi()).thenReturn(new ProcessApiServiceImpl(dbMock));
        Client client = ClientBuilder.newClient();
        Response res = client.target(TestPortProvider.generateURL("/online-service/process"))
                .queryParam("name", "name2").request().get();
        Assert.assertEquals(200, res.getStatus());
        JSONArray arr = new JSONArray(res.readEntity(String.class));
        Assert.assertEquals(1, arr.length());
        JSONObject pn = arr.getJSONObject(0);
        Assert.assertEquals("Test filter by name", "name2", pn.get("name"));
        client.close();
    }

    @Test
    public void testGetByOwner() throws UnsupportedEncodingException {
        PowerMockito.mockStatic(ProcessApiServiceFactory.class);
        when(ProcessApiServiceFactory.getProcessApi()).thenReturn(new ProcessApiServiceImpl(dbMock));
        Client client = ClientBuilder.newClient();
        Response res = client.target(TestPortProvider.generateURL("/online-service/process"))
                .queryParam("owner", "owner1").request().get();
        Assert.assertEquals(200, res.getStatus());
        JSONArray arr = new JSONArray(res.readEntity(String.class));
        Assert.assertEquals(1, arr.length());
        JSONObject obj = arr.getJSONObject(0);
        Assert.assertEquals("Test filter by owner", "owner1", obj.get("owner"));
        client.close();
    }

    @Test
    public void testGetByDate() throws UnsupportedEncodingException {
        PowerMockito.mockStatic(ProcessApiServiceFactory.class);
        when(ProcessApiServiceFactory.getProcessApi()).thenReturn(new ProcessApiServiceImpl(dbMock));
        Client client = ClientBuilder.newClient();
        DateTime dt = new DateTime(2016, 1, 16, 01, 0, 0, 0);
        String date = dt.toDateTimeISO().toString();
        long dt_millis = dt.getMillis();
        Response res = client.target(TestPortProvider.generateURL("/online-service/process"))
                .queryParam("date", URLEncoder.encode(date, "UTF-8")).request().get();
        Assert.assertEquals(200, res.getStatus());
        JSONArray arr = new JSONArray(res.readEntity(String.class));
        Assert.assertEquals(1, arr.length());
        JSONObject pn = arr.getJSONObject(0);
        long millis = DateTime.parse((String) pn.get("date")).getMillis();
        Assert.assertEquals("Test filter by date", dt_millis, millis);
        client.close();
    }

    @Test
    public void testWrongParamFormat() throws UnsupportedEncodingException {
        PowerMockito.mockStatic(ProcessApiServiceFactory.class);
        when(ProcessApiServiceFactory.getProcessApi()).thenReturn(new ProcessApiServiceImpl(dbMock));
        Client client = ClientBuilder.newClient();
        // wrong date format returns 400 error code
        Response res = client.target(TestPortProvider.generateURL("/online-service/process"))
                .queryParam("date", "YYYY-WWW").request().get();
        Assert.assertEquals("Test wrong date parameter format", 400, res.getStatus());
        res.readEntity(String.class);
        client.close();
    }

    @Test
    public void testGetByCreationDate() throws UnsupportedEncodingException {
        PowerMockito.mockStatic(ProcessApiServiceFactory.class);
        when(ProcessApiServiceFactory.getProcessApi()).thenReturn(new ProcessApiServiceImpl(dbMock));
        Client client = ClientBuilder.newClient();
        DateTime dt = new DateTime(2016, 1, 15, 01, 10, 0, 0);
        String date = dt.toDateTimeISO().toString();
        long dt_millis = dt.getMillis();
        Response res = client.target(TestPortProvider.generateURL("/online-service/process"))
                .queryParam("creationDate", URLEncoder.encode(date, "UTF-8")).request().get();
        Assert.assertEquals(200, res.getStatus());

        JSONArray arr = new JSONArray(res.readEntity(String.class));
        Assert.assertEquals(1, arr.length());
        JSONObject pn = arr.getJSONObject(0);
        long millis = DateTime.parse((String) pn.get("creationDate")).getMillis();
        Assert.assertEquals("Test filter by creation date", dt_millis, millis);
        client.close();
    }

    @Test
    public void testGetByProcessId() throws UnsupportedEncodingException {
        PowerMockito.mockStatic(ProcessApiServiceFactory.class);
        when(ProcessApiServiceFactory.getProcessApi()).thenReturn(new ProcessApiServiceImpl(dbMock));
        Client client = ClientBuilder.newClient();
        Response res = client.target(TestPortProvider.generateURL("/online-service/process/2222")).request().get();
        res.readEntity(String.class);
        Assert.assertEquals("Test get workflow by processId", 200, res.getStatus());
        client.close();
    }

    @Test
    public void testProcessNotFound() throws UnsupportedEncodingException {
        PowerMockito.mockStatic(ProcessApiServiceFactory.class);
        when(ProcessApiServiceFactory.getProcessApi()).thenReturn(new ProcessApiServiceImpl(dbMock));
        Client client = ClientBuilder.newClient();
        Response res = client.target(TestPortProvider.generateURL("/online-service/process/0000")).request().get();
        res.readEntity(String.class);
        Assert.assertEquals("Test processId not found", 404, res.getStatus());
        client.close();
    }

    @Test
    public void testGetByProcessAndWorkflowId() throws UnsupportedEncodingException {
        PowerMockito.mockStatic(ProcessApiServiceFactory.class);
        when(ProcessApiServiceFactory.getProcessApi()).thenReturn(new ProcessApiServiceImpl(dbMock));
        Client client = ClientBuilder.newClient();
        // get workflow by processId and WorkflowId
        Response res = client.target(TestPortProvider.generateURL("/online-service/process/2222/2233")).request().get();
        res.readEntity(String.class);
        Assert.assertEquals("Test get workflow by processId and workflowId", 200, res.getStatus());
        client.close();
    }

    @Test
    public void testWorkflowNotFound() throws UnsupportedEncodingException {
        PowerMockito.mockStatic(ProcessApiServiceFactory.class);
        when(ProcessApiServiceFactory.getProcessApi()).thenReturn(new ProcessApiServiceImpl(dbMock));
        Client client = ClientBuilder.newClient();
        // get workflow by processId and WorkflowId
        Response res = client.target(TestPortProvider.generateURL("/online-service/process/2222/8888")).request().get();
        res.readEntity(String.class);
        Assert.assertEquals("Test workflowId not found", 404, res.getStatus());
        client.close();
    }

    private class OnlineDbFactoryMock implements OnlineDbFactory {

        @Override
        public OnlineDb create() {
            return new OnlineDbMock();
        }

    }

    private class OnlineDbMock implements OnlineDb {

        private Map<String, OnlineProcess> processMap;
        private Map<String, DateTime> workflowMap;

        public OnlineDbMock() {
            processMap = new HashMap<String, OnlineProcess>();
            DateTime dt = new DateTime(2016, 1, 15, 01, 0, 0, 0);
            OnlineProcess p = new OnlineProcess("1111", "name1", "owner1", CaseType.FO.toString(), dt,
                    dt.plusMinutes(10));
            p.addWorkflow("2016-01-10T01:00:00.000+00:00", "1122");
            dt = new DateTime(2016, 1, 16, 01, 0, 0, 0);
            OnlineProcess p2 = new OnlineProcess("2222", "name2", "owwer2", CaseType.FO.toString(), dt,
                    dt.plusMinutes(10));
            p2.addWorkflow("2016-01-11T02:00:00.000+00:00", "2233");

            processMap.put(p.getId(), p);
            processMap.put(p2.getId(), p2);

            workflowMap = new HashMap<String, DateTime>();

            DateTime wdt = new DateTime(2016, 1, 10, 01, 0, 0, 0);
            workflowMap.put("1122", wdt);

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
            return null;
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
            return null;
        }

        @Override
        public void storeState(String workflowId, Integer stateId, Network network) {

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
            return null;
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
            return null;
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
            return null;
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
            return processMap.get(processId);
        }

        @Override
        public List<String[]> getAllMetrics(String workflowId, OnlineStep step) {
            return null;
        }

    }

}
