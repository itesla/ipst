/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.io;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.joining;
/**
 * Copyright (c) 2016, Tractebel (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class AmplParameterFormatter {

    private static final String DELIMITER = "\t";

    public String format(List<?> list) {
        checkArgument(list != null, "list must not be null");
        return list.stream()
                .map(e -> convertBooleanToInteger(e))
                .map(Object::toString)
                .collect(joining(DELIMITER));
    }

    private Object convertBooleanToInteger(Object e) {
        if(e instanceof Boolean ) {
            return (boolean)e ? 1 : 0;
        }
        else if(e instanceof Float && (float)e == Float.NEGATIVE_INFINITY) {
            return "-Infinity";
        }
        else if(e instanceof Float && (float)e == Float.POSITIVE_INFINITY) {
            return "Infinity";
        }
        else {
            return e;
        }
    }
}
