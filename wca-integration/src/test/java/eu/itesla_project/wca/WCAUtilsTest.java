/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.CharStreams;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.test.NetworkTest1Factory;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCAUtilsTest {

    private FileSystem fileSystem;

    @Before
    public void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    public void testExportState() throws IOException, URISyntaxException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));
        Network network = NetworkTest1Factory.create();
        network.setCaseDate(new DateTime(1483228800000l).withZone(DateTimeZone.UTC));
        WCAUtils.exportState(network, folder, 0, 0);
        Path exportedState = folder.resolve(network.getId() + "_0_0.xiidm.gz");
        assertTrue(Files.exists(exportedState));
        File expectedState = new File(getClass().getResource("/network1.xiidm").toURI());
        assertEquals(CharStreams.toString(new InputStreamReader(new FileInputStream(expectedState))),
                CharStreams.toString(new InputStreamReader(new GZIPInputStream(Files.newInputStream(exportedState)))));
    }

}
