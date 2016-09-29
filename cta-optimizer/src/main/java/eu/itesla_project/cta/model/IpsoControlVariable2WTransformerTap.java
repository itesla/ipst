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
public class IpsoControlVariable2WTransformerTap extends IpsoControlVariable<IpsoTwoWindingsTransformer> {

    private final LinkOption linkOption;
    private final float speed;
    private final float epsilon;

    /**
     * Constructor
     */
    public IpsoControlVariable2WTransformerTap(IpsoTwoWindingsTransformer transformer, LinkOption linkOption, float speed, float epsilon, int world) {
        super(transformer, speed, world);
        this.linkOption = linkOption;
        this.speed = speed;
        this.epsilon = epsilon;
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(
                getRelatedIpsoEquipment().getId(),
                linkOption.getOption(),
                speed,
                epsilon,
                getWorld());
    }

    @Override
    public List<String> getOrderedHeaders() {
        return Lists.newArrayList(
                "NAME",
                "LINK_OPTION",
                "SPEED",
                "EPSILON",
                "WORLD");
    }
}
