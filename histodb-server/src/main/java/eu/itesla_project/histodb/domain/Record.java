/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb.domain;

import java.util.List;
import java.util.Objects;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class Record {

    private final List<Value> values;

    public Record(List<Value> values) {
        this.values = Objects.requireNonNull(values);
    }

    public List<Value> getValues() {
        return values;
    }

}
