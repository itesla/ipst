/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb.domain;

import java.util.Map;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class PositiveReactivePowerAttribute extends ComputedAttribute {

    public PositiveReactivePowerAttribute(String name) {
        super(name);
    }

    @Override
    public Object getValue(Map<String, Object> map) {
        String prefix = getName().substring(0, getName().indexOf("_QP"));
        Double q = toDouble(map.get(prefix + "_Q"));
        return (q != null && q > 0) ? q : null;
    }

}
