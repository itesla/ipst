/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.dymola.contingency;

import java.nio.file.Path;
import java.util.Objects;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.contingency.ContingenciesProviderFactory;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class CsvFileModelicaContingenciesAndActionsDatabaseClientFactory implements ContingenciesProviderFactory {

    @Override
    public CsvFileModelicaContingenciesAndActionsDatabaseClient create() {
        ModuleConfig config = PlatformConfig.defaultConfig().getModuleConfig("csvcontingencydb");
        Path csvFile = config.getPathProperty("csvFile");
        return new CsvFileModelicaContingenciesAndActionsDatabaseClient(csvFile);
    }

    @Override
    public CsvFileModelicaContingenciesAndActionsDatabaseClient create(Path csvFile) {
        Objects.requireNonNull(csvFile);
        return new CsvFileModelicaContingenciesAndActionsDatabaseClient(csvFile);
    }

}
