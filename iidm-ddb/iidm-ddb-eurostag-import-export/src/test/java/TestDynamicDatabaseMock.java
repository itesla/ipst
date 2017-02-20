/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import com.google.common.io.CharStreams;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import eu.itesla_project.iidm.ddb.eurostag_imp_exp.DynamicDatabaseClient;
import eu.itesla_project.iidm.ddb.eurostag_imp_exp.IIDMDynamicDatabaseMockFactory;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class TestDynamicDatabaseMock {

    private FileSystem fileSystem;

    private static String DTA_FILENAME = "sim.dta";

    @Before
    public void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    public void test_00() throws Exception {
        Network network = EurostagTutorialExample1Factory.create();

        Map<String, String> iidm2eurostag = new HashMap<>();
        iidm2eurostag.put("GEN", "GEN");
        iidm2eurostag.put("NGEN", "NGEN");

        Path workingDir = Files.createDirectory(fileSystem.getPath("/workingdir"));

        DynamicDatabaseClient ddbClient = new IIDMDynamicDatabaseMockFactory().create(false);
        ddbClient.dumpDtaFile(workingDir, DTA_FILENAME, network, new HashMap<String, Character>(), "mock", iidm2eurostag);

        File expectedDtaFile = new File(getClass().getResource("/sim_test00.dta").toURI());
        Path testFile = workingDir.resolve(DTA_FILENAME);

        assertEquals(CharStreams.toString(new InputStreamReader(Files.newInputStream(expectedDtaFile.toPath()))),
                CharStreams.toString(new InputStreamReader(Files.newInputStream(testFile))));

        //TODO test dummy regulators files
    }

}
