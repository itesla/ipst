/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.api.factories;

import eu.itesla_project.online.db.OnlineDbMVStoreFactory;
import eu.itesla_project.online.rest.api.ProcessApiService;
import eu.itesla_project.online.rest.api.impl.ProcessApiServiceImpl;
import eu.itesla_project.online.rest.api.util.OnlineDBUtils;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
 */
public final class ProcessApiServiceFactory {

    private final static ProcessApiService service = new ProcessApiServiceImpl(
            new OnlineDBUtils(new OnlineDbMVStoreFactory()));

    public static ProcessApiService getProcessApi() {
        return service;
    }
}
