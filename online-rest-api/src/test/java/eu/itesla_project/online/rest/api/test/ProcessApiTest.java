package eu.itesla_project.online.rest.api.test;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import eu.itesla_project.iidm.network.Country;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.NetworkFactory;
import eu.itesla_project.modules.contingencies.ActionParameters;
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
import eu.itesla_project.modules.optimizer.CCOFinalStatus;
import eu.itesla_project.online.rest.api.RestApplication;
import eu.itesla_project.online.rest.api.factories.ProcessApiServiceFactory;
import eu.itesla_project.online.rest.api.impl.ProcessApiServiceImpl;
import eu.itesla_project.online.rest.api.test.mock.OnlineDbFactoryMock;
import eu.itesla_project.online.rest.api.util.OnlineDBUtils;
import eu.itesla_project.online.rest.api.util.ProcessDBUtils;
import eu.itesla_project.online.rest.model.PostContingencyResult;
import eu.itesla_project.online.rest.model.PreContingencyResult;
import eu.itesla_project.online.rest.model.SimulationResult;
import eu.itesla_project.online.rest.model.Violation;
import eu.itesla_project.online.rest.model.WorkflowInfo;
import eu.itesla_project.online.rest.model.WorkflowResult;
import eu.itesla_project.security.LimitViolation;
import eu.itesla_project.security.LimitViolationType;

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
    public void testError() throws UnsupportedEncodingException {
        PowerMockito.mockStatic(ProcessApiServiceFactory.class);
        when(ProcessApiServiceFactory.getProcessApi()).thenReturn(new ProcessApiServiceImpl(dbMock));
        Client client = ClientBuilder.newClient();
        Response res = client.target(TestPortProvider.generateURL("/online-service/process/error")).request().get();
        res.readEntity(String.class);
        Assert.assertEquals("Test get process error", 500, res.getStatus());
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

    @Test
    public void testModelEquality() throws UnsupportedEncodingException {
        Date dt = new Date();
        eu.itesla_project.online.rest.model.Process p1 = new eu.itesla_project.online.rest.model.Process("1", "n", "o",
                dt, dt);
        eu.itesla_project.online.rest.model.Process p2 = new eu.itesla_project.online.rest.model.Process("1", "n", "o",
                dt, dt);
        Assert.assertEquals(p1, p2);
        Assert.assertEquals(p1.hashCode(), p2.hashCode());
        Assert.assertEquals(p1.toString(), p2.toString());
        PostContingencyResult pcr1 = new PostContingencyResult("c1", Collections.emptyList());
        PostContingencyResult pcr2 = new PostContingencyResult("c1", Collections.emptyList());
        Assert.assertEquals(pcr1, pcr2);
        Assert.assertEquals(pcr1.hashCode(), pcr2.hashCode());
        Assert.assertEquals(pcr1.toString(), pcr2.toString());
        PreContingencyResult pre1 = new PreContingencyResult(1, true, true);
        PreContingencyResult pre2 = new PreContingencyResult(1, true, true);
        Assert.assertEquals(pre1, pre2);
        Assert.assertEquals(pre1.hashCode(), pre2.hashCode());
        Assert.assertEquals(pre1.toString(), pre2.toString());
        SimulationResult s1 = new SimulationResult(1);
        SimulationResult s2 = new SimulationResult(1);
        Assert.assertEquals(s1, s2);
        Assert.assertEquals(s1.hashCode(), s2.hashCode());
        Assert.assertEquals(s1.toString(), s2.toString());
        Violation v1 = new Violation("IT", "eq1", "test", 0, 0, 0);
        Violation v2 = new Violation("IT", "eq1", "test", 0, 0, 0);
        Assert.assertEquals(v1, v2);
        Assert.assertEquals(v1.hashCode(), v2.hashCode());
        Assert.assertEquals(v1.toString(), v2.toString());
        WorkflowResult wr1 = new WorkflowResult("p1", "w1", "2017");
        WorkflowResult wr2 = new WorkflowResult("p1", "w1", "2017");
        Assert.assertEquals(wr1, wr2);
        Assert.assertEquals(wr1.hashCode(), wr2.hashCode());
        Assert.assertEquals(wr1.toString(), wr2.toString());

        WorkflowInfo w1 = new WorkflowInfo("1", "2017", wr1);
        WorkflowInfo w2 = new WorkflowInfo("1", "2017", wr2);
        Assert.assertEquals(w1, w2);
        Assert.assertEquals(w1.hashCode(), w2.hashCode());
        Assert.assertEquals(w1.toString(), w2.toString());
    }
    
    @Test
    public void testGetProcessSynthesis() throws UnsupportedEncodingException {
        PowerMockito.mockStatic(ProcessApiServiceFactory.class);
        when(ProcessApiServiceFactory.getProcessApi()).thenReturn(new ProcessApiServiceImpl(dbMock));
        Client client = ClientBuilder.newClient();
        Response res = client.target(TestPortProvider.generateURL("/online-service/synthesis/1111")).request().get();
        System.out.println(res.readEntity(String.class));
        Assert.assertEquals("Test get process synthesis by processId", 200, res.getStatus());
        client.close();
    }
    
    @Test
    public void testProcessSynthesisNotfound() throws UnsupportedEncodingException {
        PowerMockito.mockStatic(ProcessApiServiceFactory.class);
        when(ProcessApiServiceFactory.getProcessApi()).thenReturn(new ProcessApiServiceImpl(dbMock));
        Client client = ClientBuilder.newClient();
        Response res = client.target(TestPortProvider.generateURL("/online-service/synthesis/1234")).request().get();
        System.out.println(res.readEntity(String.class));
        Assert.assertEquals("Test process synthesis not found", 404, res.getStatus());
        client.close();
    }
    

}
