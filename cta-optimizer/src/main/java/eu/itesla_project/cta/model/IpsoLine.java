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
public class IpsoLine extends AbstractIpsoBranch {

    private final boolean connectedSide1;
    private final boolean connectedSide2;
    private final float maxCurrentPermanentLimit;
    private final float currentFlowSide1;
    private final float rpu;
    private final float xpu;
    private final float gpu;
    private final float bpu;
    private final float currentFlowSide2;

    public IpsoLine(String id, String iidmId, IpsoNode ipsoNode1, IpsoNode ipsoNode2, boolean connectedSide1, boolean connectedSide2, float rpu, float xpu, float gpu, float bpu, float maxCurrentPermanentLimit, float currentFlowSide1, float currentFlowSide2, int world) {
        super(id, iidmId, world, ipsoNode1, ipsoNode2);
        checkArgument(ipsoNode1 != null, "ipsoNode1 must not be null");
        checkArgument(ipsoNode2 != null, "ipsoNode2 must not be null");
        this.connectedSide1 = connectedSide1;
        this.connectedSide2 = connectedSide2;
        this.rpu = rpu;
        this.xpu = xpu;
        this.gpu = gpu;
        this.bpu = bpu;
        this.maxCurrentPermanentLimit = maxCurrentPermanentLimit;
        this.currentFlowSide1 = currentFlowSide1;
        this.currentFlowSide2 = currentFlowSide2;
    }

    public float getMaxCurrentPermanentLimit() {
        return maxCurrentPermanentLimit;
    }

    public boolean hasMaxCurrentPermanentLimitDefined() {
        return !Float.isNaN(maxCurrentPermanentLimit);
    }

    public float getCurrentFlowSide1() {
        return currentFlowSide1;
    }

    public float getCurrentFlowSide2() {
        return currentFlowSide2;
    }

    public float getResistance() { return rpu; }

    public float getReactance() { return xpu; }

    public float getSemiConductance() { return gpu; }

    public float getSemiSusceptance() { return bpu; }

    private char connectionStatusOf(boolean connected) {
        return connected ? 'Y' : 'N';
    }

    @Override
    public List<String> getOrderedHeaders() {
        return Lists.newArrayList(
                "NAME",
                "NODE_NAME_1",
                "NODE_NAME_2",
                "CONNECTED_SIDE_1",
                "CONNECTED_SIDE_2",
                "RESISTANCE",
                "REACTANCE",
                "SEMI_CONDUCTANCE",
                "SEMI_SUSCEPTANCE",
                "WORLD");
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(
                getId(),
                ipsoNode1.getId(),
                ipsoNode2.getId(),
                connectionStatusOf(connectedSide1),
                connectionStatusOf(connectedSide2),
                rpu,
                xpu,
                gpu,
                bpu,
                getWorld());
    }

    @Override
    public boolean isConnected() {
        return connectedSide1 && connectedSide2;
    }

    @Override
    protected char getConnectionCodeFor(boolean connected) {
        return connected ? 'Y' : 'N';
    }
}
