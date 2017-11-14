/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.ddb.modelica_imp_exp.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

/**
 * @author Luis Maria Zamarreno <zamarrenolm@aia.com>
 * @author Silvia Machado <machados@aia.es>
 */
public final class Utils {

    private Utils() {
    }

    public static byte[] stringAsByteArrayUTF8(String par) {
        try {
            return par.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
            return new byte[] {};
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
}
