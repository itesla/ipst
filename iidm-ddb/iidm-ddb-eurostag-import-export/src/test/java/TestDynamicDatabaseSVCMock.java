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
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.SvcTestCaseFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class TestDynamicDatabaseSVCMock {

    private FileSystem fileSystem;

    private static final String DTA_FILENAME = "sim.dta";
    private static final List<String> SVC_REGULATORS_FILENAMES = Arrays.asList("dummefd.fri",
            "dummefd.frm",
            "dummefd.par",
            "dummycm.fri",
            "dummycm.frm",
            "dummycm.par",
            "interdum.frm",
            "interdum.par",
            "interdum.fri");


    @Before
    public void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    public void test_SvcTestCase() throws Exception {
        Network network = SvcTestCaseFactory.create();

        Map<String, String> iidm2eurostag = new HashMap<>();
        iidm2eurostag.put("G1", "G1");
        iidm2eurostag.put("B1", "B1");
        iidm2eurostag.put("SVC2", "SVC2");
        iidm2eurostag.put("B2", "B2");

        Path workingDir = Files.createDirectory(fileSystem.getPath("/workingdir"));

        DynamicDatabaseClient ddbClient = new IIDMDynamicDatabaseSVCMockFactory().create(false);
        ddbClient.dumpDtaFile(workingDir, DTA_FILENAME, network, new HashMap<String, Character>(), "mock", iidm2eurostag, null);

        File expectedDtaFile = new File(getClass().getResource("/sim_test_svc.dta").toURI());
        Path testFile = workingDir.resolve(DTA_FILENAME);
        assertEquals(CharStreams.toString(new InputStreamReader(Files.newInputStream(expectedDtaFile.toPath()))),
                CharStreams.toString(new InputStreamReader(Files.newInputStream(testFile))));

        SVC_REGULATORS_FILENAMES.stream()
                .forEach(filename -> assertTrue(Files.isRegularFile(workingDir.resolve(filename))));

    }


}
