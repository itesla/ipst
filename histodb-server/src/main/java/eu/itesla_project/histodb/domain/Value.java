/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb.domain;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class Value {

    private final Object object;

    public Value(Object val) {
        if (val != null && val instanceof Double && ((Double) val).equals(Double.NaN)) {
            this.object = null;
        } else {
            this.object = val;
        }
    }

    public String toString() {
        if (object == null) {
            return "";
        } else {
            return object.toString();
        }
    }

    public Object getObject() {
        return object;
    }

}
