/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online;

import com.fasterxml.jackson.annotation.JsonValue;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public enum UnitEnum {
    MW("MW"), KV("KV"), PERCENTAGE("%");

    private final String value;

    UnitEnum(String value)
    {
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString(){
        return value;
    }

}
