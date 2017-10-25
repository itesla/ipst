/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.histo;

import java.util.Objects;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class HistoDbMetaAttributeId extends HistoDbAttributeId {

    public static final HistoDbMetaAttributeId CIM_NAME = new HistoDbMetaAttributeId(HistoDbMetaAttributeType.cimName);
    @Deprecated
    @SuppressWarnings("checkstyle:constantname")
    public static final HistoDbMetaAttributeId cimName = CIM_NAME;

    public static final HistoDbMetaAttributeId DATE_TIME = new HistoDbMetaAttributeId(HistoDbMetaAttributeType.datetime);;
    @Deprecated
    @SuppressWarnings("checkstyle:constantname")
    public static final HistoDbMetaAttributeId datetime = DATE_TIME;

    public static final HistoDbMetaAttributeId DAY_TIME = new HistoDbMetaAttributeId(HistoDbMetaAttributeType.daytime);
    @Deprecated
    @SuppressWarnings("checkstyle:constantname")
    public static final HistoDbMetaAttributeId daytime = DATE_TIME;

    public static final HistoDbMetaAttributeId MONTH = new HistoDbMetaAttributeId(HistoDbMetaAttributeType.month);
    @Deprecated
    @SuppressWarnings("checkstyle:constantname")
    public static final HistoDbMetaAttributeId month = MONTH;

    public static final HistoDbMetaAttributeId FORECAST_TIME = new HistoDbMetaAttributeId(HistoDbMetaAttributeType.forecastTime);
    @Deprecated
    @SuppressWarnings("checkstyle:constantname")
    public static final HistoDbMetaAttributeId forecastTime = FORECAST_TIME;

    public static final HistoDbMetaAttributeId HORIZON = new HistoDbMetaAttributeId(HistoDbMetaAttributeType.horizon);
    @Deprecated
    @SuppressWarnings("checkstyle:constantname")
    public static final HistoDbMetaAttributeId horizon = HORIZON;


    private final HistoDbMetaAttributeType type;

    public HistoDbMetaAttributeId(HistoDbMetaAttributeType type) {
        this.type = Objects.requireNonNull(type);
    }

    public HistoDbMetaAttributeType getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HistoDbMetaAttributeId) {
            return type.equals(((HistoDbMetaAttributeId) obj).getType());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
