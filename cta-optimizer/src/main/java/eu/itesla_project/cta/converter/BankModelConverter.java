/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.cta.model.IpsoBank;
import eu.itesla_project.cta.model.IpsoNode;
import eu.itesla_project.iidm.network.Bus;
import eu.itesla_project.iidm.network.Equipments;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.ShuntCompensator;

/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 *
 * Bank (ShuntCompensator) converter
 */
class BankModelConverter extends AbstractModelConverter<ShuntCompensator, IpsoBank>{

    BankModelConverter(ConversionContext context) {
        super(context);
    }

    @Override
    protected IpsoBank doConvert(ShuntCompensator shunt) {

        String id = createIpsoId();

        Equipments.ConnectionInfo info =
                Equipments.getConnectionInfoInBusBreakerView(shunt.getTerminal());
        Bus bus = info.getConnectionBus();
        boolean connected = info.isConnected();
        final IpsoNode ipsoNode = getContext().getMappingBetweenIidmIdAndIpsoEquipment().getIpsoNodeFor(bus).get();

        int maxSteps = shunt.getMaximumSectionCount();
        //...number of steps in service
        int selectedSteps = shunt.getCurrentSectionCount();

        //...no active lost in the iidm shunt compensator
        float activePöwerByStep = 0.f;
        //...getValue vnom on the connected node
        float vnom = shunt.getTerminal().getVoltageLevel().getNominalV();
        //...reactive power of each step [Mvar]
        float reactivePowerByStep = vnom*vnom * shunt.getMaximumB()
                                              / shunt.getMaximumSectionCount();

        return new IpsoBank(id,
                shunt.getId(),
                ipsoNode,
                connected,
                maxSteps,
                selectedSteps,
                activePöwerByStep,
                reactivePowerByStep,
                getContext().getWorld());
    }

    @Override
    public Iterable<ShuntCompensator> gatherDataToConvertFrom(Network network) {
        return network.getShunts();
    }

    @Override
    protected ComponentType getComponentType() {
        return ComponentType.BANK;
    }
}
