/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class DicoEurostagNamingStrategy implements EurostagNamingStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(DicoEurostagNamingStrategy.class);

    private BiMap<String, String> dicoMap = HashBiMap.create();

    private final CutEurostagNamingStrategy defaultStrategy = new CutEurostagNamingStrategy();

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
                    LOGGER.warn("Skipping mapping iidmId: " + iidmId + ", esgId: " + esgId + ". esgId's length > " + NameType.GENERATOR.getLength() + ".  Line " + count + " in " + dicoFile.toString());
                    continue;
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
        //partition the iidmIds set in two: tiidms with a dico mapping and iidms without a dico mapping
        Map<Boolean, List<String>> dicoPartioned =
                iidmIds.stream().collect(Collectors.partitioningBy(iidmId -> dicoMap.containsKey(iidmId)));

        //first process the entry that are in the dico mapping
        dicoPartioned.get(true).forEach(iidmId -> {
            if (!dictionary.iidmIdExists(iidmId)) {
                String esgId = dicoMap.get(iidmId);
                LOGGER.debug("dico mapping found for iidmId: '{}'; esgId: '{}'", iidmId, esgId);
                dictionary.add(iidmId, esgId);
            }
        });

        //then process the entry that aren't, with the default strategy
        if (dicoPartioned.get(false).size() > 0) {
            LOGGER.warn("dico mapping not found for iidmId ids: {}", dicoPartioned.get(false));
            defaultStrategy.fillDictionary(dictionary, nameType, new HashSet(dicoPartioned.get(false)));
        }
    }
}

