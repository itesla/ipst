/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.ucte.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.toIntExact;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class EntsoeFileName {

    private static final Pattern DATE_REGEX = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})_(B\\d|\\d{2})(\\d{2})_(\\w{2})(\\d)_(\\w{2})(\\d).*");

    private final DateTime date;

    private final int forecastDistance;

    private final UcteGeographicalCode geographicalCode;

    private static long calcForecastDistance(DateTime date, int offset) {
        return new Interval(date.withTimeAtStartOfDay(),date).toDuration().getStandardMinutes() + offset;
    }


    public static EntsoeFileName parse(String str) {
        DateTime date = DateTime.now();
        int forecastDistance = 0;
        UcteGeographicalCode geographicalCode = null;
        Matcher m = DATE_REGEX.matcher(str);
        if (m.matches()) {
            // time zone is Europe/Paris
            int year = Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2));
            int dayOfMonth = Integer.parseInt(m.group(3));
            String hourDayGroup = m.group(4);
            boolean cestFlag=hourDayGroup.startsWith("B");
            int hourOfDay = (cestFlag==true) ? Integer.parseInt(m.group(4).substring(1)) : Integer.parseInt(m.group(4));
            int minute = Integer.parseInt(m.group(5));
            date = new DateTime(year, month, dayOfMonth, hourOfDay, minute, DateTimeZone.forID("Europe/Paris"));
            if (cestFlag) {
                date = date.plusHours(1);
            }
            String fileType=m.group(6);
            String dweekS=m.group(7);
            String cCode=m.group(8);
            String versionS=m.group(9);

            // extract forecast distance
            switch (fileType) {
                case "FO" : // Day ahead forecast
                    forecastDistance = toIntExact(calcForecastDistance(date, 6*60)); // DACF generated at 18:00 one day ahead7
                    break;
                case "2D" : // Two days ahead forecast
                    forecastDistance = toIntExact(calcForecastDistance(date, 29*60)); // D2CF generated at 19:00 one day ahead7
                    break;
                case "SN" : // Snapshot
                    forecastDistance = 0;
                    break;
                case "RE" : //Reference //TODO forecastDistance
                    forecastDistance = 0;
                    break;
                case "LR" : //Long Term Reference  //TODO forecastDistance
                    forecastDistance = 0;
                    break;
                default: //hh Intraday Forecasts, for the rest of the day: two-digits number is the forecast distance, in hours
                    forecastDistance = Integer.parseInt(m.group(6)) * 60;
                    break;
            }

            geographicalCode = UcteGeographicalCode.valueOf(cCode);
        }
        return new EntsoeFileName(date, forecastDistance, geographicalCode);
    }

    private EntsoeFileName(DateTime date, int forecastDistance, UcteGeographicalCode geographicalCode) {
        this.date = date;
        this.forecastDistance = forecastDistance;
        this.geographicalCode = geographicalCode;
    }

    public DateTime getDate() {
        return date;
    }

    public int getForecastDistance() {
        return forecastDistance;
    }

    public UcteGeographicalCode getGeographicalCode() {
        return geographicalCode;
    }

    public String getCountry() {
        return geographicalCode != null ? geographicalCode.getCountry().toString() : null;
    }

}
