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
public class IpsoConstraintNodeAcVoltageBounds extends IpsoConstraint<IpsoNode> {

    protected static final String VOLTAGE_MIN = "VOLTAGE_MIN";
    protected static final String VOLTAGE_MAX = "VOLTAGE_MAX";
    public static final float DEFAULT_BOUND_MIN = 0f;
    public static final float DEFAULT_BOUND_MAX = 1.5f;
    private final VoltageUnit unit;

    public IpsoConstraintNodeAcVoltageBounds(IpsoNode ipsoNode, VoltageUnit unit, float minVoltage, float maxVoltage, int world) {
        super(ipsoNode, minVoltage, maxVoltage, world);
        this.unit = unit;
    }

    /**
     * @return  node voltage in pu
     */
    @Override
    protected float getConstrainedAttributeValueFor(IpsoNode equipment) {
        return equipment.getVoltage() / equipment.getBaseVoltage();
    }

    @Override
    protected float defaultBoundsMin() {
        return DEFAULT_BOUND_MIN;
    }

    @Override
    protected float defaultBoundsMax() {
        return DEFAULT_BOUND_MAX;
    }

    @Override
    public List<String> getOrderedHeaders() {
        return Lists.newArrayList(
                NAME,
                UNIT,
                VOLTAGE_MIN,
                VOLTAGE_MAX,
                WORLD);
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(
                getRelatedIpsoEquipment().getId(),
                unit.getUnit(),
                DataUtil.replaceNanByEmpty(getBoundsMin()),
                DataUtil.replaceNanByEmpty(getBoundsMax()),
                getWorld());
    }
}
