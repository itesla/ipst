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
public class IpsoConstraintNodeAcAngleBounds extends IpsoConstraint<IpsoNode> {

    protected static final String ANGLE_MIN = "ANGLE_MIN";
    protected static final String ANGLE_MAX = "ANGLE_MAX";

    /**
     * Constructor
     */
    public IpsoConstraintNodeAcAngleBounds(IpsoNode ipsoNode, float angleMin, float angleMax, int world) {
        super(ipsoNode, angleMin, angleMax, world);
    }

    @Override
    protected float defaultBoundsMin() {
        return -360f;
    }

    @Override
    protected float defaultBoundsMax() {
        return 360f;
    }

    @Override
    protected float getConstrainedAttributeValueFor(IpsoNode equipment) {
        return equipment.getAngle();
    }

    @Override
    public List<String> getOrderedHeaders() {
        return Lists.newArrayList(
                NAME,
                ANGLE_MIN,
                ANGLE_MAX,
                WORLD);
    }
}
