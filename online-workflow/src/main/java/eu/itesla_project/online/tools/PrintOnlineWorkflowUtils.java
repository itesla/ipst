/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.tools;

import eu.itesla_project.commons.io.SystemOutStreamWriter;
import eu.itesla_project.commons.io.table.*;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author c.biasuzzi@techrain.it
 */
public class PrintOnlineWorkflowUtils {

    public static TableFormatter createFormatter(TableFormatterConfig config, Path outputFilePath, String tableTitle, Column... columns) throws IOException {
        Writer writer;
        TableFormatterFactory formatterFactory;
        if (outputFilePath != null) {
            formatterFactory = new CsvTableFormatterFactory();
            writer = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8);
        } else {
            formatterFactory = new AsciiTableFormatterFactory();
            writer = new SystemOutStreamWriter();
        }
        return formatterFactory.create(writer, tableTitle, config, columns);
    }
}


