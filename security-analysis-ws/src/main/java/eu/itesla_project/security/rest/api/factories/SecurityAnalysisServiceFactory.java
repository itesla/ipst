/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.security.rest.api.factories;

import eu.itesla_project.security.rest.api.SecurityAnalysisService;
import eu.itesla_project.security.rest.api.impl.SecurityAnalysisServiceImpl;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.it>
 */
public final class SecurityAnalysisServiceFactory {

    private SecurityAnalysisServiceFactory() {
    }

    private static final SecurityAnalysisService SERVICE = new SecurityAnalysisServiceImpl();

    public static SecurityAnalysisService getSecurityServiceApi() {
        return SERVICE;
    }
}
