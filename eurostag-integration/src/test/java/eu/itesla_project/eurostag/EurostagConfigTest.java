/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.eurostag;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import eu.itesla_project.commons.config.InMemoryPlatformConfig;
import eu.itesla_project.commons.config.MapModuleConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class EurostagConfigTest {

    private FileSystem fileSystem;
    private InMemoryPlatformConfig platformConfig;
    private MapModuleConfig moduleConfig;

    @Before
    public void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        moduleConfig = platformConfig.createModuleConfig("eurostag");
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    public void testEmptyEurostagSectionConfig() throws Exception {
        EurostagConfig config = EurostagConfig.load(platformConfig);
        assertEquals(config.getEurostagHomeDir(), null);
        assertNotNull(config.getEurostagCptCommandName());
    }

    @Test
    public void testEurostagHomeConfig() throws Exception {
        Path eurostagHome = fileSystem.getPath("/eurostag_Linux_v51_iTesla");
        moduleConfig.setPathProperty("eurostagHomeDir", eurostagHome);
        EurostagConfig config = EurostagConfig.load(platformConfig);
        assertEquals(eurostagHome, config.getEurostagHomeDir());
    }

    @Test
    public void testEurostagCommandConfig() throws Exception {
        String eurostagCmndName = "ulimit -s unlimited && eustag_cpt_noGUI.e";
        moduleConfig.setStringProperty("eurostagCptCommandName", eurostagCmndName);
        EurostagConfig config = EurostagConfig.load(platformConfig);
        System.out.println(config);
        assertEquals(eurostagCmndName, config.getEurostagCptCommandName());
    }
}