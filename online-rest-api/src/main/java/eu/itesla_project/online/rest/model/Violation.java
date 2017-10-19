/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
 */
public class Violation {

    private final String equipment;
    private final String country;
    private final int voltageLevel;
    private final String type;
    private final float value;
    private final float limit;

    public Violation(String country, String equipment, String type, float limit, float value, int voltageLevel) {
        this.country = Objects.requireNonNull(country);
        this.equipment = Objects.requireNonNull(equipment);
        this.type = Objects.requireNonNull(type);
        this.limit = limit;
        this.value = value;
        this.voltageLevel = voltageLevel;
    }

    @JsonProperty("equipment")
    public String getEquipment() {
        return equipment;
    }

    @JsonProperty("country")
    public String getCountry() {
        return country;
    }

    @JsonProperty("voltageLevel")
    public int getVoltageLevel() {
        return voltageLevel;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("value")
    public float getValue() {
        return value;
    }

    @JsonProperty("limit")
    public float getLimit() {
        return limit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Violation violation = (Violation) o;
        return Objects.equals(equipment, violation.equipment) && Objects.equals(country, violation.country)
                && Objects.equals(voltageLevel, violation.voltageLevel) && Objects.equals(type, violation.type)
                && Objects.equals(value, violation.value) && Objects.equals(limit, violation.limit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(equipment, country, voltageLevel, type, value, limit);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Violation {").append(System.lineSeparator());

        sb.append("    equipment: ").append(toIndentedString(equipment)).append(System.lineSeparator());
        sb.append("    country: ").append(toIndentedString(country)).append(System.lineSeparator());
        sb.append("    voltageLevel: ").append(toIndentedString(voltageLevel)).append(System.lineSeparator());
        sb.append("    type: ").append(toIndentedString(type)).append(System.lineSeparator());
        sb.append("    value: ").append(toIndentedString(value)).append(System.lineSeparator());
        sb.append("    limit: ").append(toIndentedString(limit)).append(System.lineSeparator());
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace(System.lineSeparator(), System.lineSeparator() + "    ");
    }
}
