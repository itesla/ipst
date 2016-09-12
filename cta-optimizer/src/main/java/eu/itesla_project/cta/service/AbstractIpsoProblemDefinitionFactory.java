/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import eu.itesla_project.cta.model.IpsoNetworkState;
import eu.itesla_project.cta.model.IpsoProblemDefinition;

/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
abstract class AbstractIpsoProblemDefinitionFactory {

    public abstract IpsoProblemDefinition createProblemDefinitionFor(IpsoNetworkState ipsoNetworkState, IpsoOptions options);

    /**
     * @return A problem definition from constraints and control variables
     */
    protected IpsoProblemDefinition createOperationalProblemDefinitionFor(IpsoNetworkState networkState, IpsoOptions options) {

        return IpsoProblemDefinitionBuilder
                .create(networkState.getCaseName(), networkState.getWorld(), options)
                .addVoltageBoundsConstraints(networkState)
                .addAngleBoundsConstraints(networkState)
                .addActivePowerBoundsConstraints(networkState)
                .addReactivePowerBoundsConstraints(networkState)
                .addLineFlowConstraints(networkState)
                .addBankStepBoundsConstraints(networkState)
                .addTwoWindingTransformerFowConstraints(networkState)
                .addTwoWindingTransformerTapBoundsConstraints(networkState)
                .addTwoWindingTransformerTapVariables(networkState)
                .addActiveProductionVariables(networkState)
                .addReactiveProductionVariables(networkState)
                .addStatismGeneratorVariables(networkState)
                .addBankStepVariables(networkState)
                .createIpsoProblemDefinition();
    }
}
