/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.eurostag.export;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class DicoEurostagNamingStrategy implements EurostagNamingStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(DicoEurostagNamingStrategy.class);

    private BiMap<String, String> dicoMap = HashBiMap.create();

    private CutEurostagNamingStrategy defaultStrategy = new CutEurostagNamingStrategy();

    class DicoCsvReader {

        private static final String SEPARATOR = ";";

        private final Reader source;

        DicoCsvReader(Reader source) {
            this.source = source;
        }

        List<List<String>> readDicoMappings() throws IOException {
            try (BufferedReader reader = new BufferedReader(source)) {
                return reader.lines()
                        .skip(1)
                        .map(line -> Arrays.asList(line.split(SEPARATOR)))
                        .collect(Collectors.toList());
            }
        }
    }

    public DicoEurostagNamingStrategy(Path dicoFile) {
        if ((dicoFile == null) || (!Files.isRegularFile(dicoFile))) {
            String errMsg = "csv file does not exist or is not valid: " + dicoFile;
            LOGGER.error(errMsg);
            throw new RuntimeException(errMsg);
        } else {
            LOGGER.debug("reading iidm-esgid mapping from csv file " + dicoFile);
            // Note: csv files's first line is skipped, it is expected to be a header line
            List<List<String>> dicoMappings;
            try {
                Reader reader = Files.newBufferedReader(dicoFile, Charset.forName("UTF-8"));
                DicoCsvReader dicoReader = new DicoCsvReader(reader);
                dicoMappings = dicoReader.readDicoMappings();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }

            int count = 1;
            for (List<String> row : dicoMappings) {
                count++;
                String iidmId = row.get(0).trim();
                String esgId = row.get(1).trim();
                if (esgId.length() > NameType.GENERATOR.getLength()) {
                    String errMsg = "esgId: " + esgId + " 's length > " + NameType.GENERATOR.getLength() + ".  Line " + count + " in " + dicoFile.toString();
                    throw new RuntimeException(errMsg);
                }
                if ("".equals(iidmId) || "".equals(esgId)) {
                    String errMsg = "either iidmId or esgId or both are empty strings. Line " + count + " in " + dicoFile.toString();
                    LOGGER.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
                if (dicoMap.containsKey(esgId)) {
                    String errMsg = "esgId: " + esgId + " already mapped.";
                    LOGGER.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
                dicoMap.put(iidmId, esgId);
            }
        }
    }

    @Override
    public void fillDictionary(EurostagDictionary dictionary, NameType nameType, Set<String> iidmIds) {
        iidmIds.forEach(iidmId -> {
            if (!dictionary.iidmIdExists(iidmId)) {
                String esgId;
                if (dicoMap.containsKey(iidmId)) {
                    esgId = dicoMap.get(iidmId);
                } else {
                    esgId = defaultStrategy.getEsgId(dictionary, nameType, iidmId);
                    LOGGER.warn(" dico mapping not found for iidmId: '{}'; esgId: '{}' generated using CutName strategy", iidmId, esgId);
                }
                dictionary.add(iidmId, esgId);
                LOGGER.debug("iidmId: '{}' ; esgId: '{}'", iidmId, esgId);
            }
        });
    }
}

