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
public class IpsoNode extends IpsoEquipment {

    public static final String BASE_VOLTAGE = "BASE_VOLTAGE";
    public static final String INITIAL_VOLTAGE = "INITIAL_VOLTAGE";
    public static final String INITIAL_ANGLE = "INITIAL_ANGLE";
    public static final String ACTIVE_POWER = "ACTIVE_POWER";
    public static final String REACTIVE_POWER = "REACTIVE_POWER";
    private static final String REGION = "REGION";
    private static final String MACRO_REGION = "MACRO_REGION";
    private static final String NODE_TYPE = "NODE_TYPE";
    private final String regionName;
    private String macroRegionName;
    private final float baseVoltage;
    private float activePower;
    private final float reactivePower;
    private final float minVoltageLimit;
    private final float maxVoltageLimit;
    private final float angle;
    private final float voltage;
    private IpsoNodeType nodeType;

    /**
     * constructor
     */
    public IpsoNode(String id, String iidmId, String regionName, String macroRegionName, float baseVoltage, float voltage, float angle, IpsoNodeType nodeType,  float activePower, float reactivePower, float minVoltageLimit, float maxVoltageLimit, int world) {
        super(id, iidmId, world);
        this.regionName = regionName;
        this.macroRegionName = macroRegionName;
        this.baseVoltage = baseVoltage;
        this.nodeType = nodeType;
        this.activePower = activePower;
        this.reactivePower = reactivePower;
        this.minVoltageLimit = minVoltageLimit;
        this.maxVoltageLimit = maxVoltageLimit;
        this.voltage = voltage;
        this.angle = angle;
    }

    @Override
    public List<Object> getOrderedValues() {

        return Lists.newArrayList(
                getId(),
                regionName,
                macroRegionName, // macro-regionName ?
                replaceNanAndLogIt(BASE_VOLTAGE, baseVoltage, 0f),
                replaceNanAndLogIt(INITIAL_VOLTAGE, voltage, 0f),
                replaceNanAndLogIt(INITIAL_ANGLE, angle, 0f),
                nodeType.getValue(),
                replaceNanAndLogIt(ACTIVE_POWER, activePower, 0f),
                replaceNanAndLogIt(REACTIVE_POWER, reactivePower, 0f),
                getWorld());
    }

    @Override
    public List<String> getOrderedHeaders() {
        return Lists.newArrayList(
                NAME,
                REGION,
                MACRO_REGION,
                BASE_VOLTAGE,
                INITIAL_VOLTAGE,
                INITIAL_ANGLE,
                NODE_TYPE,
                ACTIVE_POWER,
                REACTIVE_POWER,
                WORLD);
    }


    public boolean isSlackBus() {
        return nodeType == IpsoNodeType.SB;
    }

    public float getMinVoltageLimit() {
        return minVoltageLimit;
    }

    public float getMaxVoltageLimit() {
        return maxVoltageLimit;
    }

    public float getAngle() {
        return angle;
    }

    public float getVoltage() {
        return voltage;
    }

    public void setNodeType(IpsoNodeType nodeType) {
        this.nodeType = nodeType;
    }

    public void setMacroRegionName(String macroRegionName) {
        this.macroRegionName = macroRegionName;
    }

    public String getRegionName() {
        return regionName;
    }

    public float getBaseVoltage() {
        return baseVoltage;
    }

    public float getActivePower() {
        return activePower;
    }

    public float getReactivePower() {
        return reactivePower;
    }
}


