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
public class IpsoConstraint2WTransformerTapBounds extends IpsoConstraint<IpsoTwoWindingsTransformer> {

    protected static final String TAP_MIN = "TAP_MIN";
    protected static final String TAP_MAX = "TAP_MAX";

    /**
     * Constructor
     */
    public IpsoConstraint2WTransformerTapBounds(IpsoTwoWindingsTransformer transformer, int tapMin, int tapMax, int world) {
        super(transformer, tapMin, tapMax, world);
    }

    @Override
    protected float getConstrainedAttributeValueFor(IpsoTwoWindingsTransformer equipment) {
        return equipment.getRegulationParameters().getCurrentStepPosition();
    }

    @Override
    public List<String> getOrderedHeaders() {
        return Lists.newArrayList(
                NAME,
                TAP_MIN,
                TAP_MAX,
                WORLD);
    }
}
