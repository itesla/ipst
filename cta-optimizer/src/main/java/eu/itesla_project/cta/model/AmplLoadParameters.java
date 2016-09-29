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
public class AmplLoadParameters extends AmplElement{

    private final String id;
    private final String connectedNodeId;
    private final boolean initialConfigG;
    private final boolean canReconfig;
    private final float pl;
    private final float ql;
    private final float initialDL;
    private final float dlMax;
    private final float wDL;


    public AmplLoadParameters(String id, String connectedNodeId, boolean connected, boolean canReconfig,float pl, float ql, float initialDL, float dlMax, float wDL) {
        this.id = id;
        this.connectedNodeId = connectedNodeId;
        this.initialConfigG = connected;
        this.canReconfig = canReconfig;
        this.pl = pl;
        this.ql = ql;
        this.initialDL = initialDL;
        this.dlMax = dlMax;
        this.wDL = wDL;
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(
                this.id,
                this.connectedNodeId,
                this.initialConfigG,
                this.canReconfig,
                this.pl,
                this.ql,
                this.initialDL,
                this.dlMax,
                this.wDL);
    }
}
