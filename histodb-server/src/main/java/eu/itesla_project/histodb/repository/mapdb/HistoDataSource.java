/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb.repository.mapdb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.mapdb.Atomic;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;

import eu.itesla_project.histodb.config.HistoDbConfiguration;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class HistoDataSource implements AutoCloseable {

    static Logger log = LoggerFactory.getLogger(HistoDataSource.class);
    private final String name;
    private final String prefix;
    private final String postfix;
    private final Path storeDir;
    private Network referenceNetwork;
    private final DB db;
    private final HistoDbConfiguration config;

    public DB getDb() {
        return db;
    }

    public HistoDataSource(HistoDbConfiguration config, String name, String prefix, String postfix) throws IOException {
        this.config = Objects.requireNonNull(config);
        this.name = Objects.requireNonNull(name);
        this.prefix = Objects.requireNonNull(prefix);
        this.postfix = Objects.requireNonNull(postfix);
        if (config.getMapDb().isPersistent()) {
            this.storeDir = Files.createDirectories(Paths.get(config.getMapDb().getBasedir(), name, prefix, postfix));
            File store = new File(storeDir.toFile(), "mapdb");
            DBMaker.Maker maker = DBMaker.fileDB(store);
            maker.fileMmapEnableIfSupported().fileMmapPreclearDisable()
                    .fileChannelEnable()
                    .closeOnJvmShutdown();
            this.db = maker.make();
            String net = (String) db.atomicString("referenceNetwotk").createOrOpen().get();
            if (net != null) {
                this.referenceNetwork = Importers.loadNetwork(Paths.get(storeDir.toString(), net),
                        LocalComputationManager.getDefault(), new ImportConfig(), (Properties) null);
            }
        } else {
            this.storeDir = null;
            DBMaker.Maker maker = DBMaker.memoryDB();
            this.db = maker.make();
        }
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getPostfix() {
        return postfix;
    }

    public BTreeMap<HistoKey, Map<String, Object>> getMap() {
        return (BTreeMap<HistoKey, Map<String, Object>>) db.treeMap(name + prefix + postfix).valueSerializer(new MapSerializer())
                .valuesOutsideNodesEnable().createOrOpen();
    }

    public void saveReferenceNetwork(Network network) throws IOException {
        Objects.requireNonNull(network);
        if (!config.getMapDb().isPersistent()) {
            this.referenceNetwork = network;
        } else if (this.referenceNetwork == null || !this.referenceNetwork.getId().equals(network.getId())) {
            DataSource dataSource = new FileDataSource(storeDir, network.getId());
            Properties parameters = new Properties();
            parameters.setProperty("iidm.export.xml.indent", "true");
            Exporters.export("XIIDM", network, parameters, dataSource);
            Atomic.String reference = db.atomicString("referenceNetwotk").createOrOpen();
            String oldNet = reference.get();
            reference.set(network.getId() + ".xiidm");
            this.referenceNetwork = network;
            if (oldNet != null) {
                Files.delete(Paths.get(storeDir.toString(), oldNet));
            }
        }
    }

    public Network getReferenceNetwork() {
        return referenceNetwork;
    }

    public void commit() {
        if (db != null) {
            db.commit();
        }
    }

    @Override
    public void close() throws Exception {
        if (db != null && config.getMapDb().isPersistent()) {
            db.close();
        }
    }
}
