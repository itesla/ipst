/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb.domain;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.powsybl.commons.io.table.Column;
import com.powsybl.commons.io.table.CsvTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatter;
import com.powsybl.commons.io.table.TableFormatterConfig;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class DataSet {
    private final List<Attribute> headers = new ArrayList();
    private final List<Record> records = new ArrayList();

    public DataSet() {
    }

    public int size() {
        return records.size();
    }

    public void add(Record results) {
        Objects.requireNonNull(results);
        records.add(results);
    }

    public void addAll(List<Record> results) {
        Objects.requireNonNull(results);
        records.addAll(results);
    }

    public void addHeaders(List<Attribute> columns) {
        Objects.requireNonNull(columns);
        headers.addAll(columns);
    }

    public void writeCsv(Writer writer) throws IOException {
        Objects.requireNonNull(writer);
        writeCsv(writer, Locale.getDefault(), ';', true);
    }

    public void writeCsv(Writer writer, Locale locale, char separator, boolean writeHeaders) throws IOException {
        Objects.requireNonNull(writer);
        TableFormatterConfig formatterConfig = new TableFormatterConfig(locale, separator, "", writeHeaders, false);
        CsvTableFormatterFactory csvTableFormatterFactory = new CsvTableFormatterFactory();
        Column[] cols = headers.stream().map(h -> new Column(h.getName())).toArray(sz -> new Column[sz]);
        try (TableFormatter formatter = csvTableFormatterFactory.create(writer, "", formatterConfig, cols)) {
            records.forEach(r -> {
                r.getValues().forEach(a -> {
                    try {
                        if ("".equals(a.toString())) {
                            formatter.writeEmptyCell();
                        } else {
                            formatter.writeCell(a.toString());
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            });
            writer.flush();
        }
    }

    public List<Attribute> getHeaders() {
        return headers;
    }

    public List<Record> getRecords() {
        return records;
    }

}
