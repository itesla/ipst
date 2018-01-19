/*
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.test;

import com.powsybl.iidm.network.Country;
import eu.itesla_project.modules.histo.*;
import eu.itesla_project.modules.histo.cache.HistoDbCache;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class HistoDbClientTestImpl implements HistoDbClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoDbClientTestImpl.class);

    private InputStream createInputStreamFromAnEmptyString() {
        return new ByteArrayInputStream("".getBytes());
    }

    @Override
    public HistoDbCache getCache() {
        return null;
    }


    @Override
    public List<HistoDbAttributeId> listAttributes() throws IOException {
        return Collections.emptyList();
    }

    @Override
    public int queryCount(Interval interval, HistoDbHorizon horizon) throws IOException {
        return 0;
    }

    @Override
    public HistoDbStats queryStats(Set<Country> countries, Set<HistoDbEquip> equips,
                                   Set<HistoDbAttr> attrs, Interval interval, HistoDbHorizon horizon, boolean async) throws IOException, InterruptedException {
        return new HistoDbStats();
    }

    @Override
    public HistoDbStats queryStats(Set<HistoDbAttributeId> attrIds, Interval interval, HistoDbHorizon horizon, boolean async) throws IOException, InterruptedException {
        return new HistoDbStats();
    }


    @Override
    public InputStream queryCsv(HistoQueryType queryType, Set<Country> countries, Set<HistoDbEquip> equips,
                                Set<HistoDbAttr> attrs, Interval interval, HistoDbHorizon horizon, boolean zipped, boolean async) throws IOException, InterruptedException {
        return createInputStreamFromAnEmptyString();
    }

    @Override
    public InputStream queryCsv(HistoQueryType queryType, Set<HistoDbAttributeId> attrIds, Interval interval, HistoDbHorizon horizon, boolean zipped, boolean async) throws IOException, InterruptedException {
        return createInputStreamFromAnEmptyString();
    }


    @Override
    public List<String> listDbs() {
        return Collections.emptyList();
    }

    @Override
    public String getDbName() {
        return "";
    }

    @Override
    public void setDbName(String dbName) {
        LOGGER.info("set db to {} ", dbName);
    }

    @Override
    public void clearDb() {
        LOGGER.info("cleardb");
    }

    @Override
    public void clearDb(String dbName) {
        LOGGER.info("cleardb({})", dbName);
    }

    @Override
    public void close() throws Exception {
    }

}
