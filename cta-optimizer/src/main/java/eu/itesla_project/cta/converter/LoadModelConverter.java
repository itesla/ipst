/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.cta.model.IpsoLoad;
import eu.itesla_project.iidm.network.Load;
import eu.itesla_project.iidm.network.Network;

import static com.google.common.base.Preconditions.checkArgument;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class LoadModelConverter extends AbstractLoadModelConverter<Load>{

    LoadModelConverter(ConversionContext context) {
        super(context);
    }


    @Override
    public IpsoLoad doConvert(Load load) {
        checkArgument(load != null, "load must not be null");
        return convertLoad(load);
    }

    @Override
    public Iterable<Load> gatherDataToConvertFrom(Network network) {
        return network.getLoads();
    }

    @Override
    protected ComponentType getComponentType() {
        return ComponentType.LOAD;
    }
}
