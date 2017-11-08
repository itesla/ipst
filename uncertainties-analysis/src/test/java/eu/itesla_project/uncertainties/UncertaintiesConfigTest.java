/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.uncertainties;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.iidm.network.Country;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.util.Arrays;
import java.util.EnumSet;

import static org.junit.Assert.*;

public class UncertaintiesConfigTest {

    private FileSystem fileSystem;
    private InMemoryPlatformConfig inMemoryPlatformConfig;

    @Before
    public void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        inMemoryPlatformConfig = new InMemoryPlatformConfig(fileSystem);
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    public void testConfig() throws Exception {
        MapModuleConfig moduleConfig = inMemoryPlatformConfig.createModuleConfig("uncertainties-analysis");
        moduleConfig.setStringProperty("onlyIntermittentGeneration", "true");
        moduleConfig.setStringProperty("prctRisk", "0.9");
        moduleConfig.setStringProperty("withBoundaries", "true");
        moduleConfig.setStringListProperty("boundariesFilter", Arrays.asList(Country.FR.name(), Country.DE.name()));
        moduleConfig.setStringProperty("debug", "true");

        UncertaintiesAnalysisConfig config = UncertaintiesAnalysisConfig.load(inMemoryPlatformConfig);
        assertTrue(config.isOnlyIntermittentGeneration());
        assertEquals(0.9f, config.getPrctRisk(), 0.0f);
        assertTrue(config.isWithBoundaries());
        assertEquals(EnumSet.of(Country.FR, Country.DE), config.getBoundariesFilter());
        assertTrue(config.isDebug());
    }

    @Test
    public void testDefaultConfig() {
        UncertaintiesAnalysisConfig defaultConfig = UncertaintiesAnalysisConfig.load(inMemoryPlatformConfig);
        assertFalse(defaultConfig.isOnlyIntermittentGeneration());
        assertEquals(0.85f, defaultConfig.getPrctRisk(), 0.0f);
        assertFalse(defaultConfig.isWithBoundaries());
        assertNull(defaultConfig.getBoundariesFilter());
        assertFalse(defaultConfig.isDebug());
        assertFalse(defaultConfig.useMonthlyCache());
    }
}
