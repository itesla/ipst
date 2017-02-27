/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.joda.time.DateTime;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class TimeValue {
    private final DateTime timestamp;
    private final Map<IndicatorEnum, Indicator> indicators = new HashMap<IndicatorEnum, Indicator>();

    public TimeValue(DateTime timestamp) {
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public Collection<Indicator> getIndicators() {
        return indicators.values();
    }

    public void putIndicator(Indicator indicator) {
        Objects.requireNonNull(indicator);
        this.indicators.put(indicator.getId(),indicator);
    }

}
