/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.contingencies;

import java.nio.file.Path;

import com.powsybl.contingency.ContingenciesProviderFactory;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface ContingenciesAndActionsDatabaseClientFactory extends ContingenciesProviderFactory {

    @Override
    ContingenciesAndActionsDatabaseClient create();

    @Override
    default ContingenciesAndActionsDatabaseClient create(Path contingenciesAndActionsFile) {
        return create();
    }

}
