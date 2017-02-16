/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.api;

import java.util.Collections;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
 */
@ApplicationPath("/")
public class RestApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Collections.singleton(ProcessApi.class);
    }

}
