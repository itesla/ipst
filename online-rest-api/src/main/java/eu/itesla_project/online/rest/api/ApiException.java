/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.api;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
 */
public class ApiException extends Exception {
    private final int code;

    public int getCode() {
        return code;
    }

    public ApiException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
