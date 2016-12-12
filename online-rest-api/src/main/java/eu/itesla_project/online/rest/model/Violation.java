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
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2016-10-06T14:01:02.692Z")
public class Violation {

    private String equipment = null;
    private String country = null;
    private Integer voltageLevel = null;
    private String type = null;
    private Float value = null;
    private Float limit = null;

    /**
     **/

    @JsonProperty("equipment")
    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    /**
     **/

    @JsonProperty("country")
    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    /**
     **/

    @JsonProperty("voltageLevel")
    public Integer getVoltageLevel() {
        return voltageLevel;
    }

    public void setVoltageLevel(Integer voltageLevel) {
        this.voltageLevel = voltageLevel;
    }

    /**
     **/

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     **/

    @JsonProperty("value")
    public Float getValue() {
        return value;
    }

    public void setValue(Float value) {
        this.value = value;
    }

    /**
     **/

    @JsonProperty("limit")
    public Float getLimit() {
        return limit;
    }

    public void setLimit(Float limit) {
        this.limit = limit;
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
