/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.mcla;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import eu.itesla_project.mcla.forecast_errors.FEAHistoDBFacade;
import eu.itesla_project.modules.histo.HistoDbClient;
import eu.itesla_project.modules.histo.HistoDbHorizon;
import eu.itesla_project.modules.histo.HistoQueryType;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class FEAHistoDBFacadeTest {

    private FileSystem fileSystem;

    @Before
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    public void testHistoricalDataToCsvFile() throws Exception {
        List<String> generatorsIds = Arrays.asList("generator1", "generator2");
        List<String> loadsIds = Arrays.asList("load1", "load2", "load3");
        Interval histoInterval = Interval.parse("2013-01-01T00:00:00+01:00/2013-01-31T23:59:00+01:00");
        
        String csvContent = String.join(System.lineSeparator(),
                                        String.join(",", 
                                                    "datetime",
                                                    "horizon",
                                                    "forecastTime",
                                                    generatorsIds.stream().map(generatorId -> String.join(",", generatorId + "_P", generatorId + "_Q")).collect(Collectors.joining(",")),
                                                    loadsIds.stream().map(loadId -> String.join(",", loadId + "_P", loadId + "_Q")).collect(Collectors.joining(","))),
                                        String.join(",", 
                                                    "Fri 01 Jan 2013 00:00:00 GMT","720","DACF",
                                                    "0.1","-0.1","0.2","-0.2",
                                                    "0.1","-0.1","0.2","-0.2","0.3","-0.3"),
                                        String.join(",", 
                                                    "Fri 01 Jan 2013 00:00:00 GMT","0","SN",
                                                    "0.11","-0.11","0.21","-0.21",
                                                    "0.11","-0.11","0.21","-0.21","0.31","-0.31"));

        HistoDbClient histoDbClient = Mockito.mock(HistoDbClient.class);
        Mockito.when(histoDbClient.queryCsv(Matchers.eq(HistoQueryType.forecastDiff), 
                                            Matchers.any(), 
                                            Matchers.eq(histoInterval), 
                                            Matchers.eq(HistoDbHorizon.DACF), 
                                            Matchers.eq(false),
                                            Matchers.eq(false)))
               .thenReturn(new ByteArrayInputStream(csvContent.getBytes()));
        
        
        String feaCsvFileName = "forecasterrors_historicaldata.csv";
        Path workingDir = Files.createDirectory(fileSystem.getPath("/working-dir"));
        
        Path historicalDataCsvFile = workingDir.resolve(feaCsvFileName);
        FEAHistoDBFacade.historicalDataToCsvFile(histoDbClient,
                                                 generatorsIds,
                                                 loadsIds,
                                                 histoInterval,
                                                 historicalDataCsvFile);
        
        assertTrue(Files.exists(historicalDataCsvFile));
        try (InputStream expectedStream = new ByteArrayInputStream(csvContent.getBytes());
             InputStream actualStream = Files.newInputStream(historicalDataCsvFile)) {
            assertTrue(IOUtils.contentEquals(expectedStream, actualStream));
        }
        
    }

}
