/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import eu.itesla_project.commons.util.StringToIntMapper;
import eu.itesla_project.contingency.BranchContingency;
import eu.itesla_project.contingency.Contingency;
import eu.itesla_project.contingency.ContingencyElement;
import eu.itesla_project.contingency.ContingencyImpl;
import eu.itesla_project.iidm.datasource.MemDataSource;
import eu.itesla_project.iidm.export.ampl.AmplSubset;
import eu.itesla_project.iidm.network.Identifiable;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.StateManager;
import eu.itesla_project.iidm.network.test.NetworkTest1Factory;
import eu.itesla_project.modules.wca.WCAClusterNum;
import eu.itesla_project.security.LimitViolation;
import eu.itesla_project.security.LimitViolationType;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCAUtilsTest {

    private FileSystem fileSystem;

    @Before
    public void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    public void testExportState() throws IOException, URISyntaxException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));
        Network network = NetworkTest1Factory.create();
        network.setCaseDate(new DateTime(1483228800000L).withZone(DateTimeZone.UTC));
        WCAUtils.exportState(network, folder, 0, 0);
        Path exportedState = folder.resolve(network.getId() + "_0_0.xiidm.gz");
        assertTrue(Files.exists(exportedState));

        try (GZIPInputStream stream = new GZIPInputStream(Files.newInputStream(exportedState))) {
            assertTrue(IOUtils.contentEquals(getClass().getResourceAsStream("/network1.xiidm"), stream));
        }
    }

    private void testInjection(Map<String, Float> injections, String injection, float value) {
        assertTrue(injections.containsKey(injection));
        assertEquals(value, injections.get(injection), 0);
    }

    @Test
    public void testReadDomainsResult() throws URISyntaxException, IOException {
        Path workingDir = Paths.get(getClass().getResource("/domains").toURI());

        WCADomainsResult domainsResult = WCAUtils.readDomainsResult("wca_domains_1", workingDir, "wca_uncertainties.txt");
        assertFalse(domainsResult.foundBasicViolations());
        assertFalse(domainsResult.areRulesViolated());
        assertEquals(0, domainsResult.getPreventiveActionIndex());
        testInjection(domainsResult.getInjections(), "LOAD_1", -1.1f);
        testInjection(domainsResult.getInjections(), "LOAD_2", 1.5f);
        testInjection(domainsResult.getInjections(), "GEN_1", 1.2f);

        domainsResult = WCAUtils.readDomainsResult("wca_domains_2", workingDir, "wca_uncertainties.txt");
        assertTrue(domainsResult.foundBasicViolations());
        assertFalse(domainsResult.areRulesViolated());
        assertEquals(0, domainsResult.getPreventiveActionIndex());
        testInjection(domainsResult.getInjections(), "LOAD_1", -1.1f);
        testInjection(domainsResult.getInjections(), "LOAD_2", 1.5f);
        testInjection(domainsResult.getInjections(), "GEN_1", 1.2f);
    }

    @Test
    public void testReadClustersResults() throws URISyntaxException, IOException {
        Path workingDir = Paths.get(getClass().getResource("/clusters").toURI());

        WCAClustersResult clustersResult =  WCAUtils.readClustersResult("wca_clusters_1", workingDir, "wca_flow_1.txt", "wca_uncertainties.txt");
        assertEquals(WCAClusterNum.TWO, clustersResult.getClusterNum());
        assertFalse(clustersResult.foundViolations());
        assertEquals(1, clustersResult.getCurativeActionIndex());
        testInjection(clustersResult.getInjections(), "LOAD_1", -1.1f);
        testInjection(clustersResult.getInjections(), "LOAD_2", 1.5f);
        testInjection(clustersResult.getInjections(), "GEN_1", 1.2f);

        clustersResult =  WCAUtils.readClustersResult("wca_clusters_2", workingDir, "wca_flow_2.txt", "wca_uncertainties.txt");
        assertEquals(WCAClusterNum.ONE, clustersResult.getClusterNum());
        assertTrue(clustersResult.foundViolations());
        assertEquals(0, clustersResult.getCurativeActionIndex());
        testInjection(clustersResult.getInjections(), "LOAD_1", -1.1f);
        testInjection(clustersResult.getInjections(), "LOAD_2", 1.5f);
        testInjection(clustersResult.getInjections(), "GEN_1", 1.2f);
    }

    private void assertEqualsToRef(MemDataSource dataSource, String fileSuffix, String title, String id) {
        String fileContent = "#" + title + System.lineSeparator()  + 
                             "#\"num\" \"id\"" + System.lineSeparator() +
                             "1 \"" + id + "\"" + System.lineSeparator();
        assertEquals(fileContent, new String(dataSource.getData(fileSuffix, WCAConstants.TXT_EXT), StandardCharsets.UTF_8));
    }

    @Test
    public void testWriteContingencies() {
        ContingencyElement contingencyElement = new BranchContingency("line");
        Contingency contingency = new ContingencyImpl("contigency_1", contingencyElement);
        MemDataSource dataSource = new MemDataSource();
        StringToIntMapper<AmplSubset> mapper = new StringToIntMapper<>(AmplSubset.class);

        mapper.newInt(AmplSubset.FAULT, contingency.getId());
        WCAUtils.writeContingencies(Collections.singleton(contingency), dataSource, mapper);
        assertEqualsToRef(dataSource, WCAConstants.FAULTS_FILE_SUFFIX, "Contingencies", "contigency_1");
    }

    @Test
    public void testWriteActions() {
        MemDataSource dataSource = new MemDataSource();
        StringToIntMapper<AmplSubset> mapper = new StringToIntMapper<>(AmplSubset.class);

        mapper.newInt(AmplSubset.CURATIVE_ACTION, "action_1");
        WCAUtils.writeActions(Collections.singleton("action_1"), dataSource, mapper, "Curative actions", AmplSubset.CURATIVE_ACTION);
        assertEqualsToRef(dataSource, WCAConstants.ACTIONS_FILE_SUFFIX, "Curative actions", "action_1");

        mapper.newInt(AmplSubset.PREVENTIVE_ACTION, "action_1");
        WCAUtils.writeActions(Collections.singleton("action_1"), dataSource, mapper, "Preventive actions", AmplSubset.PREVENTIVE_ACTION);
        assertEqualsToRef(dataSource, WCAConstants.ACTIONS_FILE_SUFFIX, "Preventive actions", "action_1");
    }

    @Test
    public void testApplyInjections() {
        Network network = NetworkTest1Factory.create();
        // fix missing terminal values
        network.getGenerator("generator1").getTerminal().setP(-network.getGenerator("generator1").getTargetP());
        network.getLoad("load1").getTerminal().setP(network.getLoad("load1").getP0());

        float loadP = network.getLoad("load1").getTerminal().getP();
        float loadP0 = network.getLoad("load1").getP0();
        float generatorP = network.getGenerator("generator1").getTerminal().getP();
        float generatorTargetP = network.getGenerator("generator1").getTargetP();

        Map<String, Float> injections = new HashMap<String, Float>();
        injections.put("load1", 10f);
        injections.put("generator1", -10f);
        WCAUtils.applyInjections(network, StateManager.INITIAL_STATE_ID, injections);

        assertEquals(loadP+10, network.getLoad("load1").getTerminal().getP(), 0);
        assertEquals(loadP0+10, network.getLoad("load1").getP0(), 0);
        assertEquals(generatorP-10, network.getGenerator("generator1").getTerminal().getP(), 0);
        assertEquals(generatorTargetP+10, network.getGenerator("generator1").getTargetP(), 0);
    }

    @Test
    public void testContainsViolation() {
        Identifiable line1 = Mockito.mock(Identifiable.class);
        Mockito.when(line1.getId()).thenReturn("line1");
        LimitViolation line1Violation = new LimitViolation(line1, LimitViolationType.CURRENT, 1000f, "10'", 1100f);
        Identifiable line2 = Mockito.mock(Identifiable.class);
        Mockito.when(line2.getId()).thenReturn("line2");
        LimitViolation line2Violation = new LimitViolation(line2, LimitViolationType.CURRENT, 900f, "20'", 950f);
        Identifiable line3 = Mockito.mock(Identifiable.class);
        Mockito.when(line3.getId()).thenReturn("line3");
        LimitViolation line3Violation = new LimitViolation(line3, LimitViolationType.CURRENT, 1000f, "30'", 1300f);

        assertFalse(WCAUtils.containsViolation(Arrays.asList(line1Violation, line3Violation), line2Violation));
        assertTrue(WCAUtils.containsViolation(Arrays.asList(line1Violation, line2Violation, line3Violation), line2Violation));
        
    }

}
