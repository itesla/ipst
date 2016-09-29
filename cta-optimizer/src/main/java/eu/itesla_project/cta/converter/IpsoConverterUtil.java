/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.iidm.network.DanglingLine;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class IpsoConverterUtil {

    protected static final String BUS_RADIX = "_bus";
    protected static final String LOAD_RADIX = "_load";

    private IpsoConverterUtil() {
    }

    public static String getFictiveBusIdFor(DanglingLine danglingLine) {
        return new StringBuilder(danglingLine.getId()).append(BUS_RADIX).toString();

    }

    public static String getFictiveLoadIdFor(DanglingLine danglingLine) {
        return new StringBuilder(danglingLine.getId()).append(LOAD_RADIX).toString();
    }
}
