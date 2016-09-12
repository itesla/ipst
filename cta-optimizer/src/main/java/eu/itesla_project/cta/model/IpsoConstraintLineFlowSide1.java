/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoConstraintLineFlowSide1 extends AbstractIpsoConstraintLineFlow {

    public IpsoConstraintLineFlowSide1(IpsoLine line, FlowUnit unit, float min, float max, int world) {
        super(line, unit, min, max, world);
    }

    @Override
    protected float getConstrainedAttributeValueFor(IpsoLine equipment) {
        return Math.max(equipment.getCurrentFlowSide1(), equipment.getCurrentFlowSide2());
    }

}
