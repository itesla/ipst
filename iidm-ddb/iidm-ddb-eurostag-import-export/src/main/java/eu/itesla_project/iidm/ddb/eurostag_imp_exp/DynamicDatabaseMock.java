/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.ddb.eurostag_imp_exp;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import eu.itesla_project.iidm.network.Bus;
import eu.itesla_project.iidm.network.Generator;
import eu.itesla_project.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
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
    private static List<String> MOCK_REG_FILES_PREFIXES = Arrays.asList("dummefd", "dummycm");
    private static List<String> REG_EXTENSIONS = Arrays.asList("fri", "frm", "par", "pcp", "rcp");

    static Logger LOGGER = LoggerFactory.getLogger(DynamicDatabaseMock.class);

    @Override
    public void dumpDtaFile(Path workingDir, String fileName, Network network, Map<String, Character> parallelIndexes, String eurostagVersion, Map<String, String> iidm2eurostagId) {
        Objects.requireNonNull(workingDir);
        Objects.requireNonNull(fileName);
        Objects.requireNonNull(network);
        Objects.requireNonNull(parallelIndexes);
        Objects.requireNonNull(eurostagVersion);
        Objects.requireNonNull(iidm2eurostagId);

        //uses the first connected generator that is available in the iidm2eurostag map
        Generator generator = network.getGeneratorStream().filter(gen -> ((iidm2eurostagId.containsKey(gen.getId())) && (gen.getTerminal().isConnected()))).findFirst().get();
        if (generator == null) {
            throw new RuntimeException("could not find a suitable generator to use in " + fileName);
        }

        Bus bus = generator.getTerminal().getBusBreakerView().getConnectableBus();
        if ((bus == null) || (!iidm2eurostagId.containsKey(bus.getId()))) {
            throw new RuntimeException("suitable node not found");
        }
        String mappedGenName = formatString8(iidm2eurostagId.get(generator.getId()));
        String mappedNodeName = formatString8(iidm2eurostagId.get(bus.getId()));

        LOGGER.info("generator:  iidm {}, eurostag {}", generator.getId(), mappedGenName);
        LOGGER.info("node:  iidm {}, eurostag {}", generator.getTerminal().getBusBreakerView().getConnectableBus().getId(), mappedNodeName);

        try {
            try (final Reader reader = new InputStreamReader(getClass().getResourceAsStream(MINIMAL_DTA_TEMPLATE))) {
                String dtaContents = CharStreams.toString(reader);

                //change the connection node name, according to the current network and the iidm2eurostag mapping
                String newDtaContents = dtaContents
                        .replace("NODENAME", mappedNodeName)
                        .replace("MINIMALI", mappedGenName);
                try (BufferedWriter writer = java.nio.file.Files.newBufferedWriter(workingDir.resolve(fileName))) {
                    writer.write(newDtaContents);
                }
            }
            //copy the regulators dummy files
            MOCK_REG_FILES_PREFIXES.stream()
                    .flatMap(filePrefix -> REG_EXTENSIONS.stream().map(fileSuffix -> filePrefix + "." + fileSuffix))
                    .forEach(filename -> {
                        try (final InputStream reader = getClass().getResourceAsStream("/" + filename)) {
                            LOGGER.info("copying regulator file: {}", filename);
                            Files.write(workingDir.resolve(filename), ByteStreams.toByteArray(reader));
                        } catch (IOException e) {
                            LOGGER.error(e.getMessage(), e);
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String formatString8(String string) {
        return Strings.padEnd((string.length() > 8) ? string.substring(0, 8) : string, 8, ' ');
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
