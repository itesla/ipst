/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.offline.db;

import com.google.common.io.ByteStreams;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import eu.itesla_project.modules.offline.*;
import com.powsybl.simulation.securityindexes.OverloadSecurityIndex;
import com.powsybl.simulation.securityindexes.SecurityIndex;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.nio.file.ShrinkWrapFileSystems;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class AbstractOfflineDbTest {

    protected static final String DB_NAME = "testdb";

    protected FileSystem fileSystem;
    protected Path tmpDir;

    public AbstractOfflineDbTest() {
    }

    @Before
    public void setUp() throws IOException {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
        fileSystem = ShrinkWrapFileSystems.newFileSystem(archive);
        tmpDir = fileSystem.getPath("tmp");
    }

    @After
    public void tearDown() throws IOException {
        fileSystem.close();
    }

    private OfflineWorkflowCreationParameters createParameters() {
        return new OfflineWorkflowCreationParameters(EnumSet.of(Country.FR), DateTime.now(), new Interval(DateTime.now(), DateTime.now().plus(1)), false, false);
    }

    protected void test(OfflineDb offlineDb) throws IOException {
        offlineDb.createWorkflow(null, createParameters());
        List<String> workflowIds = offlineDb.listWorkflows();
        assertEquals(1, workflowIds.size());
        String workflowId = "workflow-0";
        assertEquals(workflowId, workflowIds.get(0));
        Path workflowDir = tmpDir.resolve(DB_NAME).resolve(workflowId);
        assertTrue(Files.exists(workflowDir));
        assertEquals(0, offlineDb.createSample(workflowId));
        assertEquals(1, offlineDb.createSample(workflowId));
        assertEquals(2, offlineDb.getSampleCount(workflowId));
        Network n = EurostagTutorialExample1Factory.create();
        offlineDb.storeTaskStatus(workflowId, 0, OfflineTaskType.SAMPLING, OfflineTaskStatus.SUCCEED, null);
        offlineDb.storeTaskStatus(workflowId, 0, OfflineTaskType.STARTING_POINT_INITIALIZATION, OfflineTaskStatus.SUCCEED, null);
        offlineDb.storeTaskStatus(workflowId, 0, OfflineTaskType.LOAD_FLOW, OfflineTaskStatus.SUCCEED, null);
        Load l = n.getLoad("LOAD");
        l.getTerminal().setP(10);
        offlineDb.storeState(workflowId, 0, n, null);
        l.getTerminal().setP(11);
        offlineDb.storeState(workflowId, 1, n, null);
        offlineDb.storeTaskStatus(workflowId, 0, OfflineTaskType.STABILIZATION, OfflineTaskStatus.SUCCEED, null);
        offlineDb.storeTaskStatus(workflowId, 0, OfflineTaskType.IMPACT_ANALYSIS, OfflineTaskStatus.SUCCEED, null);
        SecurityIndex si = new OverloadSecurityIndex("NHV1_NHV2_1", 0.5);
        offlineDb.storeSecurityIndexes(workflowId, 0, Arrays.asList(si));
        assertEquals(1, offlineDb.getSecurityIndexIds(workflowId).size());
        assertEquals(si.getId(), offlineDb.getSecurityIndexIds(workflowId).iterator().next());
        StringWriter writer = new StringWriter();
        offlineDb.exportCsv(workflowId, writer, new OfflineDbCsvExportConfig(';', OfflineAttributesFilter.ALL, false, true));
        writer.close();
        String offlineDbCsvRef = new String(ByteStreams.toByteArray(getClass().getResourceAsStream("/offlinedb.csv")), StandardCharsets.UTF_8);    
        String[] lines1= new BufferedReader(new StringReader(writer.toString())).lines().toArray(String[]::new); 
        String[] lines2= new BufferedReader(new StringReader(offlineDbCsvRef)).lines().toArray(String[]::new);
        
        assertTrue(Arrays.equals(lines1, lines2));
      //  Assert.assertTrue(writer.toString().equals(offlineDbCsvRef));
        offlineDb.deleteWorkflow(workflowId);
        assertTrue(offlineDb.listWorkflows().isEmpty());
        assertFalse(Files.exists(workflowDir));
    }

}
