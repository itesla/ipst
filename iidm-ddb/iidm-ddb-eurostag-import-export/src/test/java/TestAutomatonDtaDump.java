/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import com.google.common.io.CharStreams;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.simulation.SimulationParameters;
import eu.itesla_project.iidm.ddb.eurostag_imp_exp.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class TestAutomatonDtaDump {

    FileSystem fileSystem;
    InMemoryPlatformConfig platformConfig;
    MapModuleConfig configExport;
    SimulationParameters simulationParameters;
    Network network;
    Map<String, String> iidm2eurostagId;


    private DdExportConfig createDdExportConfig(Boolean enabledAutomaton, String angularReferenceGenerator, Double minimumPhaseDifferenceThreshold, Double maximumPhaseDifferenceThreshold, Double observationDuration) {
        if (enabledAutomaton != null) {
            MapModuleConfig configSection = platformConfig.createModuleConfig(DdExportConfig.MODULE_NAME);
            if (enabledAutomaton) {
                configSection.setStringProperty("automatonA17", String.valueOf(enabledAutomaton));
            }
            if (angularReferenceGenerator != null) {
                configSection.setStringProperty("automatonA17AngularReferenceGenerator", angularReferenceGenerator);
            }
            if (minimumPhaseDifferenceThreshold != null) {
                configSection.setStringProperty("automatonA17MinimumPhaseDifferenceThreshold", String.valueOf(minimumPhaseDifferenceThreshold));
            }
            if (maximumPhaseDifferenceThreshold != null) {
                configSection.setStringProperty("automatonA17MaximumPhaseDifferenceThreshold", String.valueOf(maximumPhaseDifferenceThreshold));
            }
            if (observationDuration != null) {
                configSection.setStringProperty("automatonA17ObservationDuration", String.valueOf(observationDuration));
            }
        }
        return DdExportConfig.load(platformConfig);
    }


    private SimulationParameters createSimpleParamConfig() throws IOException {
        MapModuleConfig paramConfig = platformConfig.createModuleConfig("simulation-parameters");
        paramConfig.setStringProperty("preFaultSimulationStopInstant", "5");
        paramConfig.setStringProperty("faultEventInstant", "7");
        paramConfig.setStringProperty("postFaultSimulationStopInstant", "9");
        paramConfig.setStringProperty("generatorFaultShortCircuitDuration", "0.3");
        paramConfig.setStringProperty("branchSideOneFaultShortCircuitDuration", "0.8");
        paramConfig.setStringProperty("branchSideTwoFaultShortCircuitDuration", "0.8");

        return SimulationParameters.load(platformConfig);
    }


    @Before
    public void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        simulationParameters = createSimpleParamConfig();

        network = EurostagTutorialExample1Factory.create();
        iidm2eurostagId = network.getGeneratorStream().map(g -> new AbstractMap.SimpleEntry<>(g.getId(), g.getId())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    private String dumpDataAutomatonA17AsString(Network network, Map<String, String> iidm2eurostagId, SimulationParameters simulationParameters, DdExportConfig configExport) throws IOException {
        try (ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(); PrintStream out = new PrintStream(outByteStream, true, "UTF-8")) {

            DdbDtaImpExp.dumpDataAutomatonA17(network, out, iidm2eurostagId, simulationParameters, configExport);

            String ret = outByteStream.toString().trim();
            return ret;
        }
    }

    private String readFileAsString(Path file) throws IOException {
        return new String(Files.readAllBytes(file)).trim();
    }


    @Test
    public void testAutomatonA17Disabled() throws Exception {
        DdExportConfig configExport = createDdExportConfig(false, "GEN", -240.0, 240.0, 15.0);
        assertEquals("", dumpDataAutomatonA17AsString(network, iidm2eurostagId, simulationParameters, configExport));
    }

    @Test
    public void testAutomatonA17AllParameters() throws Exception {
        DdExportConfig configExport = createDdExportConfig(true, "GEN", -255.0, 256.0, 18.0);
        Path expectedContentPath = Paths.get(TestAutomatonDtaDump.class.getResource("/automatonA17_test01.dta").toURI());
        assertEquals(readFileAsString(expectedContentPath), dumpDataAutomatonA17AsString(network, iidm2eurostagId, simulationParameters, configExport));
    }

    @Test
    public void testAutomatonA17NoRefGen() throws Exception {
        DdExportConfig configExport = createDdExportConfig(true, "DOESNOTEXIST", -240.0, 240.0, 15.0);
        assertEquals("", dumpDataAutomatonA17AsString(network, iidm2eurostagId, simulationParameters, configExport));
    }

    @Test
    public void testAutomatonA17NullRefGen() throws Exception {
        DdExportConfig configExport = createDdExportConfig(true, null, -240.0, 240.0, 15.0);
        assertEquals("", dumpDataAutomatonA17AsString(network, iidm2eurostagId, simulationParameters, configExport));
    }

    @Test
    public void testAutomatonA17NoThresholds() throws Exception {
        DdExportConfig configExport = createDdExportConfig(true, "GEN", null, null, 15.0);
        Path expectedContentPath = Paths.get(TestAutomatonDtaDump.class.getResource("/automatonA17_test00.dta").toURI());
        assertEquals(readFileAsString(expectedContentPath), dumpDataAutomatonA17AsString(network, iidm2eurostagId, simulationParameters, configExport));
    }

    @Test
    public void testAutomatonA17NoObservationDuration() throws Exception {
        DdExportConfig configExport = createDdExportConfig(true, "GEN", -255.0, 256.0, null);
        Path expectedContentPath = Paths.get(TestAutomatonDtaDump.class.getResource("/automatonA17_test02.dta").toURI());
        assertEquals(readFileAsString(expectedContentPath), dumpDataAutomatonA17AsString(network, iidm2eurostagId, simulationParameters, configExport));
    }

    @Test
    public void testNoExportConfigSection() throws Exception {
        DdExportConfig configExport = createDdExportConfig(null, null, null, null, null);
        assertEquals("", dumpDataAutomatonA17AsString(network, iidm2eurostagId, simulationParameters, configExport));
    }
}
