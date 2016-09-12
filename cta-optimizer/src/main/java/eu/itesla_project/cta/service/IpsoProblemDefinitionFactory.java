/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import eu.itesla_project.cta.model.IpsoNetworkState;
import eu.itesla_project.cta.model.IpsoProblemDefinition;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
final class IpsoProblemDefinitionFactory extends AbstractIpsoProblemDefinitionFactory {

    private transient final IpsoProblemComponentFactory ipsoProblemComponentFactory;

    IpsoProblemDefinitionFactory() {
        ipsoProblemComponentFactory = new IpsoProblemComponentFactory();
    }

    @Override
    public IpsoProblemDefinition createProblemDefinitionFor(IpsoNetworkState ipsoNetworkState, IpsoOptions options) {
        checkArgument(ipsoNetworkState != null, "configuration must not be null");
        checkArgument(options != null, "options must not be null");
        return createOperationalProblemDefinitionFor(ipsoNetworkState, options);
    }
}
