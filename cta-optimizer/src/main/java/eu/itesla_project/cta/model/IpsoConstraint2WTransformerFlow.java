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
public class IpsoConstraint2WTransformerFlow extends AbstractFlowConstraint<IpsoTwoWindingsTransformer> {

    /**
     * Constructor
     */
    public IpsoConstraint2WTransformerFlow(IpsoTwoWindingsTransformer transformer, FlowUnit unit, float min, float max, int world) {
        super(transformer, unit, min, max, world);
    }

    @Override
    protected float getConstrainedAttributeValueFor(IpsoTwoWindingsTransformer equipment) {
        return equipment.getCurrentFlow();
    }

    @Override
    public List<String> getOrderedHeaders() {
        return Lists.newArrayList(
                NAME,
                UNIT,
                FLOW_MIN,
                FLOW_MAX,
                WORLD);
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(
                getRelatedIpsoEquipment().getId(),
                getFlowUnit().getUnit(),
                getBoundsMin(),
                getBoundsMax(),
                getWorld());
    }
}
