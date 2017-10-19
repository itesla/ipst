/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.test;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClientFactory;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CsvFileContingenciesAndActionsDatabaseClientFactory implements ContingenciesAndActionsDatabaseClientFactory {

    @Override
    public ContingenciesAndActionsDatabaseClient create() {
        ModuleConfig config = PlatformConfig.defaultConfig().getModuleConfig("csvcontingencydb");
        Path csvFile = config.getPathProperty("csvFile");

        return new CsvFileContingenciesAndActionsDatabaseClient(csvFile);
    }

    @Override
    public ContingenciesAndActionsDatabaseClient create(Path csvFile) {
        Objects.requireNonNull(csvFile);


        return new CsvFileContingenciesAndActionsDatabaseClient(csvFile);
    }

    @Override
    public ContingenciesAndActionsDatabaseClient create(InputStream data) {
        Objects.requireNonNull(data);
        return new CsvFileContingenciesAndActionsDatabaseClient(data);
    }

}
