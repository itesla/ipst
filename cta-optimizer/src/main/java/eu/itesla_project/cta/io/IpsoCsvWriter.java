/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.io;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import eu.itesla_project.cta.model.IpsoWritable;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.List;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoCsvWriter implements IpsoWriter {

    @Override
    public void write(List<? extends IpsoWritable> writables, Path toPath) throws IOException {
        Preconditions.checkArgument(writables != null, "writables cannot be null");
        Preconditions.checkArgument(toPath != null, "toPath cannot be null");

        try (Writer writer = new FileWriter(toPath.toFile());
             ICsvListWriter csvWriter = new CsvListWriter(writer, CsvPreference.EXCEL_PREFERENCE)) {
            writeHeader(writables, csvWriter);
            writeLines(writables, csvWriter);
        }
    }

    private void writeLines(List<? extends IpsoWritable> writables, ICsvListWriter csvWriter) throws IOException {
        for (IpsoWritable writable : writables) {
            csvWriter.write(writable.getOrderedValues());
        }
    }

    private void writeHeader(List<? extends IpsoWritable> writables, ICsvListWriter csvwriter) throws IOException {
        IpsoWritable firstOne = Iterables.getFirst(writables, null);
        if (firstOne != null) {
            csvwriter.write(firstOne.getOrderedHeaders());
        }
    }

    @Override
    public IpsoOutputFormat getFormat() {
        return IpsoOutputFormat.CSV;
    }
}
