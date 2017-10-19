/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.mcla.forecast_errors;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import eu.itesla_project.modules.histo.HistoDbClient;
import eu.itesla_project.modules.mcla.ForecastErrorsAnalyzer;
import eu.itesla_project.modules.mcla.ForecastErrorsAnalyzerFactory;
import eu.itesla_project.modules.mcla.ForecastErrorsDataStorage;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class ForecastErrorsAnalyzerFactoryImpl implements ForecastErrorsAnalyzerFactory {

    @Override
    public ForecastErrorsAnalyzer create(Network network, ComputationManager computationManager,
                                         ForecastErrorsDataStorage forecastErrorsDataStorage, HistoDbClient histoDbClient) {
        return new ForecastErrorsAnalyzerImpl(network, computationManager, forecastErrorsDataStorage, histoDbClient);
    }

}
