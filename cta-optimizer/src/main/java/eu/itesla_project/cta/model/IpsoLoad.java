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
public class IpsoLoad extends IpsoOneConnectionEquipment {

    protected static final String NODE_NAME = "NODE_NAME";
    protected static final String CONNECTED = "CONNECTED";
    protected static final String INITIAL_ACTIVE_POWER = "INITIAL_ACTIVE_POWER";
    protected static final String INITIAL_REACTIVE_POWER = "INITIAL_REACTIVE_POWER";
    private final float activePower;
    private final float reactivePower;

    public IpsoLoad(String id, String iidmId,IpsoNode connectedNode, boolean connected, float activePower, float reactivePower, int world) {
        super(id, iidmId, connectedNode, world);
        checkArgument(connectedNode != null, "connectedNode must not be null");
        this.connected = connected;
        this.activePower = activePower;
        this.reactivePower = reactivePower;
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
                WORLD);
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(
                getId(),
                getConnectedNode().getId(),
                connectionStatusOf(connected),
                replaceNanAndLogIt(INITIAL_ACTIVE_POWER, activePower, 0f),
                replaceNanAndLogIt(INITIAL_REACTIVE_POWER, reactivePower, 0f),
                getWorld());
    }

    public float getActivePower() {
        return activePower;
    }

    public float getReactivePower() {
        return reactivePower;
    }
}
