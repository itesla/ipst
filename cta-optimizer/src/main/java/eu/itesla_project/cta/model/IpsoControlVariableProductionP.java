/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import com.google.common.collect.Lists;

import java.util.List;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoControlVariableProductionP extends IpsoControlVariable<IpsoGenerator> {

    private final LinkOption option;
    private final float speed;

    public IpsoControlVariableProductionP(IpsoGenerator generator, LinkOption option, float speed, int world) {
        super(generator, speed, world);
        this.option = option;
        this.speed = speed;
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(getRelatedIpsoEquipment().getId(), option.getOption(), speed, getWorld());
    }

    @Override
    public List<String> getOrderedHeaders() {
        return Lists.newArrayList(
                "NAME",
                "LINK_OPTION",
                "SPEED",
                "WORLD");
    }
}

