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
public abstract class ComputedAttribute extends Attribute {

    public ComputedAttribute(String name) {
        super(name);
    }

    public abstract Object getValue(Map<String, Object> map);

    protected Double toDouble(Object o) {
        return o == null ? null : (Double) o;
    }

}
