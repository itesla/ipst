/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.cta.model.IpsoLoad;
import eu.itesla_project.cta.model.IpsoNode;
import eu.itesla_project.iidm.network.DanglingLine;
import eu.itesla_project.iidm.network.Network;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class XloadModelConverter extends AbstractLoadModelConverter<DanglingLine>{

    XloadModelConverter(ConversionContext context) {
        super(context);
    }


    @Override
    public IpsoLoad doConvert(DanglingLine danglingLine) {
        checkArgument(danglingLine != null, "danglingLine must not be null");

        String id = createIpsoId();
        Optional<IpsoNode> ipsoNodeFor = getContext().getMappingBetweenIidmIdAndIpsoEquipment().getIpsoNodeFor(IpsoConverterUtil.getFictiveBusIdFor(danglingLine));

        if(ipsoNodeFor.isPresent()) {
            IpsoNode ipsoNode = ipsoNodeFor.get();

            float activePower = danglingLine.getP0();
            float reactivePower = danglingLine.getQ0();
            return new IpsoLoad(
                    id,
                    getIdOf(danglingLine),
                    ipsoNode,
                    true,
                    activePower,
                    reactivePower,
                    getContext().getWorld());
        }
        else {
            return null;
        }
    }

    @Override
    public Iterable<DanglingLine> gatherDataToConvertFrom(Network network) {
        return network.getDanglingLines();
    }

    @Override
    protected ComponentType getComponentType() {
        return ComponentType.XLOAD;
    }

    /**
     * @return A fake id for fictive node created from danglingLine
     */
    @Override
    protected String getIdOf(DanglingLine danglingLine) {
        return IpsoConverterUtil.getFictiveLoadIdFor(danglingLine);
    }
}
