/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.model;

import java.util.Objects;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class Indicator {
    private final IndicatorEnum id;
    private final UnitEnum unit;
    private final double value;

    public Indicator(IndicatorEnum id, UnitEnum unit, double value) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(unit);
        this.id = id;
        this.unit = unit;
        this.value = value;
    }

    public IndicatorEnum getId() {
        return id;
    }

    public UnitEnum getUnit() {
        return unit;
    }

    public double getValue() {
        return value;
    }

}
