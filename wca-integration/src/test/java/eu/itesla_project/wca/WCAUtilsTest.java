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

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.StateManagerConstants;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.NetworkTest1Factory;
import eu.itesla_project.modules.wca.WCAClusterNum;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationType;

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
        Contingency contingency = new Contingency("contigency_1", contingencyElement);
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

        double loadP = network.getLoad("load1").getTerminal().getP();
        double loadP0 = network.getLoad("load1").getP0();
        double generatorP = network.getGenerator("generator1").getTerminal().getP();
        double generatorTargetP = network.getGenerator("generator1").getTargetP();

        Map<String, Float> injections = new HashMap<String, Float>();
        injections.put("load1", 10f);
        injections.put("generator1", -10f);
        WCAUtils.applyInjections(network, StateManagerConstants.INITIAL_STATE_ID, injections);

        assertEquals(loadP + 10, network.getLoad("load1").getTerminal().getP(), 0.0);
        assertEquals(loadP0 + 10, network.getLoad("load1").getP0(), 0.0);
        assertEquals(generatorP - 10, network.getGenerator("generator1").getTerminal().getP(), 0.0);
        assertEquals(generatorTargetP + 10, network.getGenerator("generator1").getTargetP(), 0.0);
    }

    @Test
    public void testContainsViolation() {
        LimitViolation line1Violation = new LimitViolation("line1", LimitViolationType.CURRENT, "10'",10*60, 1f, 1000f, 1100f, Branch.Side.ONE);
        LimitViolation line2Violation = new LimitViolation("line2", LimitViolationType.CURRENT, "20'", 20*60, 1f, 900f, 950f, Branch.Side.ONE);
        LimitViolation line3Violation = new LimitViolation("line3", LimitViolationType.CURRENT, "30'", 30*60, 1000f, 1f, 1300f, Branch.Side.ONE);

        assertFalse(WCAUtils.containsViolation(Arrays.asList(line1Violation, line3Violation), line2Violation));
        assertTrue(WCAUtils.containsViolation(Arrays.asList(line1Violation, line2Violation, line3Violation), line2Violation));
        
    }

}
