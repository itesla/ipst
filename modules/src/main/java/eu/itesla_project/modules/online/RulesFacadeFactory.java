/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.online;

import eu.itesla_project.modules.rules.RulesDbClient;

/**
*
* @author Quinary <itesla@quinary.com>
*/
public interface RulesFacadeFactory {

     OnlineRulesFacade create(RulesDbClient rulesDbClient);

}
