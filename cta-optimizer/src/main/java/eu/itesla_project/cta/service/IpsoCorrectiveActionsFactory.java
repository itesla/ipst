/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import com.google.common.collect.Lists;
import eu.itesla_project.cta.model.*;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static eu.itesla_project.cta.model.LinkOption.TAP;
import static eu.itesla_project.cta.service.IpsoControlVariableFactory.DEFAULT_SPEED_TAP;
import static eu.itesla_project.cta.service.IpsoControlVariableFactory.EPSILON;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class IpsoCorrectiveActionsFactory {

    private final IpsoProblemComponentFactory ipsoProblemComponentFactory;

    IpsoCorrectiveActionsFactory() {
        ipsoProblemComponentFactory = new IpsoProblemComponentFactory();
    }

    public <T extends IpsoProblemDefinitionElement> List<T> createProblemDefinitionElementsFor(IpsoEquipment equipment) {
        checkArgument(equipment != null, "equipment must be null");
        List<T> elements = Lists.newArrayList();

        if ( equipment instanceof IpsoTwoWindingsTransformer) {
            final IpsoTwoWindingsTransformer twoWindingsTransformer = (IpsoTwoWindingsTransformer) equipment;
            elements.add(createControlVariableForTapChanger(twoWindingsTransformer));
            elements.add(createConstraintForTapChanger(twoWindingsTransformer));
        }
        else if (equipment instanceof IpsoGenerator) {
            final IpsoGenerator generator = (IpsoGenerator)equipment;
            elements.add(createControlVariableActiveProductionFor(generator));
            elements.add(createConstraintActiveProductionFor(generator));
        }
        return elements;
    }

    private <T extends IpsoProblemDefinitionElement> T createConstraintActiveProductionFor(IpsoGenerator generator) {
        return (T)ipsoProblemComponentFactory.createConstraintProductionPBounds(generator, generator.getMinActivePower(), generator.getMaxActivePower(),0 );
    }

    private <T extends IpsoProblemDefinitionElement> T createControlVariableActiveProductionFor(IpsoGenerator generator) {
        return (T)ipsoProblemComponentFactory.createVariableProductionP(generator, LinkOption.POWER, 0.0f, 0);
    }

    private <T extends IpsoProblemDefinitionElement> T createControlVariableForTapChanger(IpsoTwoWindingsTransformer transformer) {
        return  (T) ipsoProblemComponentFactory.createVariable2WTransformerTap(
                transformer,
                TAP,
                DEFAULT_SPEED_TAP,
                EPSILON,
                0);
    }

    private <T extends IpsoProblemDefinitionElement> T createConstraintForTapChanger(IpsoTwoWindingsTransformer transformer) {
        return  (T)ipsoProblemComponentFactory.createConstraint2WTfoTap(
                transformer,
                transformer.getLowStep(),
                transformer.getHighStep(),
                0);
    }
}
