/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class ViolationSynthesis {
    private final String equipment;
    private final String type;
    private final float limit;
    private final List<TimeValue> timeValues = new ArrayList<TimeValue>();

    public ViolationSynthesis(String equipment, String type, float limit ) {
        Objects.requireNonNull(equipment);
        Objects.requireNonNull(type);
        this.equipment = equipment;
        this.type = type;
        this.limit = limit;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((equipment == null) ? 0 : equipment.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ViolationSynthesis other = (ViolationSynthesis) obj;
        if (equipment == null) {
            if (other.equipment != null)
                return false;
        } else if (!equipment.equals(other.equipment))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    public String getEquipment() {
        return equipment;
    }

    public String getType() {
        return type;
    }

    public Float getLimit() {
        return limit;
    }

    public List<TimeValue> getTimeValues() {
        return timeValues;
    }

    public void addTimeValue(TimeValue val){
        Objects.requireNonNull(val);
        timeValues.add(val);
    }
    
}
