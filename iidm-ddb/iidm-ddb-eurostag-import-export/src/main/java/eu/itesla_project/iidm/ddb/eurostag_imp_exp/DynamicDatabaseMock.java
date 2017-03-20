/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.ddb.eurostag_imp_exp;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import eu.itesla_project.iidm.network.Bus;
import eu.itesla_project.iidm.network.Generator;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.StaticVarCompensator;
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
    private static final String MINIMAL_DTA_SVC_TEMPLATE = "/sim_min_svc.dta";
    private static final List<String> MOCK_REG_FILES_PREFIXES = Arrays.asList("dummefd", "dummycm");
    private static final List<String> MOCK_REG_FILES_SVC_PREFIXES = Arrays.asList("dummefd", "dummycm", "interdum");
    private static final List<String> REG_EXTENSIONS = Arrays.asList("fri", "frm", "par", "pcp", "rcp");

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDatabaseMock.class);

    private void copyRegulatorsFiles(List<String> regulatorsNames, Path workingDir) {
        regulatorsNames.stream()
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
    }

    private void copyDtaFile(String templateName, Path destFile, Map<String, String> mapping) throws IOException {
        try (final Reader reader = new InputStreamReader(getClass().getResourceAsStream(templateName))) {
            String dtaContents = CharStreams.toString(reader);
            dtaContents = mapping.keySet().stream()
                    .reduce(dtaContents, (str, key) -> str.replaceAll(key, mapping.get(key)));
            try (BufferedWriter writer = Files.newBufferedWriter(destFile)) {
                writer.write(dtaContents);
            }
        }
    }

    private void copyDynamicFiles(String templateName, Path workingDir, String fileName, Map<String, String> mapping, List<String> regulatorFiles) {
        try {
            //change IDs (machines/nodes) in a template according to a mapping and write the result to a file
            copyDtaFile(templateName, workingDir.resolve(fileName), mapping);
            //copy a list of regulators dummy files to a directory
            copyRegulatorsFiles(regulatorFiles, workingDir);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


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
                .filter(gen -> ((iidm2eurostagId.containsKey(gen.getId())) && (gen.getTerminal().isConnected())))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("could not find a suitable generator in network: " + network + ", to be used in: " + fileName));

        Bus bus = generator.getTerminal().getBusBreakerView().getConnectableBus();
        if ((bus == null) || (!iidm2eurostagId.containsKey(bus.getId()))) {
            throw new RuntimeException("suitable node not found");
        }
        String mappedGenName = formatString8(iidm2eurostagId.get(generator.getId()));
        String mappedNodeName = formatString8(iidm2eurostagId.get(bus.getId()));

        LOGGER.info("generator:  iidm {}, eurostag {}", generator.getId(), mappedGenName);
        LOGGER.info("node:  iidm {}, eurostag {}", generator.getTerminal().getBusBreakerView().getConnectableBus().getId(), mappedNodeName);

        //Mock DB, driven by network ID
        switch (network.getId()) {

            // the test SVC network
            case "svcTestCase":
                //uses the first connected SVC that is available in the iidm2eurostag map
                StaticVarCompensator svc1 = network.getStaticVarCompensatorStream()
                        .filter(svc -> ((iidm2eurostagId.containsKey(svc.getId())) && (svc.getTerminal().isConnected())))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("could not find a suitable svc in network: " + network + ", to be used in: "));
                String mappedSvcName = iidm2eurostagId.get(svc1.getId());
                LOGGER.info("svc:  iidm {}, eurostag {}", svc1.getId(), mappedSvcName);

                copyDynamicFiles(MINIMAL_DTA_SVC_TEMPLATE, workingDir, fileName,
                        ImmutableMap.of("NODENAME", mappedNodeName, "MINIMALI", mappedGenName, "SVCDUMMY", mappedSvcName),
                        MOCK_REG_FILES_SVC_PREFIXES);

                break;


            default:
                copyDynamicFiles(MINIMAL_DTA_TEMPLATE, workingDir, fileName,
                        ImmutableMap.of("NODENAME", mappedNodeName, "MINIMALI", mappedGenName),
                        MOCK_REG_FILES_PREFIXES);
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
