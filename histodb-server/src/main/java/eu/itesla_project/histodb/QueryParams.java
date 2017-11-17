/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.web.context.request.WebRequest;

import eu.itesla_project.histodb.domain.Attribute;
import eu.itesla_project.histodb.domain.CurrentPowerRatioAttribute;
import eu.itesla_project.histodb.domain.NegativePowerAttribute;
import eu.itesla_project.histodb.domain.NegativeReactivePowerAttribute;
import eu.itesla_project.histodb.domain.PositivePowerAttribute;
import eu.itesla_project.histodb.domain.PositiveReactivePowerAttribute;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class QueryParams {

    private final boolean headers;
    private final String horizon;
    private final int forecastTime;
    private final int columnStart;
    private final int columnEnd;
    private final int count;
    private final int start;
    private final List<Attribute> cols;
    private final Long timeFrom;
    private final Long timeTo;
    private final Long dayTimeFrom;
    private final Long dayTimeTo;
    private final List<String> countries;
    private final List<String> regions;
    private final List<String> ids;
    private final List<String> equipments;
    private final List<String> powers;
    private final List<String> attribs;

    public QueryParams(WebRequest request) {
        Objects.requireNonNull(request);

        this.headers = Boolean.parseBoolean(request.getParameter("headers"));
        this.horizon = request.getParameter("horizon");

        String countStr = request.getParameter("count");
        this.count = countStr != null ? Integer.parseInt(countStr) : 50;

        String startStr = request.getParameter("start");
        this.start = startStr != null ? Integer.parseInt(startStr) : 0;

        String colStr = request.getParameter("cols");
        this.cols = colStr != null ? toAttributes(colStr) : null;

        String colRangeStr = request.getParameter("colRange");
        int[] range = null;
        if (colRangeStr != null) {
            if (colRangeStr.equals("*")) {
                range = null;
            } else if (colRangeStr.matches("[0-9]*-[0-9]*")) {
                String[] splitted = colRangeStr.split("-");
                range = new int[2];
                range[0] = Integer.parseInt(splitted[0]);
                range[1] = Integer.parseInt(splitted[1]);
            } else {
                throw new IllegalArgumentException("Wrong colsRange value: " + colRangeStr);
            }
        }

        if (range != null && cols != null) {
            throw new IllegalArgumentException("Cannot use both column range and column names");
        }

        if (cols == null && range == null) {
            range = new int[2];
            range[0] = 0;
            range[1] = 100;
        }

        this.columnStart = range != null ? range[0] : -1;
        this.columnEnd = range != null ? range[1] : -1;

        String forecastStr = request.getParameter("forecast");
        int forecast = -1;
        if (forecastStr != null) {
            try {
                forecast = Integer.parseInt(forecastStr);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Wrong forecast value: " + forecastStr);
            }
        }
        this.forecastTime = forecast;

        String timeStr = request.getParameter("time");
        String daytimeStr = request.getParameter("daytime");

        DateTimeFormatter dateParser = ISODateTimeFormat.dateTimeParser();
        DateTimeFormatter hourParser = ISODateTimeFormat.timeParser();

        Long from = 0L;
        Long to = Long.MAX_VALUE;

        if (timeStr != null) {
            if (timeStr.startsWith("[")) {
                if (!timeStr.endsWith("]")) {
                    throw new IllegalArgumentException("time filter must be of the form [ISODate,ISODate] ");
                }
                String[] dates = timeStr.substring(1, timeStr.length() - 1).split(",");
                from = dateParser.parseMillis(dates[0]);
                to = dateParser.parseMillis(dates[1]);
            } else {
                from = dateParser.parseMillis(timeStr);
                to = from;
            }
        }

        this.timeFrom = from;
        this.timeTo = to;

        Long dTimeFrom = null;
        Long dTimeTo = null;

        if (daytimeStr != null) {
            if (daytimeStr.startsWith("[")) {
                if (!daytimeStr.endsWith("]")) {
                    throw new IllegalArgumentException("daytime filter must be of the form [ISOTime,ISOTime] ");
                }
                String[] times = daytimeStr.substring(1, daytimeStr.length() - 1).split(",");
                dTimeFrom = hourParser.parseMillis(times[0]);
                dTimeTo = hourParser.parseMillis(times[1]);
            } else {
                dTimeFrom = hourParser.parseMillis(daytimeStr);
                dTimeTo = dTimeFrom;
            }
        }
        this.dayTimeFrom = dTimeFrom;
        this.dayTimeTo = dTimeTo;

        String powerTypeStr = request.getParameter("powerType");
        String eqtypeStr = request.getParameter("equip");
        String measuretypeStr = request.getParameter("attr");
        String regionStr = request.getParameter("region");
        String countryStr = request.getParameter("country");
        String equipIdsStr = request.getParameter("ids");

        this.countries = countryStr != null ? Arrays.asList(countryStr.split(",")) : null;
        this.regions = regionStr != null ? Arrays.asList(regionStr.split(",")) : null;
        this.ids = equipIdsStr != null ? Arrays.asList(equipIdsStr.split(",")) : null;
        this.equipments = eqtypeStr != null ? Arrays.asList(eqtypeStr.split(",")) : null;
        this.powers = powerTypeStr != null ? Arrays.asList(powerTypeStr.split(",")) : null;
        this.attribs = measuretypeStr != null ? Arrays.asList(measuretypeStr.split(",")) : null;
    }

    private List<Attribute> toAttributes(String colStr) {
        List<String> cls = Arrays.asList(colStr.split(","));
        return cls.stream().map(c -> getAttribute(c)).collect(Collectors.toList());
    }

    private Attribute getAttribute(String name) {
        if (name.endsWith("_IP")) {
            return new CurrentPowerRatioAttribute(name);
        } else if (name.endsWith("_PP")) {
            return new PositivePowerAttribute(name);
        } else if (name.endsWith("_PN")) {
            return new NegativePowerAttribute(name);
        } else if (name.endsWith("_QP")) {
            return new PositiveReactivePowerAttribute(name);
        } else if (name.endsWith("_QN")) {
            return new NegativeReactivePowerAttribute(name);
        }
        return new Attribute(name);
    }

    public String getHorizon() {
        return horizon;
    }

    public int getForecastTime() {
        return forecastTime;
    }

    public boolean isHeaders() {
        return headers;
    }

    public int getCount() {
        return count;
    }

    public List<Attribute> getCols() {
        return cols;
    }

    public Long getTimeFrom() {
        return timeFrom;
    }

    public Long getTimeTo() {
        return timeTo;
    }

    public Long getDayTimeFrom() {
        return dayTimeFrom;
    }

    public Long getDayTimeTo() {
        return dayTimeTo;
    }

    public List<String> getCountries() {
        return countries;
    }

    public List<String> getRegions() {
        return regions;
    }

    public List<String> getIds() {
        return ids;
    }

    public List<String> getEquipments() {
        return equipments;
    }

    public List<String> getPowers() {
        return powers;
    }

    public List<String> getAttribs() {
        return attribs;
    }

    public int getColumnStart() {
        return columnStart;
    }

    public int getColumnEnd() {
        return columnEnd;
    }

    @Override
    public String toString() {
        return "QueryParams [headers=" + headers + ", horizon=" + horizon + ", forecastTime=" + forecastTime
                + ", columnStart=" + columnStart + ", columnEnd=" + columnEnd + ", start=" + start + ", count=" + count
                + ", cols=" + cols + ", timeFrom=" + timeFrom + ", timeTo=" + timeTo + ", dayTimeFrom=" + dayTimeFrom
                + ", dayTimeTo=" + dayTimeTo + ", countries=" + countries + ", regions=" + regions + ", ids=" + ids
                + ", equipments=" + equipments + ", powers=" + powers + ", attribs=" + attribs + "]";
    }

    public int getStart() {
        return start;
    }

}
