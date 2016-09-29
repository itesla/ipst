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
public class   AmplGeneratorParameters extends AmplElement{
    private final String id;
    private final String connectedNodeId;
    private final float pgMin;
    private final float initialP;
    private final float pgMax;
    private final float qgMin;
    private final float intialQ;
    private final float qgMax;
    private final float dlgenMax;
    private final float wDlgen;
    private final boolean initialConfigG;
    private final boolean canReconfig;


    public AmplGeneratorParameters(String id, String connectedNodeId, float pgMin, float initialP, float pgMax, float qgMin, float intialQ,  float qgMax, boolean connected, boolean canReconfig,float dlgenMax, float wDlgen) {
        this.id = id;
        this.connectedNodeId = connectedNodeId;
        this.pgMin = pgMin;
        this.initialP = initialP;
        this.pgMax = pgMax;
        this.qgMin = qgMin;
        this.intialQ = intialQ;
        this.initialConfigG = connected;
        this.canReconfig = canReconfig;
        this.qgMax = qgMax;
        this.dlgenMax = dlgenMax;
        this.wDlgen = wDlgen;
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(
                this.id,
                this.connectedNodeId,
                this.pgMin,
                this.initialP,
                this.pgMax,
                this.qgMin,
                this.intialQ,
                this.qgMax,
                this.initialConfigG,
                this.canReconfig,
                this.dlgenMax,
                this.wDlgen);
    }
}
