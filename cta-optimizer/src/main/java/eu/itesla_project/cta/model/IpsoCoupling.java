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
public class IpsoCoupling extends AbstractIpsoBranch{

    private static final String NODE_NAME_1 = "NODE_NAME_1";
    private static final String NODE_NAME_2 = "NODE_NAME_2";
    private static final String CONNECTION_STATUS = "CONNECTION_STATUS";
    private final boolean connected;

    /**
     * constructor
     */
    public IpsoCoupling(String id, String iidmId,IpsoNode ipsoNode1, IpsoNode ipsoNode2, boolean connected, int world) {
        super(id, iidmId, world, ipsoNode1, ipsoNode2);
        this.connected = connected;
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(getId(), ipsoNode1.getId(), ipsoNode2.getId(), getConnectionCodeFor(connected), getWorld());
    }

    @Override
    public List<String> getOrderedHeaders() {
        return Lists.newArrayList(
                NAME,
                NODE_NAME_1,
                NODE_NAME_2,
                CONNECTION_STATUS,
                WORLD);
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

}
