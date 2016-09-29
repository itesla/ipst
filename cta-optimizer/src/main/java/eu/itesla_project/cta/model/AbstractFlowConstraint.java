/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public abstract class AbstractFlowConstraint<T extends IpsoEquipment> extends IpsoConstraint<T> {

    protected static final String FLOW_MIN = "FLOW_MIN";
    protected static final String FLOW_MAX = "FLOW_MAX";
    private final FlowUnit flowUnit;

    public AbstractFlowConstraint(T equipment, FlowUnit flowUnit ,float min, float max, int world) {
        super(equipment, min, max, world);
        this.flowUnit = flowUnit;
    }

    public float getMaxFlow() {
        return getBoundsMax();
    }

    public float getMinFlow() {
        return getBoundsMin();
    }

    public FlowUnit getFlowUnit() {
        return flowUnit;
    }
}
