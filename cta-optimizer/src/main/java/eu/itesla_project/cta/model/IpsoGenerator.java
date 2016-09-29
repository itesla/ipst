/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import com.google.common.collect.Lists;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoGenerator extends IpsoOneConnectionEquipment {
    protected static final float DELTA_Q_EPSILON = 0.1f;

    protected static final String NODE_NAME = "NODE_NAME";
    protected static final String CONNECTED = "CONNECTED";
    protected static final String INITIAL_ACTIVE_POWER = "INITIAL_ACTIVE_POWER";
    protected static final String INITIAL_REACTIVE_POWER = "INITIAL_REACTIVE_POWER";
    protected static final String MIN_ACTIVE_POWER = "MIN_ACTIVE_POWER";
    protected static final String MAX_ACTIVE_POWER = "MAX_ACTIVE_POWER";
    protected static final String MIN_REACTIVE_POWER = "MIN_REACTIVE_POWER";
    protected static final String MAX_REACTIVE_POWER = "MAX_REACTIVE_POWER";
    protected static final String NOMINAL_POWER = "NOMINAL_POWER";
    protected static final String NOMINAL_ACTIVE_POWER = "NOMINAL_ACTIVE_POWER";
    protected static final String NOMINAL_REACTIVE_POWER = "NOMINAL_REACTIVE_POWER";
    protected static final String NOMINAL_VOLTAGE = "NOMINAL_VOLTAGE";
    private final boolean regulating;
    private final float minActivePower;
    private final float maxActivePower;
    private final float minReactivePower;
    private final float maxReactivePower;
    private final float nominalPower;
    private final float activePower;
    private final float reactivePower;
    private final float nominalVoltage;
    private final float setpointVoltage;
    private final float deltaQ;
    private boolean biggest;  // biggest active power

    /**
     * Constructor
     */
    public IpsoGenerator(String id,
                         String iidmId,
                         IpsoNode connectedNode,
                         boolean connected,
                         boolean regulating,
                         float minActivePower,
                         float maxActivePower,
                         float minReactivePower,
                         float maxReactivePower,
                         float nominalPower,
                         float activePower,
                         float reactivePower,
                         float nominalVoltage,
                         float setpointVoltage,
                         int world) {
        super(id, iidmId, connectedNode, world);
        checkArgument(connectedNode != null, "connectedNode must not be null");
        this.regulating = regulating;
        this.connected = connected;
        this.minActivePower = minActivePower;
        this.maxActivePower = maxActivePower;
        this.minReactivePower = minReactivePower;
        this.maxReactivePower = maxReactivePower;
        this.nominalPower = nominalPower;
        this.activePower = activePower;
        this.reactivePower = reactivePower;
        this.nominalVoltage = nominalVoltage;
        this.setpointVoltage = setpointVoltage;
        this.deltaQ = maxReactivePower - minReactivePower;
    }

    public boolean isRegulating() {
        return regulating;
    }

    public void setBiggest(boolean biggest) {
        this.biggest = biggest;
    }

    /**
     *
     * @return true if the generator is the biggest one
     * Remark: The biggest generator is always connected to a node
     */
    public boolean isBiggest() {
        return biggest;
    }

    /**
     * @return true iff Qmax-Qmin  > DELTAT_Q_EPSILON
     */
    public boolean isDeltaQLimitUpperThanOneMvar() {
        return Math.abs(maxReactivePower - minReactivePower) > DELTA_Q_EPSILON;
    }

    /**
     * @return true iff Qmax-Qmin  < DELTAT_Q_EPSILON
     */
    public boolean isDeltaQLimitLowerThanOneMvar() {
        return Math.abs(deltaQ) < DELTA_Q_EPSILON;
    }

    public float getDeltaQ() {
        return deltaQ;
    }

    public float getMinActivePower() {return minActivePower;}

    public float getMaxActivePower() {return maxActivePower;}

    public float getMinReactivePower() {return minReactivePower;}

    public float getMaxReactivePower() {return maxReactivePower;}


    /**
     * @return the voltage setpoint in pu
     */
    public float getVoltageSetpointPu() {
        if (Float.isNaN(setpointVoltage) ||
                Float.isNaN(nominalVoltage) ||
                nominalVoltage == 0f) {
            return Float.NaN;
        }
        else {
            return setpointVoltage / nominalVoltage;
        }
    }


    public float getActivePower() {
        return activePower;
    }

    public float getReactivePower() {
        return reactivePower;
    }

    private char connectionStatusOf(boolean connected) {
        return connected ? 'Y' : 'N';
    }

    @Override
    public List<String> getOrderedHeaders() {
        return Lists.newArrayList(
                NAME,
                NODE_NAME,
                CONNECTED,
                INITIAL_ACTIVE_POWER,
                INITIAL_REACTIVE_POWER,
                MIN_ACTIVE_POWER,
                MAX_ACTIVE_POWER,
                MIN_REACTIVE_POWER,
                MAX_REACTIVE_POWER,
                NOMINAL_POWER,
                NOMINAL_ACTIVE_POWER,
                NOMINAL_REACTIVE_POWER,
                NOMINAL_VOLTAGE,
                "BVD", // not used
                "XMQ", // not used
                "SETPOINT_VOLTAGE",
                "CALVAER_VOLTAGE", // not used
                "CAPABILITY_CURVE_QMIN_PMIN", // not used
                "CAPABILITY_CURVE_QMIN_PMAX", // not used
                "CAPABILITY_CURVE_XCR", // not used
                "GENERATOR_TYPE", // not used
                "NUMBER_OF_LINKED_GENERATORS", // not used
                "NAMES_OF_LINKED_GENERATORS", // will be a string
                "DROOP_COEFFICIENT", // not used
                "WORLD");
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(
                getId(),
                getConnectedNode().getId(),
                connectionStatusOf(connected),
                replaceNanAndLogIt(INITIAL_ACTIVE_POWER, activePower,0f),
                replaceNanAndLogIt(INITIAL_REACTIVE_POWER, reactivePower,0f),
                replaceNanAndLogIt(MIN_ACTIVE_POWER, minActivePower,0f),
                replaceNanAndLogIt(MAX_ACTIVE_POWER, maxActivePower,0f),
                replaceNanAndLogIt(MIN_REACTIVE_POWER, minReactivePower,0f),
                replaceNanAndLogIt(MAX_REACTIVE_POWER, maxReactivePower,0f),
                replaceNanAndLogIt(NOMINAL_POWER, nominalPower,0f),
                replaceNanAndLogIt(NOMINAL_ACTIVE_POWER, activePower,0f),
                replaceNanAndLogIt(NOMINAL_REACTIVE_POWER, reactivePower,0f),
                replaceNanAndLogIt(NOMINAL_VOLTAGE, nominalVoltage,0f),
                null, // BVD
                null, // XMQ
                DataUtil.getSafeValueOf(setpointVoltage),
                null, // "CALVAER_VOLTAGE"
                null, // CAPABILITY_CURVE_QMIN_PMIN"  not used
                null, // CAPABILITY_CURVE_QMIN_PMAX"  not used
                null, // CAPABILITY_CURVE_XCR"  not used
                null, // GENERATOR_TYPE"  not used
                null, // NUMBER_OF_LINKED_GENERATORS"  not used
                null, // NAMES_OF_LINKED_GENERATORS"  will be a string
                null, // DROOP_COEFFICIENT"  not used
                getWorld());
    }
}
