/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.import_.cim1;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import eu.itesla_project.iidm.datasource.DataSource;
import eu.itesla_project.iidm.datasource.FileDataSource;
import eu.itesla_project.iidm.datasource.ReadOnlyDataSource;
import eu.itesla_project.iidm.datasource.ZipFileDataSource;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * @author Mathieu Bague <mathieu.bague at rte-france.com>
 */
public class CIM1ImporterTest {

    private FileSystem fileSystem;

    private DataSource zdsMerged;
    private DataSource zdsSplit;
    private DataSource fdsMerged;
    private DataSource fdsUnzippedMerged;
    private DataSource fdsSplit;
    private DataSource fdsUnzippedSplit;

    private void copyFile(DataSource dataSource, String filename) throws IOException {
        try (OutputStream stream = dataSource.newOutputStream(filename, false)) {
            IOUtils.copy(getClass().getResourceAsStream("/" + filename), stream);
        }
    }

    @Before
    public void setUp() throws IOException {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());

        Path test1 = Files.createDirectory(fileSystem.getPath("test1"));
        fdsMerged = new FileDataSource(test1, "ieee14bus");
        fdsUnzippedMerged = new FileDataSource(test1, "ieee14bus_ME");
        copyFile(fdsMerged, "ieee14bus_ME.xml");
        copyFile(fdsMerged, "ENTSO-E_Boundary_Set_EU_EQ.xml");
        copyFile(fdsMerged, "ENTSO-E_Boundary_Set_EU_TP.xml");

        Path test2 = Files.createDirectory(fileSystem.getPath("test2"));
        zdsMerged = new ZipFileDataSource(test2, "ieee14bus");
        copyFile(zdsMerged, "ieee14bus_ME.xml");
        copyFile(fdsMerged, "ENTSO-E_Boundary_Set_EU_EQ.xml");
        copyFile(fdsMerged, "ENTSO-E_Boundary_Set_EU_TP.xml");

        Path test3 = Files.createDirectory(fileSystem.getPath("test3"));
        fdsSplit = new FileDataSource(test3, "ieee14bus");
        fdsUnzippedSplit = new FileDataSource(test3, "ieee14bus_EQ");
        copyFile(fdsSplit, "ieee14bus_EQ.xml");
        copyFile(fdsSplit, "ieee14bus_TP.xml");
        copyFile(fdsSplit, "ieee14bus_SV.xml");
        copyFile(fdsSplit, "ENTSO-E_Boundary_Set_EU_EQ.xml");
        copyFile(fdsSplit, "ENTSO-E_Boundary_Set_EU_TP.xml");

        Path test4 = Files.createDirectory(fileSystem.getPath("test4"));
        zdsSplit = new ZipFileDataSource(test4, "ieee14bus");
        copyFile(zdsSplit, "ieee14bus_EQ.xml");
        copyFile(zdsSplit, "ieee14bus_TP.xml");
        copyFile(zdsSplit, "ieee14bus_SV.xml");
        copyFile(zdsSplit, "ENTSO-E_Boundary_Set_EU_EQ.xml");
        copyFile(zdsSplit, "ENTSO-E_Boundary_Set_EU_TP.xml");
    }

    @After
    public void tearDown() throws IOException {
        fileSystem.close();
    }

    @Test
    public void exists() {
        CIM1Importer importer = new CIM1Importer();
        Assert.assertEquals(true, importer.exists(fdsMerged));
        Assert.assertEquals(true, importer.exists(fdsUnzippedMerged));
        Assert.assertEquals(true, importer.exists(zdsMerged));
        Assert.assertEquals(true, importer.exists(fdsSplit));
        Assert.assertEquals(true, importer.exists(fdsUnzippedSplit));
        Assert.assertEquals(true, importer.exists(zdsSplit));
    }

    @Test
    public void testImport() {
        testImport(fdsMerged);
        testImport(fdsUnzippedMerged);
        testImport(fdsSplit);
        testImport(fdsUnzippedSplit);
    }

    private void testImport(ReadOnlyDataSource dataSource) {
        CIM1Importer importer = new CIM1Importer();
        try {
            importer.import_(dataSource, new Properties());
            Assert.fail();
        } catch (RuntimeException ignored) {
        }
    }
}
