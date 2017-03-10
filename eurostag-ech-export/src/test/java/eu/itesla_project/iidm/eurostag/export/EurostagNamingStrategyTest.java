/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.eurostag.export;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import eu.itesla_project.commons.config.InMemoryPlatformConfig;
import eu.itesla_project.commons.config.MapModuleConfig;
import eu.itesla_project.iidm.network.Generator;
import eu.itesla_project.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class EurostagNamingStrategyTest {


    private Network network;

    private Network createMockNetwork(List<String> genNames) {
        Objects.requireNonNull(genNames);

        Network net = Mockito.mock(Network.class);
        Network.BusView busView = Mockito.mock(Network.BusView.class);
        Network.BusBreakerView busBreakerView = Mockito.mock(Network.BusBreakerView.class);
        Mockito.when(busView.getBuses())
                .thenReturn(Collections.emptyList());
        Mockito.when(net.getBusBreakerView())
                .thenReturn(busBreakerView);
        Mockito.when(busBreakerView.getBuses())
                .thenReturn(Collections.emptyList());
        Mockito.when(net.getDanglingLines())
                .thenReturn(Collections.emptyList());
        Mockito.when(net.getLoads())
                .thenReturn(Collections.emptyList());
        Mockito.when(net.getShunts())
                .thenReturn(Collections.emptyList());
        Mockito.when(net.getStaticVarCompensators())
                .thenReturn(Collections.emptyList());
        Mockito.when(net.getVoltageLevels())
                .thenReturn(Collections.emptyList());
        Mockito.when(net.getLines())
                .thenReturn(Collections.emptyList());
        Mockito.when(net.getTwoWindingsTransformers())
                .thenReturn(Collections.emptyList());
        Mockito.when(net.getThreeWindingsTransformers())
                .thenReturn(Collections.emptyList());

        List<Generator> genList = genNames.stream().map(x -> {
            Generator g = Mockito.mock(Generator.class);
            Mockito.when(g.getId())
                    .thenReturn(x);
            return g;
        }).collect(Collectors.toList());
        Mockito.when(net.getGenerators())
                .thenReturn(genList);
        return net;
    }


    private boolean isValid(EurostagEchExportConfig config, String string) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(string);
        String forbiddenCharacters = config.getForbiddenCharactersString();
        return (forbiddenCharacters.length() == 0) || !string.matches(".*[" + forbiddenCharacters + "].*");
    }


    @Before
    public void setUp() throws Exception {
        List<String> genNames = Arrays.asList("GEN1;", "GEN2;", "GEN3;", "GENOK[", "GE,NA^C;EN", "GE,NA^C;EN2", "GEN1_", "GEN^^^^^", "GEN_____", "GEN____0", "GEN(((((");
        network = createMockNetwork(genNames);
    }

    private EurostagEchExportConfig getConfig(String forbiddenChars, String forbiddenCharactersReplacement) {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);
        MapModuleConfig moduleConfig = platformConfig.createModuleConfig(EurostagEchExportConfig.EUROSTAG_ECH_EXPORT_CONFIG);
        moduleConfig.setStringProperty("noGeneratorMinMaxQ", "false");
        moduleConfig.setStringProperty("noSwitch", "false");
        moduleConfig.setStringProperty("forbiddenCharacters", forbiddenChars);
        moduleConfig.setStringProperty("forbiddenCharactersReplacement", forbiddenCharactersReplacement);

        return EurostagEchExportConfig.load(platformConfig);
    }

    @Test
    public void testForbiddenChars() throws IOException {
        EurostagEchExportConfig config = getConfig(";^", "_");
        EurostagDictionary ed = EurostagDictionary.create(network, null, config);
        ed.toMap().forEach((iidmId, esgId) -> assertTrue(isValid(config, esgId)));
    }

    @Test
    public void testForbiddenCharsInvalidConfig() throws IOException {
        try {
            EurostagDictionary ed = EurostagDictionary.create(network, null, getConfig(";^", ";"));
            throw new RuntimeException("should not be this statement");
        } catch (IllegalArgumentException err) {
        }
    }


    @Test
    public void testEmptyForbiddenChars() throws IOException {
        // assumption:  empty forbidden characters string means NO replacement
        EurostagEchExportConfig config = getConfig("", "_");
        EurostagDictionary ed = EurostagDictionary.create(network, null,
                config);
        ed.toMap().keySet().stream().forEach(iidmId -> assertEquals(isValid(config, iidmId), isValid(config, ed.getEsgId(iidmId))));
    }

    @Test
    public void testDefaultCharsConfig() throws IOException {
        EurostagEchExportConfig config = EurostagEchExportConfig.load();
        EurostagDictionary ed = EurostagDictionary.create(network, null, config);
        ed.toMap().forEach((iidmId, esgId) -> {
            assertTrue(isValid(config, esgId));
        });
    }

}