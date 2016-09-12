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
public class IpsoConstraintBankStepBounds extends IpsoConstraint<IpsoBank> {

    protected static final String STEP_MIN = "STEP_MIN";
    protected static final String STEP_MAX = "STEP_MAX";

    public IpsoConstraintBankStepBounds(IpsoBank ipsoBank, int stepMin, int stepMax, int world) {
        super(ipsoBank, stepMin, stepMax, world);
    }

    @Override
    protected float getConstrainedAttributeValueFor(IpsoBank equipment) {
        return equipment.getCurrentSectionCount();
    }

    @Override
    public List<String> getOrderedHeaders() {
        return Lists.newArrayList(
                NAME,
                STEP_MIN,
                STEP_MAX,
                WORLD);
    }
}
