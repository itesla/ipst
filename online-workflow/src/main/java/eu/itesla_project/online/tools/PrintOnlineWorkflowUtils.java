/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.tools;

import com.powsybl.commons.io.table.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author c.biasuzzi@techrain.it
 */
public class PrintOnlineWorkflowUtils {

    enum TableFormatterFactorySupplier {

        ascii(AsciiTableFormatterFactory::new),
        csv(CsvTableFormatterFactory::new);

        public final Supplier<TableFormatterFactory> supplier;

        private TableFormatterFactorySupplier(Supplier<TableFormatterFactory> supplier) {
            this.supplier = Objects.requireNonNull(supplier, "null supplier for name " + name());
        }
    }

    private PrintOnlineWorkflowUtils() {
    }

    public static String availableTableFormatterFormats() {
        return Arrays.stream(TableFormatterFactorySupplier.values()).map(x -> x.name()).collect(Collectors.joining(", "));
    }

    public static boolean isTableFactoryAvailable(String formatName) {
        return Arrays.stream(TableFormatterFactorySupplier.values()).anyMatch((t) -> t.name().equals(formatName));
    }

    public static TableFormatterFactory tableFactoryByFormatName(String formatName) {
        return TableFormatterFactorySupplier.valueOf(formatName).supplier.get();
    }

    public static TableFormatter createFormatter(TableFormatterConfig config, String outputFormatName, Path outputFilePath, String tableTitle, Column... columns) throws IOException {
        Writer writer;
        TableFormatterFactory formatterFactory;
        if (isTableFactoryAvailable(outputFormatName)) {
            formatterFactory = tableFactoryByFormatName(outputFormatName);
        } else {
            throw new RuntimeException("output format not supported: " + outputFormatName);
        }
        if (outputFilePath != null) {
            writer = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8);
        } else {
            writer = new OutputStreamWriter(System.out) {
                @Override
                public void close() throws IOException {
                    flush();
                }
            };
        }
        return formatterFactory.create(writer, tableTitle, config, columns);
    }
}
