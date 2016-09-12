/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import com.google.common.collect.Lists;
import eu.itesla_project.cta.model.IpsoComponent;
import eu.itesla_project.cta.model.IpsoGenerator;
import eu.itesla_project.cta.model.IpsoNetworkState;
import eu.itesla_project.cta.model.ValidationType;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoInvalidIpsoComponentFactory {

    public List<IpsoInvalidComponent> createInvalidComponentsThatHave(ValidationType validationType, Class<? extends IpsoComponent> ipsoComponentClass, IpsoNetworkState networkState) {
        checkArgument(networkState != null, "networkState must not be null");
        checkArgument(ipsoComponentClass != null, "ipsoComponentClass must not be null");

        switch (validationType) {
            case DELTA_P_LIMIT_LOWER_THAN_ONE_MVAR:
                return Lists.newArrayList(); // not used
            case DELTA_Q_LIMIT_LOWER_THAN_ONE_MVAR:
                return  createInvalidComponentForQLimits(validationType, ipsoComponentClass, networkState);
            default:
                throw new IllegalArgumentException("unsupported ValidationType");
        }
    }

    private List<IpsoInvalidComponent> createInvalidComponentForQLimits(ValidationType validationType, Class<? extends IpsoComponent> ipsoComponentClass, IpsoNetworkState networkState) {
        return networkState.getConnectedAndRegulatingGenerators()
                .filter(IpsoGenerator::isDeltaQLimitLowerThanOneMvar)
                .map(generator -> new IpsoInvalidComponent(generator, validationType, ipsoComponentClass, generator.getDeltaQ()))
                .collect(toList());
    }
}
