/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.ddb.eurostag_imp_exp;

import com.google.common.collect.ImmutableMap;
import eu.itesla_project.iidm.network.Bus;
import eu.itesla_project.iidm.network.Generator;
import eu.itesla_project.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
class DynamicDatabaseMock implements DynamicDatabaseClient {

    private static final String MINIMAL_DTA_TEMPLATE = "/sim_min.dta";
    private static final List<String> MOCK_REG_FILES_PREFIXES = Arrays.asList("dummefd", "dummycm");

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDatabaseMock.class);

    @Override
    public void dumpDtaFile(Path workingDir, String fileName, Network network, Map<String, Character> parallelIndexes, String eurostagVersion, Map<String, String> iidm2eurostagId) {
        Objects.requireNonNull(workingDir);
        Objects.requireNonNull(fileName);
        Objects.requireNonNull(network);
        Objects.requireNonNull(parallelIndexes);
        Objects.requireNonNull(eurostagVersion);
        Objects.requireNonNull(iidm2eurostagId);

        LOGGER.info("exporting dynamic data for network: {}", network.getId());

        //uses the first connected generator that is available in the iidm2eurostag map
        Generator generator = network.getGeneratorStream()
                .filter(gen -> (iidm2eurostagId.containsKey(gen.getId())) && (gen.getTerminal().isConnected()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("could not find a suitable generator in network: " + network + ", to be used in: " + fileName));

        Bus bus = generator.getTerminal().getBusBreakerView().getConnectableBus();
        if ((bus == null) || (!iidm2eurostagId.containsKey(bus.getId()))) {
            bus = generator.getTerminal().getBusView().getConnectableBus();
            if ((bus == null) || (!iidm2eurostagId.containsKey(bus.getId()))) {
                throw new RuntimeException("suitable node not found");
            }
        }

        DynamicDatabaseMockUtils mockUtils = new DynamicDatabaseMockUtils();
        String mappedGenName = mockUtils.formatString8(iidm2eurostagId.get(generator.getId()));
        String mappedNodeName = mockUtils.formatString8(iidm2eurostagId.get(bus.getId()));

        LOGGER.info("generator:  iidm {}, eurostag {}", generator.getId(), mappedGenName);
        LOGGER.info("node:  iidm {}, eurostag {}", bus.getId(), mappedNodeName);

        mockUtils.copyDynamicDataFiles(MINIMAL_DTA_TEMPLATE, workingDir, fileName,
                ImmutableMap.of("NODENAME", mappedNodeName, "MINIMALI", mappedGenName),
                MOCK_REG_FILES_PREFIXES);
    }

    @Override
    public String getName() {
        return "mock DDB";
    }

    @Override
    public String getVersion() {
        return null;
    }

}
