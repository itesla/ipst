/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb.repository.mapdb;

import java.io.Serializable;
import java.util.Comparator;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class HistoKey implements Comparable<HistoKey>, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Comparator<String> NULL_SAFE_COMPARATOR = Comparator.nullsLast(String::compareToIgnoreCase);


    private final String horizon;
    private final Long dateTime;
    private final int forecastDistance;

    public HistoKey(String horizon, long datetime, int forecastDistance) {
        this.horizon = horizon;
        this.dateTime = datetime;
        this.forecastDistance = forecastDistance;
    }

    public String getHorizon() {
        return horizon;
    }

    public long getDateTime() {
        return dateTime;
    }

    public int getForecastDistance() {
        return forecastDistance;
    }

    @Override
    public int compareTo(HistoKey other) {
        return Comparator.comparing(HistoKey::getDateTime).thenComparing(HistoKey::getHorizon, NULL_SAFE_COMPARATOR)
                .thenComparingInt(HistoKey::getForecastDistance).compare(this, other);
    }

}
