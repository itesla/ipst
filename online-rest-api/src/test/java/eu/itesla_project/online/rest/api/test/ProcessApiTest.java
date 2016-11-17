package eu.itesla_project.online.rest.api.test;

import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.test.TestPortProvider;
import org.joda.time.DateTime;
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

import eu.itesla_project.online.rest.api.DateTimeParameter;
import eu.itesla_project.online.rest.api.RestApplication;
import eu.itesla_project.online.rest.api.factories.ProcessApiServiceFactory;
import eu.itesla_project.online.rest.api.impl.ProcessApiServiceImpl;
import eu.itesla_project.online.rest.api.util.ProcessDBUtils;
import eu.itesla_project.online.rest.model.Process;
import eu.itesla_project.online.rest.model.WorkflowInfo;
import eu.itesla_project.online.rest.model.WorkflowResult;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ProcessApiServiceFactory.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })

public class ProcessApiTest {

	private static UndertowJaxrsServer server;
	private final ProcessDBMock dbMock = new ProcessDBMock();


	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		server = new UndertowJaxrsServer().start();
		PowerMockito.mockStatic(ProcessApiServiceFactory.class);

		server.deploy(RestApplication.class, "online-service");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		server.stop();
	}

	@Test
	public void testProcess() throws UnsupportedEncodingException {

		when(ProcessApiServiceFactory.getProcessApi()).thenReturn(new ProcessApiServiceImpl(dbMock));

		// get all processes
		Client client = ClientBuilder.newClient();
		Response res = client.target(TestPortProvider.generateURL("/online-service/process")).request().get();
		Assert.assertEquals(200, res.getStatus());
		JSONArray arr = new JSONArray(res.readEntity(String.class));
		Assert.assertEquals("Test get all processes", 2, arr.length());

		// get processes filtered by basecase
		res = client.target(TestPortProvider.generateURL("/online-service/process"))
				.queryParam("basecase", "2016-01-11T02:00:00.000+00:00").request().get();
		Assert.assertEquals(200, res.getStatus());

		arr = new JSONArray(res.readEntity(String.class));
		Assert.assertEquals(1, arr.length());
		JSONObject p = arr.getJSONObject(0);
		JSONArray arr2 = p.getJSONArray("workflows");
		JSONObject wf = arr2.getJSONObject(0);
		Assert.assertEquals("Test filter by basecase", "2016-01-11T02:00:00.000+00:00", wf.get("baseCase"));

		// get processes filtered by name
		res = client.target(TestPortProvider.generateURL("/online-service/process")).queryParam("name", "name2")
				.request().get();
		Assert.assertEquals(200, res.getStatus());

		arr = new JSONArray(res.readEntity(String.class));
		Assert.assertEquals(1, arr.length());
		JSONObject pn = arr.getJSONObject(0);

		Assert.assertEquals("Test filter by name", "name2", pn.get("name"));

		// get processes filtered by owner
		res = client.target(TestPortProvider.generateURL("/online-service/process")).queryParam("owner", "owner1")
				.request().get();
		Assert.assertEquals(200, res.getStatus());

		arr = new JSONArray(res.readEntity(String.class));
		Assert.assertEquals(1, arr.length());
		pn = arr.getJSONObject(0);

		Assert.assertEquals("Test filter by owner", "owner1", pn.get("owner"));

		// get processes filtered by date
		DateTime dt = new DateTime(2016, 1, 16, 01, 0, 0, 0);
		String date = dt.toDateTimeISO().toString();
		long dt_millis = dt.getMillis();
		res = client.target(TestPortProvider.generateURL("/online-service/process"))
				.queryParam("date", URLEncoder.encode(date, "UTF-8")).request().get();
		Assert.assertEquals(200, res.getStatus());

		arr = new JSONArray(res.readEntity(String.class));
		Assert.assertEquals(1, arr.length());
		pn = arr.getJSONObject(0);
		long millis = DateTime.parse((String) pn.get("date")).getMillis();
		Assert.assertEquals("Test filter by date", dt_millis, millis);

		// wrong date format returns 400 error code
		res = client.target(TestPortProvider.generateURL("/online-service/process")).queryParam("date", "YYYY-WWW")
				.request().get();
		Assert.assertEquals("Test wrong date parameter format", 400, res.getStatus());
		res.readEntity(String.class);

		// get processes filtered by creationdate
		dt = new DateTime(2016, 1, 15, 01, 10, 0, 0);
		date = dt.toDateTimeISO().toString();
		dt_millis = dt.getMillis();
		res = client.target(TestPortProvider.generateURL("/online-service/process"))
				.queryParam("creationDate", URLEncoder.encode(date, "UTF-8")).request().get();
		Assert.assertEquals(200, res.getStatus());

		arr = new JSONArray(res.readEntity(String.class));
		Assert.assertEquals(1, arr.length());
		pn = arr.getJSONObject(0);
		millis = DateTime.parse((String) pn.get("creationDate")).getMillis();
		Assert.assertEquals("Test filter by creation date", dt_millis, millis);

		// get process by processId
		res = client.target(TestPortProvider.generateURL("/online-service/process/2222")).request().get();
		res.readEntity(String.class);
		Assert.assertEquals("Test get workflow by processId", 200, res.getStatus());

		// processId not found
		res = client.target(TestPortProvider.generateURL("/online-service/process/0000")).request().get();
		res.readEntity(String.class);
		Assert.assertEquals("Test processId not found", 404, res.getStatus());

		// get workflow by processId and WorkflowId
		res = client.target(TestPortProvider.generateURL("/online-service/process/2222/2233")).request().get();
		res.readEntity(String.class);
		Assert.assertEquals("Test get workflow by processId and workflowId", 200, res.getStatus());

		// get workflow by processId and WorkflowId
		res = client.target(TestPortProvider.generateURL("/online-service/process/2222/8888")).request().get();
		res.readEntity(String.class);
		Assert.assertEquals("Test workflowId not found", 404, res.getStatus());

		client.close();
	}

	private class ProcessDBMock implements ProcessDBUtils {
		List<Process> processes;

		ProcessDBMock() {
			processes = new ArrayList<Process>();
			Process p = new Process();
			p.setId("1111");
			p.setName("name1");
			p.setOwner("owner1");
			DateTime dt = new DateTime(2016, 1, 15, 01, 0, 0, 0);

			p.setDate(dt.toDate());

			p.setCreationDate(dt.plusMinutes(10).toDate());
			List<WorkflowInfo> workflows = new ArrayList<WorkflowInfo>();
			WorkflowInfo wi = new WorkflowInfo();
			wi.setBaseCase("2016-01-10T01:00:00.000+00:00");
			wi.setId("1122");
			WorkflowResult workflowResult = new WorkflowResult();
			workflowResult.setBasecase("2016-01-10T01:00:00.000+00:00");
			workflowResult.setProcessId("1111");
			workflowResult.setWorkflowId("1122");

			wi.setWorkflowResult(workflowResult);
			workflows.add(wi);
			p.setWorkflows(workflows);
			processes.add(p);

			Process p2 = new Process();
			p2.setId("2222");
			p2.setName("name2");
			p2.setOwner("owwer2");
			dt = new DateTime(2016, 1, 16, 01, 0, 0, 0);

			p2.setDate(dt.toDate());
			dt.plusMinutes(10);
			p2.setCreationDate(dt.plusMinutes(10).toDate());
			List<WorkflowInfo> workflows2 = new ArrayList<WorkflowInfo>();
			WorkflowInfo wi2 = new WorkflowInfo();
			wi2.setBaseCase("2016-01-11T02:00:00.000+00:00");
			wi2.setId("2233");
			WorkflowResult workflowResult2 = new WorkflowResult();
			workflowResult2.setBasecase("2016-01-11T02:00:00.000+00:00");
			workflowResult2.setProcessId("2222");
			workflowResult2.setWorkflowId("2233");

			wi2.setWorkflowResult(workflowResult2);
			workflows2.add(wi2);
			p2.setWorkflows(workflows2);
			processes.add(p2);
		}

		@Override
		public List<Process> listProcesses(String owner, String basecase, String name, DateTimeParameter date,
				DateTimeParameter creationDate) {

			List<Process> filtered_processes = processes.stream().filter(new Predicate<Process>() {

				@Override
				public boolean test(Process p) {
					boolean res = true;
					if (name != null)
						res = res && name.equals(p.getName());
					if (owner != null)
						res = res && owner.equals(p.getOwner());
					if (basecase != null) {
						res = res && p.getWorkflows().stream().map(w -> w.getBaseCase()).collect(Collectors.toList())
								.contains(basecase);
					}
					if (date != null)
						res = res && date.getDateTime().getMillis() == p.getDate().getTime();
					if (creationDate != null)
						res = res && creationDate.getDateTime().getMillis() == p.getCreationDate().getTime();
					return res;
				}
			}).collect(Collectors.toList());

			return filtered_processes;
		}

		@Override
		public Process getProcess(String processId) {
			for (Process p : processes) {
				if (p.getId().equals(processId))
					return p;
			}
			return null;
		}

		@Override
		public WorkflowResult getWorkflowResult(String processId, String workflowId) {

			for (Process p : processes) {
				if (p.getId().equals(processId)) {
					for (WorkflowInfo w : p.getWorkflows()) {
						if (w.getId().equals(workflowId))
							return w.getWorkflowResult();
					}
				}
			}
			return null;

		}

	}

}
