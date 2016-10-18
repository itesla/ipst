/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.cases;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Sets;
import eu.itesla_project.iidm.datasource.DataSource;
import eu.itesla_project.iidm.import_.Importer;
import eu.itesla_project.iidm.network.Country;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.ucte.util.UcteGeographicalCode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.nio.file.ShrinkWrapFileSystems;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class EntsoeCaseRepositoryTest {

    private FileSystem fileSystem;
    private Path rootDir;
    private EntsoeCaseRepository caseRepository;
    private Network cimNetwork;
    private Network uctNetwork;

    private class DataSourceMock implements DataSource {
        private final Path directory;
        private final String baseName;

        private DataSourceMock(Path directory, String baseName) {
            this.directory = directory;
            this.baseName = baseName;
        }

        private Path getDirectory() {
            return directory;
        }

        @Override
        public boolean exists(String fileName) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public OutputStream newOutputStream(String suffix, String ext, boolean append) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getBaseName() {
            return baseName;
        }

        @Override
        public boolean exists(String suffix, String ext) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream newInputStream(String suffix, String ext) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream newInputStream(String fileName) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    private static void createFile(Path dir, String fileName) throws IOException {
        try (Writer writer = Files.newBufferedWriter(dir.resolve(fileName))) {
            writer.write("test");
        }
    }

    @Before
    public void setUp() throws Exception {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
        fileSystem = ShrinkWrapFileSystems.newFileSystem(archive);
        rootDir = fileSystem.getPath("/");

        Importer cimImporter = Mockito.mock(Importer.class);
        Mockito.when(cimImporter.exists(Matchers.isA(DataSource.class)))
                .thenAnswer(invocation -> {
                    DataSourceMock dataSource = invocation.getArgumentAt(0, DataSourceMock.class);
                    Path file = dataSource.getDirectory().resolve(dataSource.getBaseName() + ".zip");
                    return Files.isRegularFile(file) && Files.exists(file);
                });
        Mockito.when(cimImporter.getFormat())
                .thenReturn("CIM1");
        cimNetwork = Mockito.mock(Network.class);
        Mockito.when(cimImporter.import_(Matchers.isA(DataSource.class), Matchers.any()))
                .thenReturn(cimNetwork);

        Importer uctImporter = Mockito.mock(Importer.class);
        Mockito.when(uctImporter.exists(Matchers.isA(DataSource.class)))
                .thenAnswer(invocation -> {
                    DataSourceMock dataSource = invocation.getArgumentAt(0, DataSourceMock.class);
                    Path file = dataSource.getDirectory().resolve(dataSource.getBaseName() + ".uct");
                    return Files.isRegularFile(file) && Files.exists(file);
                });
        Mockito.when(uctImporter.getFormat())
                .thenReturn("UCTE");
        uctNetwork = Mockito.mock(Network.class);
        Mockito.when(uctImporter.import_(Matchers.isA(DataSource.class), Matchers.any()))
                .thenReturn(uctNetwork);

        caseRepository = new EntsoeCaseRepository(new EntsoeCaseRepositoryConfig(rootDir, HashMultimap.create()),
                Arrays.asList(new EntsoeCaseRepository.EntsoeFormat(cimImporter, "CIM"),
                        new EntsoeCaseRepository.EntsoeFormat(uctImporter, "UCT")),
                (directory, baseName) -> new DataSourceMock(directory, baseName));
        Path dir1 = fileSystem.getPath("/CIM/SN/2013/01/13");
        Files.createDirectories(dir1);
        createFile(dir1, "20130113_0015_SN7_FR0.zip");
        createFile(dir1, "20130113_0045_SN7_FR0.zip");
        Path dir2 = fileSystem.getPath("/CIM/SN/2013/01/14");
        Files.createDirectories(dir2);
        createFile(dir2, "20130114_0015_SN1_FR0.zip");
        Path dir3 = fileSystem.getPath("/UCT/SN/2013/01/14");
        Files.createDirectories(dir3);
        createFile(dir3, "20130114_0015_SN1_FR0.uct");
        createFile(dir3, "20130114_0030_SN1_FR0.uct");
        Path dir4 = fileSystem.getPath("/UCT/SN/2013/01/15");
        Files.createDirectories(dir4);
        createFile(dir4, "20130115_0015_SN2_D20.uct");
        createFile(dir4, "20130115_0015_SN2_D40.uct");
        createFile(dir4, "20130115_0015_SN2_D70.uct");
        createFile(dir4, "20130115_0015_SN2_D80.uct");

        // D2
        Path dir5 = fileSystem.getPath("/UCT/2D/2013/01/15");
        Files.createDirectories(dir5);
        createFile(dir5, "20130115_0030_2D2_FR0.uct");
        createFile(dir5, "20130115_0130_2D2_FR0.uct");

        // LT
        Path dir6 = fileSystem.getPath("/UCT/LT/2013/01/15");
        Files.createDirectories(dir6);
        createFile(dir6, "20130115_0030_LT2_FR0.uct");
        createFile(dir6, "20130115_0130_LT2_FR0.uct");

        // RE
        Path dir7 = fileSystem.getPath("/UCT/RE/2013/01/15");
        Files.createDirectories(dir7);
        createFile(dir7, "20130115_0030_RE2_FR0.uct");
        createFile(dir7, "20130115_0130_RE2_FR0.uct");

        // INTRADAY
        Path dir8 = fileSystem.getPath("/UCT/IDCF/2013/01/15");
        Files.createDirectories(dir8);
        createFile(dir8, "20130115_0330_012_FR0.uct");
        createFile(dir8, "20130115_0330_022_FR0.uct");
        createFile(dir8, "20130115_0330_032_FR0.uct");

        // daylight saving FO
        Path dir9 = fileSystem.getPath("/UCT/FO/2016/10/30");
        Files.createDirectories(dir9);
        createFile(dir9, "20161030_0230_FO7_FR0.uct");
        createFile(dir9, "20161030_B230_FO7_FR0.uct");
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    public void testLoad() throws Exception {
        assertTrue(caseRepository.load(DateTime.parse("2013-01-13T00:15:00+01:00"), CaseType.SN, Country.FR).size() == 1);
        assertTrue(caseRepository.load(DateTime.parse("2013-01-13T00:30:00+01:00"), CaseType.SN, Country.FR).isEmpty());
        assertTrue(caseRepository.load(DateTime.parse("2013-01-13T00:15:00+01:00"), CaseType.FO, Country.FR).isEmpty());
        assertTrue(caseRepository.load(DateTime.parse("2013-01-13T00:15:00+01:00"), CaseType.SN, Country.BE).isEmpty());

        // check that cim network is loaded instead of uct network
        assertTrue(caseRepository.load(DateTime.parse("2013-01-14T00:15:00+01:00"), CaseType.SN, Country.FR).equals(Collections.singletonList(cimNetwork)));

        // check that if cim is forbidden for france, uct is loaded
        caseRepository.getConfig().getForbiddenFormatsByGeographicalCode().put(UcteGeographicalCode.FR, "CIM1");
        assertTrue(caseRepository.load(DateTime.parse("2013-01-14T00:15:00+01:00"), CaseType.SN, Country.FR).equals(Collections.singletonList(uctNetwork)));

        assertTrue(caseRepository.load(DateTime.parse("2013-01-15T00:15:00+01:00"), CaseType.SN, Country.DE).size() == 4);
    }

    @Test
    public void testIsDataAvailable() throws Exception {
        assertTrue(caseRepository.isDataAvailable(DateTime.parse("2013-01-13T00:15:00+01:00"), CaseType.SN, Country.FR));
        assertFalse(caseRepository.isDataAvailable(DateTime.parse("2013-01-13T00:30:00+01:00"), CaseType.SN, Country.FR));
    }

    @Test
    public void testDataAvailable() throws Exception {
        assertTrue(caseRepository.dataAvailable(CaseType.SN, EnumSet.of(Country.FR), Interval.parse("2013-01-13T00:00:00+01:00/2013-01-13T00:30:00+01:00"))
                .equals(Sets.newHashSet(DateTime.parse("2013-01-13T00:15:00+01:00"))));
        assertTrue(caseRepository.dataAvailable(CaseType.SN, EnumSet.of(Country.FR), Interval.parse("2013-01-13T00:00:00+01:00/2013-01-13T01:00:00+01:00"))
                .equals(Sets.newHashSet(DateTime.parse("2013-01-13T00:15:00+01:00"), DateTime.parse("2013-01-13T00:45:00+01:00"))));
        assertTrue(caseRepository.dataAvailable(CaseType.SN, EnumSet.of(Country.BE, Country.DE), Interval.parse("2013-01-13T00:00:00+01:00/2013-01-13T01:00:00+01:00"))
                .isEmpty());
        assertTrue(caseRepository.dataAvailable(CaseType.SN, EnumSet.of(Country.FR), Interval.parse("2013-01-14T00:00:00+01:00/2013-01-14T01:00:00+01:00"))
                .equals(Sets.newHashSet(DateTime.parse("2013-01-14T00:15:00+01:00"), DateTime.parse("2013-01-14T00:30:00+01:00"))));
    }

    @Test
    public void testLoadD2() throws Exception {
        assertTrue(caseRepository.load(DateTime.parse("2013-01-15T00:30:00+01:00"), CaseType.D2, Country.FR).size() == 1);
        assertTrue(caseRepository.load(DateTime.parse("2013-01-15T00:45:00+01:00"), CaseType.D2, Country.FR).isEmpty());
    }

    @Test
    public void testLoadLT() throws Exception {
        assertTrue(caseRepository.load(DateTime.parse("2013-01-15T00:30:00+01:00"), CaseType.LT, Country.FR).size() == 1);
        assertTrue(caseRepository.load(DateTime.parse("2013-01-15T00:45:00+01:00"), CaseType.LT, Country.FR).isEmpty());
    }

    @Test
    public void testLoadRE() throws Exception {
        assertTrue(caseRepository.load(DateTime.parse("2013-01-15T00:30:00+01:00"), CaseType.RE, Country.FR).size() == 1);
        assertTrue(caseRepository.load(DateTime.parse("2013-01-15T00:45:00+01:00"), CaseType.RE, Country.FR).isEmpty());
    }

    @Test
    public void testLoadDayLightSaving() throws Exception {
        List<Network> networksCEST=caseRepository.load(DateTime.parse("2016-10-30T02:30:00+02:00"), CaseType.FO, Country.FR);
        assertTrue(networksCEST.size() == 1);
        List<Network> networksCET=caseRepository.load(DateTime.parse("2016-10-30T02:30:00+01:00"), CaseType.FO, Country.FR);
        assertTrue(networksCET.size() == 1);
    }

    @Test
    public void testLoadIDCF() throws Exception {
        assertTrue(caseRepository.load(DateTime.parse("2013-01-15T03:30:00+01:00"), CaseType.IDCF01, Country.FR).size() == 1);
        assertTrue(caseRepository.load(DateTime.parse("2013-01-15T03:30:00+01:00"), CaseType.IDCF02, Country.FR).size() == 1);
        assertTrue(caseRepository.load(DateTime.parse("2013-01-15T03:30:00+01:00"), CaseType.IDCF03, Country.FR).size() == 1);
        assertTrue(caseRepository.load(DateTime.parse("2013-01-15T03:30:00+01:00"), CaseType.IDCF04, Country.FR).isEmpty());
    }

    @Test
    public void testIsDataAvailable2D() throws Exception {
        assertTrue(caseRepository.isDataAvailable(DateTime.parse("2013-01-15T00:30:00+01:00"), CaseType.D2, Country.FR));
    }

    @Test
    public void testDataAvailable2D() throws Exception {
        assertTrue(caseRepository.dataAvailable(CaseType.D2, EnumSet.of(Country.FR), Interval.parse("2013-01-15T00:00:00+01:00/2013-01-15T01:30:00+01:00"))
                .equals(Sets.newHashSet(DateTime.parse("2013-01-15T00:30:00+01:00"))));
    }

    @Test
    public void testDataAvailableIntraday() throws Exception {
        Set<DateTime> dset=caseRepository.dataAvailable(CaseType.IDCF01, EnumSet.of(Country.FR), Interval.parse("2013-01-15T00:00:00+01:00/2013-01-15T05:30:00+01:00"));
        System.out.println(dset);
        assertTrue(dset.equals(Sets.newHashSet(DateTime.parse("2013-01-15T03:30:00+01:00"))));
    }

    @Test
    public void testDataAvailableDayLightSaving() throws Exception {

        // double date CEST + CET
        Set<DateTime> dset=caseRepository.dataAvailable(CaseType.FO, EnumSet.of(Country.FR), Interval.parse("2016-10-30T00:00:00+02:00/2016-10-30T03:30:00+01:00"));
        System.out.println(dset);
        assertTrue(dset.equals(Sets.newHashSet(DateTime.parse("2016-10-30T02:30:00+02:00"),DateTime.parse("2016-10-30T02:30:00+01:00"))));

        //just the CET one
        dset=caseRepository.dataAvailable(CaseType.FO, EnumSet.of(Country.FR), Interval.parse("2016-10-30T02:30:00+01:00/2016-10-30T03:30:00+01:00"));
        System.out.println(dset);
        assertTrue(dset.equals(Sets.newHashSet(DateTime.parse("2016-10-30T02:30:00+01:00"))));
    }

}