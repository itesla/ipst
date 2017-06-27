package eu.itesla_project.online.rest.api.test;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import eu.itesla_project.online.rest.api.DateTimeParameter;
import eu.itesla_project.online.rest.api.ProcessApiService;
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


public class ProcessApiTest {

    private final ProcessDBUtils dbMock = new OnlineDBUtils(new OnlineDbFactoryMock());
    private final ProcessApiService service = new ProcessApiServiceImpl(dbMock);

    @Test
    public void testGetProcesses() throws UnsupportedEncodingException {
        Response res = service.getProcessList(null, null, null, null, null, null);
        Assert.assertEquals(200, res.getStatus());
        JSONArray arr = new JSONArray((String) res.getEntity());
        Assert.assertEquals("Test get all processes", 2, arr.length());
    }

    @Test
    public void testGetByBasecase() throws UnsupportedEncodingException {
        Response res = service.getProcessList(null, "2016-01-11T02:00:00.000+00:00", null, null, null, null);
        Assert.assertEquals(200, res.getStatus());
        JSONArray arr = new JSONArray((String) res.getEntity());
        Assert.assertEquals(1, arr.length());
        JSONObject p = arr.getJSONObject(0);
        JSONArray arr2 = p.getJSONArray("workflows");
        JSONObject wf = arr2.getJSONObject(0);
        Assert.assertEquals("Test filter by basecase", "2016-01-11T02:00:00.000+00:00", wf.get("baseCase"));
    }

    @Test
    public void testGetByName() throws UnsupportedEncodingException {
        Response res = service.getProcessList(null, null, "name2", null, null, null);
        Assert.assertEquals(200, res.getStatus());
        JSONArray arr = new JSONArray((String) res.getEntity());
        Assert.assertEquals(1, arr.length());
        JSONObject pn = arr.getJSONObject(0);
        Assert.assertEquals("Test filter by name", "name2", pn.get("name"));
    }

    @Test
    public void testGetByOwner() throws UnsupportedEncodingException {
        Response res = service.getProcessList("owner1", null, null, null, null, null);
        Assert.assertEquals(200, res.getStatus());
        JSONArray arr = new JSONArray((String) res.getEntity());
        Assert.assertEquals(1, arr.length());
        JSONObject obj = arr.getJSONObject(0);
        Assert.assertEquals("Test filter by owner", "owner1", obj.get("owner"));
    }

    @Test
    public void testGetByDate() throws Exception {
        DateTime dt = new DateTime(2016, 1, 16, 01, 0, 0, 0);
        String date = dt.toDateTimeISO().toString();
        long dt_millis = dt.getMillis();

        Response res = service.getProcessList(null, null, null, new DateTimeParameter(date), null, null);
        Assert.assertEquals(200, res.getStatus());
        JSONArray arr = new JSONArray((String) res.getEntity());
        Assert.assertEquals(1, arr.length());
        JSONObject pn = arr.getJSONObject(0);
        long millis = DateTime.parse((String) pn.get("date")).getMillis();
        Assert.assertEquals("Test filter by date", dt_millis, millis);
    }

    @Test
    public void testWrongParamFormat() throws UnsupportedEncodingException {
        try{
            DateTimeParameter dt = new DateTimeParameter("YYYY-WWW");
            Assert.fail("Wrong date format accepted");
        }
        catch(Exception ex)
        {
            Response res = ((WebApplicationException) ex).getResponse();
            Assert.assertEquals("Test wrong date parameter format", 400, res.getStatus());
            return;
        }
       
    }

    @Test
    public void testGetByCreationDate() throws Exception {
        DateTime dt = new DateTime(2016, 1, 15, 01, 10, 0, 0);
        String date = dt.toDateTimeISO().toString();
        long dt_millis = dt.getMillis();
        Response res = service.getProcessList(null, null, null, null, new DateTimeParameter(date), null);
        Assert.assertEquals(200, res.getStatus());
        JSONArray arr = new JSONArray((String) res.getEntity());
        Assert.assertEquals(1, arr.length());
        JSONObject pn = arr.getJSONObject(0);
        long millis = DateTime.parse((String) pn.get("creationDate")).getMillis();
        Assert.assertEquals("Test filter by creation date", dt_millis, millis);
    }

    @Test
    public void testGetByProcessId() throws UnsupportedEncodingException {
        Response res = service.getProcessById("2222", null);
        Assert.assertEquals("Test get workflow by processId", 200, res.getStatus());
    }

    @Test
    public void testProcessNotFound() throws UnsupportedEncodingException {
        Response res = service.getProcessById("0000", null);
        Assert.assertEquals("Test processId not found", 404, res.getStatus());
    }
    
    @Test
    public void testError() throws UnsupportedEncodingException {
        Response res = service.getProcessById("error", null);
        Assert.assertEquals("Test get process error", 500, res.getStatus());
    }

    @Test
    public void testGetByProcessAndWorkflowId() throws UnsupportedEncodingException {
        Response res = service.getWorkflowResult("2222", "2233", null);
        Assert.assertEquals("Test get workflow by processId and workflowId", 200, res.getStatus());
    }

    @Test
    public void testWorkflowNotFound() throws UnsupportedEncodingException {
        Response res = service.getWorkflowResult("2222", "8888", null);
        Assert.assertEquals("Test workflowId not found", 404, res.getStatus());
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
        Response res = service.getProcessSynthesis("1111", null);
        Assert.assertEquals("Test get process synthesis by processId", 200, res.getStatus());
    }
    
    @Test
    public void testProcessSynthesisNotfound() throws UnsupportedEncodingException {
        Response res = service.getProcessSynthesis("1234", null);
        Assert.assertEquals("Test process synthesis not found", 404, res.getStatus());
    }
    

}
