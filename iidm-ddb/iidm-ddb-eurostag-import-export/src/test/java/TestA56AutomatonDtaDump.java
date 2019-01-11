/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import eu.itesla_project.iidm.ddb.eurostag_imp_exp.DdExportConfig;
import eu.itesla_project.iidm.ddb.eurostag_imp_exp.DdbDtaImpExp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class TestA56AutomatonDtaDump {

    public static final String XML_COMMON_SECTION = "<beat>2</beat><delay>0.1</delay><voltageDip>0.2</voltageDip><baseVoltage>380.0</baseVoltage><timeConstant>10.0</timeConstant><comment>CC</comment></lossOfSynchronismProtection>";
    FileSystem fileSystem;
    InMemoryPlatformConfig platformConfig;
    MapModuleConfig configExport;
    Network network;

    private DdExportConfig createDdExportConfig(Boolean enabledAutomaton, Path a56DetailsFile) {
        if (enabledAutomaton != null) {
            MapModuleConfig configSection = platformConfig.createModuleConfig(DdExportConfig.MODULE_NAME);
            if (enabledAutomaton) {
                configSection.setStringProperty("automatonA56", String.valueOf(enabledAutomaton));
                configSection.setPathProperty("automatonA56DetailsFile", a56DetailsFile);
            }
        }
        return DdExportConfig.load(platformConfig);
    }


    @Before
    public void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        network = EurostagTutorialExample1Factory.create();
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    private String readFileAsString(Path file) throws IOException {
        return new String(Files.readAllBytes(file)).trim();
    }

    private void writeStringToFile(String data, Path file) throws IOException {
        Files.write(file, data.getBytes());
    }

    private String dumpDataA56AutomatonAsString(Network network, Map<String, String> iidm2eurostagId, DdExportConfig configExport) throws IOException{
        try (ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(); PrintStream out = new PrintStream(outByteStream, true, "UTF-8")) {
            DdbDtaImpExp.dumpDataAutomatonA56(network, out, iidm2eurostagId, configExport);
            String ret = outByteStream.toString().trim();
            return ret;
        }
    }

    private String buildDetailsXml(Map<String, String> iidm2eurostagId, String side) {
        return "<lossOfSynchronismProtections>" + iidm2eurostagId.keySet().stream()
                .map(s -> "<lossOfSynchronismProtection><branch>" + s + "</branch>" + ((side == null) ? "<side/>" : "<side>" + side + "</side>") + XML_COMMON_SECTION)
                .collect(Collectors.joining()) + "</lossOfSynchronismProtections>";
    }

    @Test
    public void testNoExportConfigSection() throws Exception {
        DdExportConfig configExport = createDdExportConfig(null, null);
        assertEquals("", dumpDataA56AutomatonAsString(network, null, configExport));
    }

    @Test
    public void testA56AutomatonDisabled() throws Exception {
        DdExportConfig configExport = createDdExportConfig(false, null);
        assertEquals("", dumpDataA56AutomatonAsString(network, null, configExport));
    }

    @Test
    public void testA56AutomatonDetailsFileDoesNotExist() throws Exception {
        Path detailsFile = fileSystem.getPath("/details_does_not_exist.xml");
        DdExportConfig configExport = createDdExportConfig(true, detailsFile);
        assertEquals("", dumpDataA56AutomatonAsString(network, null, configExport));
    }

    @Test
    public void testA56Automaton00() throws Exception {
        Path detailsFile = fileSystem.getPath("/detailsA56.xml");

        Map<String, String> iidm2eurostagId = new HashMap<>();
        iidm2eurostagId.put("NHV1_NHV2_1", "NHV1____-NHV2____-1");
        iidm2eurostagId.put("NHV2_NLOAD", "NHV2____-NLOAD___-1");

        writeStringToFile(buildDetailsXml(iidm2eurostagId, "1"), detailsFile);

        DdExportConfig configExport = createDdExportConfig(true, detailsFile);
        Path expectedContentPath = Paths.get(TestA56AutomatonDtaDump.class.getResource("/automatonA56_test00.dta").toURI());
        assertEquals(readFileAsString(expectedContentPath), dumpDataA56AutomatonAsString(network, iidm2eurostagId, configExport));
    }


    @Test
    public void testA56Automaton01() throws Exception {
        Path detailsFile = fileSystem.getPath("/detailsA56.xml");

        Map<String, String> iidm2eurostagId = new HashMap<>();
        iidm2eurostagId.put("NHV1_NHV2_1", "NHV1____-NHV2____-1");

        writeStringToFile(buildDetailsXml(iidm2eurostagId, null), detailsFile);

        DdExportConfig configExport = createDdExportConfig(true, detailsFile);
        Path expectedContentPath = Paths.get(TestA56AutomatonDtaDump.class.getResource("/automatonA56_test01.dta").toURI());
        assertEquals(readFileAsString(expectedContentPath), dumpDataA56AutomatonAsString(network, iidm2eurostagId, configExport));
    }

    @Test
    public void testA56Automaton02() throws Exception {
        Path detailsFile = fileSystem.getPath("/detailsA56.xml");

        Map<String, String> iidm2eurostagId = new HashMap<>();
        iidm2eurostagId.put("NHV1_NHV2_1", "NHV1____-NHV2____-1");

        writeStringToFile(buildDetailsXml(iidm2eurostagId, "2"), detailsFile);

        DdExportConfig configExport = createDdExportConfig(true, detailsFile);
        Path expectedContentPath = Paths.get(TestA56AutomatonDtaDump.class.getResource("/automatonA56_test02.dta").toURI());
        assertEquals(readFileAsString(expectedContentPath), dumpDataA56AutomatonAsString(network, iidm2eurostagId, configExport));
    }


}
