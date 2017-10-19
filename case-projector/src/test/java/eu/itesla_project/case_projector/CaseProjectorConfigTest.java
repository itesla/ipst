/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.case_projector;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class CaseProjectorConfigTest {

    private FileSystem fileSystem;
    private InMemoryPlatformConfig platformConfig;
    private MapModuleConfig moduleConfig;
    private Path amplHome;
    private boolean debug;

    @Before
    public void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        moduleConfig = platformConfig.createModuleConfig("caseProjector");
        amplHome = fileSystem.getPath("/ampl-home");
        debug = true;
        moduleConfig.setStringProperty("amplHomeDir", amplHome.toString());
        moduleConfig.setStringProperty("debug", Boolean.toString(debug));
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    public void testBasicConfig() throws Exception {
        CaseProjectorConfig config = CaseProjectorConfig.load(platformConfig);
        checkValues(config, amplHome, debug);
    }

    private void checkValues(CaseProjectorConfig config, Path amplHome, boolean debug) {
        assertEquals(amplHome, config.getAmplHomeDir());
        assertEquals(debug, config.isDebug());
    }


}