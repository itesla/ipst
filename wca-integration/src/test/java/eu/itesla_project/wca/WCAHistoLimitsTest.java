/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.joda.time.Interval;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import eu.itesla_project.commons.util.StringToIntMapper;
import eu.itesla_project.commons.datasource.MemDataSource;
import eu.itesla_project.iidm.export.ampl.AmplSubset;
import eu.itesla_project.iidm.export.ampl.AmplUtil;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.test.NetworkTest1Factory;
import eu.itesla_project.modules.histo.HistoDbAttr;
import eu.itesla_project.modules.histo.HistoDbClient;
import eu.itesla_project.modules.histo.HistoDbHorizon;
import eu.itesla_project.modules.histo.HistoDbNetworkAttributeId;
import eu.itesla_project.modules.histo.HistoDbStats;
import eu.itesla_project.modules.histo.HistoDbStatsType;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCAHistoLimitsTest {

    @Test
    public void testWrite() throws IOException, InterruptedException {
        Interval histoInterval = Interval.parse("2013-01-01T00:00:00+01:00/2013-01-31T23:59:00+01:00");

        Network network = NetworkTest1Factory.create();

        HistoDbClient histoDbClient = Mockito.mock(HistoDbClient.class);
        HistoDbStats histoDbStats = new HistoDbStats();
        histoDbStats.setValue(HistoDbStatsType.MIN, new HistoDbNetworkAttributeId(network.getLoads().iterator().next().getId(), HistoDbAttr.P), 0f);
        histoDbStats.setValue(HistoDbStatsType.MAX, new HistoDbNetworkAttributeId(network.getLoads().iterator().next().getId(), HistoDbAttr.P), 20f);
        histoDbStats.setValue(HistoDbStatsType.MIN, new HistoDbNetworkAttributeId(network.getGenerators().iterator().next().getId(), HistoDbAttr.P), 200f);
        histoDbStats.setValue(HistoDbStatsType.MAX, new HistoDbNetworkAttributeId(network.getGenerators().iterator().next().getId(), HistoDbAttr.P), 900f);
        Mockito.when(histoDbClient.queryStats(Matchers.anySet(), Matchers.eq(histoInterval), Matchers.eq(HistoDbHorizon.SN), Matchers.eq(true)))
               .thenReturn(histoDbStats);

        MemDataSource dataSource = new MemDataSource();

        StringToIntMapper<AmplSubset> mapper = new StringToIntMapper<>(AmplSubset.class);
        AmplUtil.fillMapper(mapper, network);

        WCAHistoLimits histoLimits = new WCAHistoLimits(histoInterval);
        histoLimits.load(network, histoDbClient);
        histoLimits.write(dataSource, mapper);

        String fileContent = String.join(System.lineSeparator(),
                                         "#loads historical data " + histoInterval,
                                         "#\"num\" \"min p (MW)\" \"max p (MW)\" \"id\"",
                                         "1 0.00000 20.0000 \""+ network.getLoads().iterator().next().getId() + "\"");
        assertEquals(fileContent, new String(dataSource.getData(WCAConstants.HISTO_LOADS_FILE_SUFFIX, WCAConstants.TXT_EXT), StandardCharsets.UTF_8).trim());

        fileContent = String.join(System.lineSeparator(),
                                  "#generators historical data " + histoInterval,
                                  "#\"num\" \"min p (MW)\" \"max p (MW)\" \"id\"",
                                  "1 200.000 900.000 \""+ network.getGenerators().iterator().next().getId() + "\"");
        assertEquals(fileContent, new String(dataSource.getData(WCAConstants.HISTO_GENERATORS_FILE_SUFFIX, WCAConstants.TXT_EXT), StandardCharsets.UTF_8).trim());
    }

}
