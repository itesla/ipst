/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.api;

import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
 */
@javax.xml.bind.annotation.XmlRootElement
public class ApiResponseMessage {

    private final ApiResponseCodeEnum code;
    private final String message;

    public ApiResponseMessage(ApiResponseCodeEnum code, String message) {
        this.code = code;
        this.message = message;
    }

    @XmlTransient
    public ApiResponseCodeEnum getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
