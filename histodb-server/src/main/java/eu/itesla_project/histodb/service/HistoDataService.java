/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb.service;

import java.io.IOException;
import java.nio.file.Path;

import eu.itesla_project.histodb.QueryParams;
import eu.itesla_project.histodb.domain.DataSet;
import eu.itesla_project.histodb.repository.mapdb.HistoDataSource;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public interface HistoDataService {

    public void importData(HistoDataSource histoDataSource, Path dir, boolean parallel) throws Exception;

    public DataSet getData(HistoDataSource histoDataSource, QueryParams queryParams);

    void importReferenceNetwork(HistoDataSource datasource, Path file) throws IOException;

    public DataSet getForecastDiff(HistoDataSource hds, QueryParams queryParams);

    public DataSet getStats(HistoDataSource hds, QueryParams queryParams);

}
