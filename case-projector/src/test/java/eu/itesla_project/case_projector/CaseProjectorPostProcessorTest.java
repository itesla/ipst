/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.case_projector;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.PlatformConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;

public class CaseProjectorPostProcessorTest {

    private FileSystem fileSystem;
    private PlatformConfig platformConfig;

    private PlatformConfig createEmptyPlatformConfig() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        return platformConfig;
    }

    @Before
    public void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = createEmptyPlatformConfig();
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    public void testInstCaseProjectorPostProcessor() throws Exception {
        //when there is no case-projector section, in the configuration, this constructor should not throw any exception
        CaseProjectorPostProcessor postProcessor = new CaseProjectorPostProcessor();
    }
}
