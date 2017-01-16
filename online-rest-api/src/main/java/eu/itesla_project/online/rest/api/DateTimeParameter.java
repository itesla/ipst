/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.api;

import java.util.Objects;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
 */
public class DateTimeParameter {

    private final DateTime dateTime;

    public DateTimeParameter(String dateTime) throws ApiException {
        Objects.requireNonNull(dateTime);
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        try {
            this.dateTime = fmt.parseDateTime(dateTime);
        } catch (IllegalArgumentException ex) {
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST)
                    .entity("Wrong date parameter format: " + ex.getMessage()).build());
        }
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public String toString() {
        return dateTime != null ? dateTime.toString() : null;
    }

}
