/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.cta.model.IpsoLoad;
import eu.itesla_project.cta.model.IpsoNode;
import eu.itesla_project.iidm.network.Bus;
import eu.itesla_project.iidm.network.Equipments;
import eu.itesla_project.iidm.network.Identifiable;
import eu.itesla_project.iidm.network.Load;

/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
abstract class AbstractLoadModelConverter<F extends Identifiable> extends AbstractModelConverter<F, IpsoLoad>{

    AbstractLoadModelConverter(ConversionContext context) {
        super(context);
    }

    protected IpsoLoad convertLoad(Load load) {

        String id = createIpsoId();

        Equipments.ConnectionInfo info = Equipments.getConnectionInfoInBusBreakerView(load.getTerminal());
        Bus bus = info.getConnectionBus();

        final IpsoNode ipsoNode = getContext().getMappingBetweenIidmIdAndIpsoEquipment().getIpsoNodeFor(bus).get();

        final float activePower = load.getTerminal().getP();
        final float reactivePower = load.getTerminal().getQ();

        return new IpsoLoad(
                id,
                load.getId(),
                ipsoNode,
                info.isConnected(),
                activePower,
                reactivePower,
                getContext().getWorld());
    }
}
