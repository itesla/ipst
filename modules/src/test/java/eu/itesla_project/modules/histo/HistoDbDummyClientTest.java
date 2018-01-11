/*
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.histo;

import eu.itesla_project.modules.test.HistoDbClientTestFactoryImpl;
import org.apache.commons.io.IOUtils;
import org.joda.time.Interval;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class HistoDbDummyClientTest {


    @Test
    public void testCreateClient() {
        HistoDbClient histoDbClient = new HistoDbClientTestFactoryImpl().create(false);
        assertNull(histoDbClient.getCache());
    }


    @Test
    public void testmethods() throws IOException, InterruptedException {
        Interval interval = Interval.parse("2013-01-14T00:00:00+01:00/2013-01-14T01:00:00+01:00");
        HistoDbClient histoDbClient = new HistoDbClientTestFactoryImpl().create(false);
        assertEquals(histoDbClient.getDbName(), "");
        assertTrue(histoDbClient.listDbs().size() == 0);
        assertTrue(histoDbClient.queryCount(interval, HistoDbHorizon.SN) == 0);
        assertEquals(histoDbClient.listAttributes(), Collections.emptyList());
        assertEquals(IOUtils.toString(histoDbClient.queryCsv(HistoQueryType.data, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), interval, HistoDbHorizon.SN, true, true), StandardCharsets.UTF_8), "");
        assertEquals(IOUtils.toString(histoDbClient.queryCsv(HistoQueryType.data, Collections.emptySet(), interval, HistoDbHorizon.SN, true, true), StandardCharsets.UTF_8), "");
        assertNotNull(histoDbClient.queryStats(Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), interval, HistoDbHorizon.SN, true));
        assertNotNull(histoDbClient.queryStats(Collections.emptySet(), interval, HistoDbHorizon.SN, true));
    }

}