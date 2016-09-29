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
public class IpsoBank extends IpsoOneConnectionEquipment {

    private static final String INITIAL_STEP_NUMBER = "INITIAL_STEP_NUMBER";
    private static final String MAX_STEP_NUMBER = "MAX_STEP_NUMBER";
    private static final String CONNECTED = "CONNECTED";
    private static final String NODE_NAME = "NODE_NAME";
    private static final String ACTIVE_LOSSES_BY_STEP = "ACTIVE_LOSSES_BY_STEP";
    private static final String REACTIVE_LOSSES_BY_STEP = "REACTIVE_LOSSES_BY_STEP";
    private final int maxSteps;
    private final int selectedSteps;
    private final float activePöwerByStep;
    private final float reactivePowerByStep;

    public IpsoBank(String id, String iidmId, IpsoNode connectedNode, boolean connected, int maxSteps, int selectedSteps, float activePöwerByStep, float reactivePowerByStep, int world) {
        super(id, iidmId, connectedNode, world);
        this.connected = connected;
        this.maxSteps = maxSteps;
        this.selectedSteps = selectedSteps;
        this.activePöwerByStep = activePöwerByStep;
        this.reactivePowerByStep = reactivePowerByStep;
    }

    public int getCurrentSectionCount() {
        return selectedSteps;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public int getSelectedSteps() {
        return selectedSteps;
    }

    public float getActivePöwerByStep() {
        return activePöwerByStep;
    }

    public float getReactivePowerByStep() {
        return reactivePowerByStep;
    }

    @Override
    public List<String> getOrderedHeaders() {
        return Lists.newArrayList(
                NAME,
                NODE_NAME,
                CONNECTED,
                MAX_STEP_NUMBER,
                INITIAL_STEP_NUMBER,
                ACTIVE_LOSSES_BY_STEP,
                REACTIVE_LOSSES_BY_STEP,
                WORLD);
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(
                getId(),
                getConnectedNode().getId(),
                connected,
                maxSteps,
                selectedSteps,
                replaceNanAndLogIt(ACTIVE_LOSSES_BY_STEP, activePöwerByStep, 0f),
                replaceNanAndLogIt(REACTIVE_LOSSES_BY_STEP, reactivePowerByStep, 0f),
                getWorld());
    }
}
