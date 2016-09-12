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
public class AmplBranchParameters extends AmplElement{
    private final String id;
    private final String or;
    private final String de;
    private final float y;
    private final float zeta;
    private final float gSh;
    private final float bSh;
    private final boolean initConfig;
    private final boolean canReconfig;
    private final float pMaxFlow;
    private final float qMaxFlow;
    private final float sMaxFlow;
    private final float iMaxFlowOr;
    private final float iMaxFlowDe;

    public AmplBranchParameters(String id, String or, String de, float y, float zeta, float gSh, float bSh, boolean initConfig, boolean canReconfig, float pMaxFlow, float qMaxFlow, float sMaxFlow, float iMaxFlowOr, float iMaxFlowDe) {
        this.id = id;
        this.or = or;
        this.de = de;
        this.y = y;
        this.zeta = zeta;
        this.gSh = gSh;
        this.bSh = bSh;
        this.initConfig = initConfig;
        this.canReconfig = canReconfig;
        this.pMaxFlow = pMaxFlow;
        this.qMaxFlow = qMaxFlow;
        this.sMaxFlow = sMaxFlow;
        this.iMaxFlowOr = iMaxFlowOr;
        this.iMaxFlowDe = iMaxFlowDe;
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(this.id,
                this.or,
                this.de,
                this.y,
                this.zeta,
                this.gSh,
                this.bSh,
                this.initConfig,
                this.canReconfig,
                this.pMaxFlow,
                this.qMaxFlow,
                this.sMaxFlow,
                this.iMaxFlowOr,
                this.iMaxFlowDe);
    }
}
