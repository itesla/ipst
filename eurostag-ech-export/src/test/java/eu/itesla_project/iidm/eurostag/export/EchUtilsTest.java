/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.eurostag.export;

import com.google.common.collect.Lists;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VscConverterStation;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.HvdcTestNetwork;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;

import static org.junit.Assert.*;


/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class EchUtilsTest {


    private final float delta = 0.0f;
    private Network network;
    private Network networkHvdc;

    @Before
    public void setUp() throws Exception {
        network = EurostagTutorialExample1Factory.create();
        networkHvdc = HvdcTestNetwork.createVsc();
    }

    private EurostagEchExportConfig getConfig(boolean noSwitch, boolean exportMainCCOnly) {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);
        MapModuleConfig moduleConfig = platformConfig.createModuleConfig("eurostag-ech-export");
        moduleConfig.setStringProperty("noSwitch", Boolean.toString(noSwitch));
        moduleConfig.setStringProperty("exportMainCCOnly", Boolean.toString(exportMainCCOnly));
        return EurostagEchExportConfig.load(platformConfig);
    }

    private EurostagEchExportConfig getConfig(boolean noSwitch) {
        return getConfig(noSwitch, false);
    }

    @Test
    public void testGetHvdcLineDcVoltage() {
        networkHvdc.getHvdcLineStream().forEach(line -> assertEquals(line.getNominalV() * 2.0f, EchUtil.getHvdcLineDcVoltage(line), delta));
    }

    @Test
    public void testGetHvdcLineDcVoltageNull() {
        try {
            EchUtil.getHvdcLineDcVoltage(null);
            fail("Expected not null parameter");
        } catch (NullPointerException e) {
        }

    }

    @Test
    public void testIsImMainCcBus() {
        Lists.newArrayList(true, false).stream().forEach(exportMainCCOnly -> {
            EchUtil.getBuses(networkHvdc, getConfig(false, exportMainCCOnly)).forEach(bus -> {
                assertEquals(EchUtil.isInMainCc(bus), true);
            });
        });
    }

    @Test
    public void testIsImMainCcGen() {
        Lists.newArrayList(true, false).stream().forEach(exportMainCCOnly -> {
            network.getGeneratorStream().forEach(gen -> {
                assertEquals(EchUtil.isInMainCc(gen, exportMainCCOnly), true);
            });
        });
    }

    @Test
    public void testIsImMainCcLine() {
        Lists.newArrayList(true, false).stream().forEach(exportMainCCOnly -> {
            network.getLines().forEach(line -> {
                assertEquals(EchUtil.isInMainCc(line, exportMainCCOnly), true);
            });
        });
    }

    @Test
    public void testIsPmode() throws IOException {
        HvdcLine hline = networkHvdc.getHvdcLine("L");
        assertTrue(EchUtil.isPMode(networkHvdc.getVscConverterStation("C2"), hline));
        assertFalse(EchUtil.isPMode(networkHvdc.getVscConverterStation("C1"), hline));
    }


}