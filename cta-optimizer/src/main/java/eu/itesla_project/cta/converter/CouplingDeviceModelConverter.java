/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.commons.ITeslaException;
import eu.itesla_project.cta.model.IpsoCoupling;
import eu.itesla_project.cta.model.IpsoNode;
import eu.itesla_project.iidm.network.Bus;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.Switch;
import eu.itesla_project.iidm.network.VoltageLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class CouplingDeviceModelConverter extends AbstractModelConverter<Switch, IpsoCoupling>{

    final class ConnectionBuses {
         private Bus from;
         private Bus to;
         public ConnectionBuses(Bus from, Bus to) {
             this.from = from;
             this.to = to;
         }

        public Bus getFrom() {
            return from;
        }

        public Bus getTo() {
            return to;
        }
    }

    private Map<String,ConnectionBuses> swtichConnectivities = new HashMap<>();

    CouplingDeviceModelConverter(ConversionContext context) {
        super(context);
    }

    @Override
    public Iterable<Switch> gatherDataToConvertFrom(Network network) {
        List<Switch> switches = new ArrayList<>();
        for (VoltageLevel vl : network.getVoltageLevels()) {
            for (Switch s : vl.getBusBreakerView().getSwitches()) {
                Bus b1 = vl.getBusBreakerView().getBus1(s.getId());
                Bus b2 = vl.getBusBreakerView().getBus2(s.getId());
                swtichConnectivities.put(s.getId(), new ConnectionBuses(b1, b2));
                switches.add(s);
            }
        }
        return switches;
    }

    @Override
    protected ComponentType getComponentType() {
        return ComponentType.COUPLING;
    }

    @Override
    protected IpsoCoupling doConvert(Switch iidmSwitch) {
        // getValue connection buses
        ConnectionBuses buses = swtichConnectivities.get(iidmSwitch.getId());
        if (buses == null) throw new ITeslaException(String.format("No connectivies found for %s", iidmSwitch.getId()));

        final IpsoNode ipsoNode1 = getContext().getMappingBetweenIidmIdAndIpsoEquipment().getIpsoNodeFor(buses.getFrom()).get();
        final IpsoNode ipsoNode2 = getContext().getMappingBetweenIidmIdAndIpsoEquipment().getIpsoNodeFor(buses.getTo()).get();
        String id = createIdFrom(ipsoNode1.getId(), ipsoNode2.getId());

        final boolean connected = !iidmSwitch.isOpen();
        return new IpsoCoupling(id, iidmSwitch.getId(), ipsoNode1, ipsoNode2, connected, getContext().getWorld());
    }

}
