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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class DynamicDatabaseMockUtils {

    private static final List<String> REG_EXTENSIONS = Arrays.asList("fri", "frm", "par");

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDatabaseMockUtils.class);

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

    public void copyDynamicDataFiles(String templateName, Path workingDir, String fileName, Map<String, String> mapping, List<String> regulatorFiles) {
        try {
            //change IDs (machines/nodes) in a template according to a mapping and write the result to a file
            copyDtaFile(templateName, workingDir.resolve(fileName), mapping);
            //copy a list of regulators dummy files to a directory
            copyRegulatorsFiles(regulatorFiles, workingDir);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    public String formatString8(String string) {
        return Strings.padEnd((string.length() > 8) ? string.substring(0, 8) : string, 8, ' ');
    }


}
