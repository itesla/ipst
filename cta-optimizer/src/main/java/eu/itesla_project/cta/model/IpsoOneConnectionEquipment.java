/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public abstract class IpsoOneConnectionEquipment extends IpsoEquipment {

    private final IpsoNode connectedNode;
    protected boolean connected;

    public IpsoOneConnectionEquipment(String id, String iidmId, IpsoNode connectedNode, int world) {
        super(id, iidmId, world);
        checkArgument(connectedNode != null, "connectedNode must not be null");
        this.connectedNode = connectedNode;
    }

    public IpsoNode getConnectedNode() {
        return connectedNode;
    }

    public boolean isConnected() {
        return connected;
    }
    public boolean couldBeConnected(List<TopologicalAction> topologicalActions) {
        return this.isConnected() || topologicalActions.stream()
                                    .filter(action -> action.getEquipmentId() == this.getId())
                                    .anyMatch(action -> action.getSwitchAction().isOppositeTo(this.isConnected()));
    }
}

