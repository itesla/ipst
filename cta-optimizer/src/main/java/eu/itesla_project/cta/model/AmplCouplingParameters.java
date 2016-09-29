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
public class AmplCouplingParameters implements AmplWritable{


    final String id;
    final String ipsoNode1;
    final String ipsoNode2;

    final boolean connected;

    final boolean canReconfig;

    public AmplCouplingParameters(String id, String ipsoNode1, String ipsoNode2, boolean connected, boolean canReconfig) {
        this.id = id;
        this.ipsoNode1 = ipsoNode1;
        this.ipsoNode2 = ipsoNode2;
        this.connected = connected;
        this.canReconfig = canReconfig;
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(
                this.id,
                this.ipsoNode1,
                this.ipsoNode2,
                this.connected,
                this.canReconfig
        );
    }
}
