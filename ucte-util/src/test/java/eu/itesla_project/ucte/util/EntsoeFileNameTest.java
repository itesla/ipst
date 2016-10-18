/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.ucte.util;

import eu.itesla_project.iidm.network.Country;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class EntsoeFileNameTest {

    @Test
    public void testValidName() {
        String fileName = "20140213_0830_SN4_D20";
        EntsoeFileName entsoeFileName = EntsoeFileName.parse(fileName);
        assertTrue(entsoeFileName.getDate().isEqual(DateTime.parse("2014-02-13T08:30:00.000+01:00")));
        assertTrue(entsoeFileName.getForecastDistance() == 0);
        assertTrue(entsoeFileName.getCountry().equals("DE"));
        assertTrue(entsoeFileName.getGeographicalCode() == UcteGeographicalCode.D2);
    }

    @Test
    public void testInvalidName() {
        String fileName = "???";
        EntsoeFileName entsoeFileName = EntsoeFileName.parse(fileName);
        assertTrue(entsoeFileName.getForecastDistance() == 0);
        assertTrue(entsoeFileName.getCountry() == null);
    }

    @Test
    public void testValidNameDaylightSaving() {
        String fileName = "20161027_B230_SN7_D20";
        EntsoeFileName entsoeFileName = EntsoeFileName.parse(fileName);
        assertTrue(entsoeFileName.getDate().isEqual(DateTime.parse("2016-10-27T02:30:00.000+01:00")));
        assertTrue(entsoeFileName.getForecastDistance() == 0);
        assertTrue(entsoeFileName.getCountry().equals("DE"));
        assertTrue(entsoeFileName.getGeographicalCode() == UcteGeographicalCode.D2);
    }

    @Test
    public void testValidName2DaysAhead() {
        String fileName = "20161027_0230_2D7_D20";
        EntsoeFileName entsoeFileName = EntsoeFileName.parse(fileName);
        assertTrue(entsoeFileName.getDate().isEqual(DateTime.parse("2016-10-27T02:30:00.000+02:00")));
        assertTrue(entsoeFileName.getForecastDistance() == 1890);
        assertTrue(entsoeFileName.getCountry().equals("DE"));
        assertTrue(entsoeFileName.getGeographicalCode() == UcteGeographicalCode.D2);
    }

    @Test
    public void testValidNameIntraday() {
        String fileName = "20161027_0230_037_D20";
        EntsoeFileName entsoeFileName = EntsoeFileName.parse(fileName);
        assertTrue(entsoeFileName.getDate().isEqual(DateTime.parse("2016-10-27T02:30:00.000+02:00")));
        assertTrue(entsoeFileName.getForecastDistance() == 3*60);
        assertTrue(entsoeFileName.getCountry().equals("DE"));
        assertTrue(entsoeFileName.getGeographicalCode() == UcteGeographicalCode.D2);
    }

    @Test
    public void testValidNameReference() {
        String fileName = "20161027_0230_RE7_D20";
        EntsoeFileName entsoeFileName = EntsoeFileName.parse(fileName);
        assertTrue(entsoeFileName.getDate().isEqual(DateTime.parse("2016-10-27T02:30:00.000+02:00")));
        assertTrue(entsoeFileName.getForecastDistance() == 0); //TODO
        assertTrue(entsoeFileName.getCountry().equals("DE"));
        assertTrue(entsoeFileName.getGeographicalCode() == UcteGeographicalCode.D2);
    }
    @Test
    public void testValidNameLongTimeReference() {
        String fileName = "20161027_0230_LR7_FR0";
        EntsoeFileName entsoeFileName = EntsoeFileName.parse(fileName);
        assertTrue(entsoeFileName.getDate().isEqual(DateTime.parse("2016-10-27T02:30:00.000+02:00")));
        assertTrue(entsoeFileName.getForecastDistance() == 0); //TODO
        assertTrue(entsoeFileName.getCountry().equals("FR"));
        assertTrue(entsoeFileName.getGeographicalCode() == UcteGeographicalCode.FR);
    }

    private static List<DateTime> getDateRange(DateTime start, DateTime end, int minutesStep) {
        List<DateTime> ret = new ArrayList<DateTime>();
        DateTime tmp = start;
        while (tmp.isBefore(end) || tmp.equals(end)) {
            ret.add(tmp);
            tmp = tmp.plusMinutes(minutesStep);
        }
        return ret;
    }

    private static String toEntsoeFileName(DateTime date, String typeS, UcteGeographicalCode countryCode, int version, String fileSuffix) {
        DateTime testDate1=date.minusHours(1);
        String ret=String.format("%04d%02d%02d_%02d%02d_" + typeS + "%01d_" + countryCode.name() + "%01d" + "."+ fileSuffix,
                date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), date.getHourOfDay(), date.getMinuteOfHour(), date.getDayOfWeek(), version);
        if (testDate1.getHourOfDay() == date.getHourOfDay()) {
            ret = ret.substring(0,9)+'B'+ret.substring(10);
        }
        return ret;
    }


    @Test
    public void testDateRangeSnapshots() {
        DateTimeZone parisZone=DateTimeZone.forID("Europe/Paris");

        DateTime startDate= LocalDateTime.parse("2016-01-01T00:45:00.000").toDateTime(parisZone);
        DateTime endDate=LocalDateTime.parse("2016-12-31T23:59:00.000").toDateTime(parisZone);

        // snapshots, generated each quarter of hour
        final int[] idx = {0};
        getDateRange(startDate,endDate,15).stream().forEach(date -> {
            String entsoeFilenameS=toEntsoeFileName(date,"SN",UcteGeographicalCode.FR,0, "uct");
            EntsoeFileName entsoeFilename= EntsoeFileName.parse(entsoeFilenameS);
            assert(entsoeFilename.getForecastDistance() == 0);
            assert(entsoeFilename.getCountry().equals(Country.FR.name()));
            assert(entsoeFilename.getGeographicalCode().equals(UcteGeographicalCode.FR));
            assert(entsoeFilename.getDate().toString().equals(date.toString()));
            idx[0]++;
        });
        System.out.println("tested: " + idx[0] + " dates.");

    }

    @Test
    public void testDateRangeForecasts() {
        DateTimeZone parisZone=DateTimeZone.forID("Europe/Paris");

        DateTime startDate=LocalDateTime.parse("2016-01-01T00:30:00.000").toDateTime(parisZone);
        DateTime endDate=LocalDateTime.parse("2016-12-31T23:59:00.000").toDateTime(parisZone);

        // snapshots, generated each quarter of hour
        final int[] idx = {0};
        getDateRange(startDate,endDate,30).stream().forEach(date -> {
            String entsoeFilenameS=toEntsoeFileName(date,"FO",UcteGeographicalCode.D2, 0, "uct");
            EntsoeFileName entsoeFilename= EntsoeFileName.parse(entsoeFilenameS);
            assertTrue(entsoeFilename.getCountry().equals(Country.DE.name()));
            assertTrue(entsoeFilename.getGeographicalCode().equals(UcteGeographicalCode.D2));
            assertTrue(entsoeFilename.getDate().toString().equals(date.toString()));
            idx[0]++;
        });
        System.out.println("tested: " + idx[0] + " dates.");

    }


}
