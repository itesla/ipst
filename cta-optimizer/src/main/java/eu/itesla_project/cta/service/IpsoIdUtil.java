/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import eu.itesla_project.cta.model.IpsoEquipment;

import java.util.concurrent.atomic.AtomicLong;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoIdUtil {

    private static AtomicLong atomicLong = new AtomicLong(1);

    public static <T extends IpsoEquipment> String getNextUniqueId() {
        return String.valueOf(atomicLong.getAndIncrement());
    }
}
