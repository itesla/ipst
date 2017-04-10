/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import eu.itesla_project.commons.config.InMemoryPlatformConfig;
import eu.itesla_project.commons.config.MapModuleConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class WCARestrictingThresholdLevelConfigTest {

    private FileSystem fileSystem;
    private InMemoryPlatformConfig platformConfig;
    private MapModuleConfig moduleConfig;

    @Before
    public void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        Path xpressPath = fileSystem.getPath("/tmp/xpress");
        moduleConfig = platformConfig.createModuleConfig("wca");
        moduleConfig.setStringProperty("xpressHome", xpressPath.toString());
        moduleConfig.setStringProperty("reducedVariableRatio", "1.5");
        moduleConfig.setStringProperty("debug", "true");
        moduleConfig.setStringProperty("exportStates", "true");
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }


    private void checkConfigValues(WCAConfig config, Path expectedXpressHome, float expectedReducedVariableRatio, boolean expectedDebug, boolean expectedExportStates, Set<WCARestrictingThresholdLevel> expectedRestrictingThresholdLevels) {
        assertEquals(config.getXpressHome(), expectedXpressHome);
        assertEquals(config.getReducedVariableRatio(), expectedReducedVariableRatio, 0.0f);
        assertEquals(config.isDebug(), expectedDebug);
        assertEquals(config.isExportStates(), expectedExportStates);
        assertEquals(config.getRestrictingThresholdLevels(), expectedRestrictingThresholdLevels);
    }

    private void checkConfigValues(WCAConfig config, Set<WCARestrictingThresholdLevel> expectedRestrictingThresholdLevels) {
        checkConfigValues(config, fileSystem.getPath("/tmp/xpress"), 1.5f, true, true, expectedRestrictingThresholdLevels);
    }

    @Test
    public void testConfig() throws Exception {
        moduleConfig.setStringProperty("restrictingThresholdLevels", "NO_HV_THRESHOLDS,NO_FOREIGN_THRESHOLDS");
        WCAConfig config = WCAConfig.load(platformConfig);
        checkConfigValues(config, EnumSet.of(WCARestrictingThresholdLevel.NO_HV_THRESHOLDS, WCARestrictingThresholdLevel.NO_FOREIGN_THRESHOLDS));
    }

    @Test
    public void testConfigDefaultsNotDeclaredThresholds() throws Exception {
        //no restrictingThresholdLevels parameter
        WCAConfig config = WCAConfig.load(platformConfig);
        checkConfigValues(config, EnumSet.noneOf(WCARestrictingThresholdLevel.class));
    }

    @Test
    public void testConfigEmptyThresholds() throws Exception {
        moduleConfig.setStringProperty("restrictingThresholdLevels", "");
        WCAConfig config = WCAConfig.load(platformConfig);
        checkConfigValues(config, EnumSet.noneOf(WCARestrictingThresholdLevel.class));
    }
}
