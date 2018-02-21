/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.security.rest.api;

import java.util.Collections;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import eu.itesla_project.security.rest.api.impl.utils.Utils;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.it>
 */
@ApplicationPath("/api")
public class RestApplication extends Application {

    public RestApplication() {
        super();
        Utils.init();
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Collections.singleton(SecurityAnalysisApi.class);
    }

}
