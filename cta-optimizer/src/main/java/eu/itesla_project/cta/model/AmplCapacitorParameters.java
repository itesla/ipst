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
public class AmplCapacitorParameters implements AmplWritable{


    private final String id;
    private final String connectedNode;
    private final boolean initConfig;
    private final boolean canReconfig;
    private final int stepMin;
    private final int stepMax;
    private final int initialStep;
    private final float activePowerByStep;
    private final float reactivePowerByStep;

    public AmplCapacitorParameters(String id, String cnode, boolean initConfig, boolean canReconfig, int stepMin, int stepMax, int initialStep, float activePowerByStep, float reactivePowerByStep) {
        this.id = id;
        this.connectedNode = cnode;
        this.initConfig = initConfig;
        this.canReconfig = canReconfig;
        this.stepMin = stepMin;
        this.stepMax = stepMax;
        this.initialStep = initialStep;
        this.activePowerByStep = activePowerByStep;
        this.reactivePowerByStep = reactivePowerByStep;
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(
                this.id,
                this.connectedNode,
                this.initConfig,
                this.canReconfig,
                this.stepMin,
                this.stepMax,
                this.initialStep,
                this.activePowerByStep,
                this.reactivePowerByStep
        );
    }
}
