/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb.repository.mapdb;

import java.io.IOException;
import java.util.HashMap;
import eu.itesla_project.histodb.config.HistoDbConfiguration;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public abstract class HistoDataSourceFactory {

    public static final HashMap<String, HistoDataSource> MEM_SOURCES = new HashMap();

    public static HistoDataSource getInstance(HistoDbConfiguration config, String name, String prefix, String postfix) throws IOException {
        if (config.getMapDb().isPersistent()) {
            return new HistoDataSource(config, name, prefix, postfix);
        } else {
            String key = name + "_" + prefix + "_" + postfix;
            HistoDataSource ds = MEM_SOURCES.get(key);
            if (ds == null) {
                ds = new HistoDataSource(config, name, prefix, postfix);
                MEM_SOURCES.put(key, ds);
            }
            return ds;
        }
    }
}
