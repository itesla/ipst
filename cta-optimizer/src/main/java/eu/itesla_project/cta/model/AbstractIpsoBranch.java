/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import java.util.List;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public abstract class AbstractIpsoBranch extends IpsoEquipment{

    protected final IpsoNode ipsoNode1;
    protected final IpsoNode ipsoNode2;

    public AbstractIpsoBranch(String id, String iidmId, int world, IpsoNode ipsoNode1, IpsoNode ipsoNode2) {
        super(id, iidmId, world);
        this.ipsoNode1 = ipsoNode1;
        this.ipsoNode2 = ipsoNode2;
    }

    public abstract boolean isConnected();

    public boolean couldBeConnected(List<TopologicalAction> topologicalActions) {
        return this.isConnected() || topologicalActions.stream()
                                    .filter(action -> action.getEquipmentId() == this.getId())
                                    .anyMatch(action -> action.getSwitchAction().isOppositeTo(this.isConnected()));
    }
    protected char getConnectionCodeFor(boolean connected) {
        return connected ? 'Y' : 'N';
    }

    public IpsoNode getIpsoNode2() {
        return ipsoNode2;
    }

    public IpsoNode getIpsoNode1() {
        return ipsoNode1;
    }

}
