/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
@SpringBootApplication
public class HistodbServerApplication {

    protected HistodbServerApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(HistodbServerApplication.class, args);
    }
}
