/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.eurostag.export;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;

import static org.junit.Assert.assertEquals;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class CheckEurostagEchExporterConfigTest {

    InMemoryPlatformConfig platformConfig;
    FileSystem fileSystem;

    @Before
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        MapModuleConfig defaultConfig = platformConfig.createModuleConfig("componentDefaultConfig");
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    private EurostagEchExportConfig getConfigFromFile(FileSystem fileSystem, boolean specificCompatibility, boolean mainCcOnly) {
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);
        MapModuleConfig moduleConfig = platformConfig.createModuleConfig("eurostag-ech-export");
        moduleConfig.setStringProperty("svcAsFixedInjectionInLF", "false");
        moduleConfig.setStringProperty("exportMainCCOnly", Boolean.toString(mainCcOnly));

        moduleConfig = platformConfig.createModuleConfig("load-flow-default-parameters");
        moduleConfig.setStringProperty("specificCompatibility", Boolean.toString(specificCompatibility));

        return EurostagEchExportConfig.load(platformConfig);
    }


    @Test
    public void testConfig() throws IOException {
        EurostagEchExportConfig config = new EurostagEchExportConfig();
        assertEquals(false, config.isSvcAsFixedInjectionInLF());
    }

    @Test
    public void testConfigFromFile() throws IOException {
        EurostagEchExportConfig config = getConfigFromFile(fileSystem, false, false);
        assertEquals(false, config.isSvcAsFixedInjectionInLF());
    }

    @Test
    public void testConfigSpecificCompatibility() throws IOException {
        EurostagEchExportConfig config = getConfigFromFile(fileSystem, true, false);
        assertEquals(true, config.isSvcAsFixedInjectionInLF());
    }

    @Test
    public void testConfigExportNoMainCC() throws IOException {
        EurostagEchExportConfig config = getConfigFromFile(fileSystem, true, false);
        assertEquals(false, config.isExportMainCCOnly());
    }

    @Test
    public void testConfigExportMainCC() throws IOException {
        EurostagEchExportConfig config = getConfigFromFile(fileSystem, true, true);
        assertEquals(true, config.isExportMainCCOnly());
    }

}
