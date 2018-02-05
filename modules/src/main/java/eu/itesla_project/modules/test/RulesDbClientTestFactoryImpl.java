/*
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.test;

import eu.itesla_project.modules.rules.RulesDbClient;
import eu.itesla_project.modules.rules.RulesDbClientFactory;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class RulesDbClientTestFactoryImpl implements RulesDbClientFactory {

    @Override
    public RulesDbClient create(String rulesDbName) {
        return new RulesDbClientTestImpl();
    }

}
