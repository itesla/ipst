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
public class IpsoConstraintGeneratorPBounds extends IpsoConstraint<IpsoGenerator> {

    protected static final String POWER_MIN = "POWER_MIN";
    protected static final String POWER_MAX = "POWER_MAX";

    /**
     * Constructor
     */
    public IpsoConstraintGeneratorPBounds(IpsoGenerator generator, float minActivePower, float maxActivePower, int world) {
        super(generator, minActivePower, maxActivePower, world);
    }

    @Override
    protected float getConstrainedAttributeValueFor(IpsoGenerator equipment) {
        return equipment.getActivePower();
    }

    @Override
    public List<String> getOrderedHeaders() {
        return Lists.newArrayList(
                NAME,
                POWER_MIN,
                POWER_MAX,
                WORLD);
    }
}
