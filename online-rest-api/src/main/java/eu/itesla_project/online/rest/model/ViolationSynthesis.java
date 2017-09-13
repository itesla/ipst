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

import eu.itesla_project.security.LimitViolationType;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class ViolationSynthesis {
    private final String equipment;
    private final LimitViolationType type;
    private final String limitName;
    private final float limit;
    private final float voltageLevel;
    private final List<TimeValue> timeValues = new ArrayList<TimeValue>();

    public ViolationSynthesis(String equipment, float voltageLevel, LimitViolationType type, float limit, String limitName ) {
        this.equipment = Objects.requireNonNull(equipment);
        this.type = Objects.requireNonNull(type);
        this.limit = limit;
        this.voltageLevel = voltageLevel;
        this.limitName = limitName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(equipment, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ViolationSynthesis other = (ViolationSynthesis) obj;
        return (equipment.equals(other.equipment) && type.equals(other.type));
    }

    public String getEquipment() {
        return equipment;
    }

    public LimitViolationType getType() {
        return type;
    }

    public Float getLimit() {
        return limit;
    }

    public List<TimeValue> getTimeValues() {
        return timeValues;
    }

    public void addTimeValue(TimeValue val) {
        Objects.requireNonNull(val);
        timeValues.add(val);
    }

    public String getLimitName() {
        return limitName;
    }

    public float getVoltageLevel() {
        return voltageLevel;
    }
    
}
