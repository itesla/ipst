/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import com.google.common.collect.Iterables;
import eu.itesla_project.commons.ITeslaException;
import eu.itesla_project.cta.model.DataUtil;
import eu.itesla_project.cta.model.IpsoLine;
import eu.itesla_project.cta.model.IpsoNode;
import eu.itesla_project.iidm.network.Bus;
import eu.itesla_project.iidm.network.DanglingLine;
import eu.itesla_project.iidm.network.Equipments;
import eu.itesla_project.iidm.network.Network;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class DanglingLineModelConverter extends AbstractModelConverter<DanglingLine, IpsoLine> {

    protected DanglingLineModelConverter(ConversionContext context) {
        super(context);
    }

    /**
     * @return ipso danglingLine
     */
    @Override
    public IpsoLine doConvert(DanglingLine danglingLine) {
        checkArgument(danglingLine != null, "danglingLine must not be null");
        checkArgument(danglingLine.getTerminal() != null, "danglingLine.getTerminal() must not be null");

        Equipments.ConnectionInfo info1 = Equipments.getConnectionInfoInBusBreakerView(danglingLine.getTerminal());

        Bus bus1 = info1.getConnectionBus();

        Optional<IpsoNode> ipsoNodeFrom = getMapping().getIpsoNodeFor(bus1);
        Optional<IpsoNode> ipsoNodeTo = getMapping().getIpsoNodeFor(getFictiveBusIdFor(danglingLine));

        if (ipsoNodeFrom.isPresent() && ipsoNodeTo.isPresent()) {
            IpsoNode ipsoNode1 = ipsoNodeFrom.get();
            IpsoNode ipsoNode2 = ipsoNodeTo.get();

            String id = createIdFrom(ipsoNode1.getId(), ipsoNode2.getId());

            float vnom = getVnom(danglingLine);
            final float vnomPow2 = (float) Math.pow(vnom, 2);
            float Rpu = (danglingLine.getR() * snref()) / vnomPow2;  //...total danglingLine resistance  [p.u.]
            float Xpu = (danglingLine.getX() * snref()) / vnomPow2;  //...total danglingLine reactance   [p.u.]
            float Gpu = (danglingLine.getG() / snref()) * vnomPow2; //...semi shunt conductance [p.u.]
            float Bpu = (danglingLine.getB() / snref()) * vnomPow2; //...semi shunt susceptance [p.u.]

            // x 100 to createAmplModelFrom pu to percent of pu, in order to be compliant with PSA format (used by IPSO)
            Rpu = Rpu * 100.0f;
            Xpu = Xpu * 100.0f;
            Gpu = Gpu * 100.0f;
            Bpu = Bpu * 100.0f;

            final int world = getContext().getWorld();
            final float currentFlow = danglingLine.getTerminal().getI();
            return new IpsoLine(
                    id,
                    danglingLine.getId(),
                    ipsoNode1,
                    ipsoNode2,
                    info1.isConnected(),
                    true,
                    Rpu,
                    Xpu,
                    Gpu,
                    Bpu,
                    findMaxCurrentPermanentLimitFor(danglingLine),
                    currentFlow,
                    currentFlow,
                    world);
        } else {
            return null;
        }
    }

    private float findMaxCurrentPermanentLimitFor(DanglingLine danglingLine) {
        if (danglingLine.getCurrentLimits() != null) {
            return danglingLine.getCurrentLimits().getPermanentLimit();
        }
        else {
            return Float.NaN;
        }
    }

    /**
     * @return Nominal Voltage [Kv]
     */
    private static float getVnom( DanglingLine danglingLine ) {
        Iterable<Bus> buses = danglingLine.getTerminal().getVoltageLevel().getBusBreakerView().getBuses();
        final Bus bus = Iterables.getFirst(buses, null);
        if ( bus == null ) throw new ITeslaException("getVnom cannot get bus of the same voltage level");
        return DataUtil.getSafeValueOf(bus.getVoltageLevel().getNominalV());
    }

    @Override
    protected ComponentType getComponentType() {
        return ComponentType.DANGLING_LINE;
    }

    @Override
    public Iterable<DanglingLine> gatherDataToConvertFrom(Network network) {
        return network.getDanglingLines();
    }

    private String getFictiveBusIdFor(DanglingLine danglingLine) {
        return IpsoConverterUtil.getFictiveBusIdFor(danglingLine);
    }

    private MappingBetweenIidmIdAndIpsoEquipment getMapping() {
        return getContext().getMappingBetweenIidmIdAndIpsoEquipment();
    }
}
