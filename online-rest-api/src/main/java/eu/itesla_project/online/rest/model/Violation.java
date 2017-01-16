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
    private final Integer voltageLevel;
    private final String type;
    private final Float value;
    private final Float limit;

    public Violation(String country, String equipment, String type, float limit, float value, int voltageLevel) {
        this.country = Objects.requireNonNull(country);
        this.equipment = Objects.requireNonNull(equipment);
        this.type = Objects.requireNonNull(type);
        this.limit = Objects.requireNonNull(limit);
        this.value = Objects.requireNonNull(value);
        this.voltageLevel = Objects.requireNonNull(voltageLevel);
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
    public Integer getVoltageLevel() {
        return voltageLevel;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("value")
    public Float getValue() {
        return value;
    }

    @JsonProperty("limit")
    public Float getLimit() {
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
        sb.append("class Violation {\n");

        sb.append("    equipment: ").append(toIndentedString(equipment)).append("\n");
        sb.append("    country: ").append(toIndentedString(country)).append("\n");
        sb.append("    voltageLevel: ").append(toIndentedString(voltageLevel)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    value: ").append(toIndentedString(value)).append("\n");
        sb.append("    limit: ").append(toIndentedString(limit)).append("\n");
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
        return o.toString().replace("\n", "\n    ");
    }
}
