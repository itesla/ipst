/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.mcla.forecast_errors;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.joda.time.Interval;

import eu.itesla_project.modules.histo.HistoDbAttr;
import eu.itesla_project.modules.histo.HistoDbAttributeId;
import eu.itesla_project.modules.histo.HistoDbClient;
import eu.itesla_project.modules.histo.HistoDbHorizon;
import eu.itesla_project.modules.histo.HistoDbMetaAttributeId;
import eu.itesla_project.modules.histo.HistoDbMetaAttributeType;
import eu.itesla_project.modules.histo.HistoDbNetworkAttributeId;
import eu.itesla_project.modules.histo.HistoQueryType;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public final class FEAHistoDBFacade {

    private FEAHistoDBFacade() {
    }

    public static void historicalDataToCsvFile(HistoDbClient histoDbClient, List<String> generatorsIds, List<String> loadsIds, 
                                               Interval histoInterval, Path historicalDataCsvFile) throws Exception {
        Set<HistoDbAttributeId> attributeIds = new LinkedHashSet<>();
        attributeIds.add(new HistoDbMetaAttributeId(HistoDbMetaAttributeType.datetime));
        attributeIds.add(new HistoDbMetaAttributeId(HistoDbMetaAttributeType.horizon));
        attributeIds.add(new HistoDbMetaAttributeId(HistoDbMetaAttributeType.forecastTime));
        generatorsIds.forEach( generatorId -> 
        {
            attributeIds.add(new HistoDbNetworkAttributeId(generatorId, HistoDbAttr.P));
            attributeIds.add(new HistoDbNetworkAttributeId(generatorId, HistoDbAttr.Q));
        });
        loadsIds.forEach( loadId -> 
        {
            attributeIds.add(new HistoDbNetworkAttributeId(loadId, HistoDbAttr.P));
            attributeIds.add(new HistoDbNetworkAttributeId(loadId, HistoDbAttr.Q));
        });
        try (InputStream is = histoDbClient.queryCsv(HistoQueryType.forecastDiff,
                                                     attributeIds,
                                                     histoInterval,
                                                     HistoDbHorizon.DACF,
                                                     false, 
                                                     false)) {
            Files.copy(is, historicalDataCsvFile);
        }
    }

}
