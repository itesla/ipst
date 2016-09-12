/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.commons.ITeslaException;
import eu.itesla_project.cta.model.DataUtil;
import eu.itesla_project.cta.model.IpsoLine;
import eu.itesla_project.cta.model.IpsoNode;
import eu.itesla_project.iidm.network.*;

/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class LineModelConverter extends AbstractBranchModelConverter<Line, IpsoLine> {

    protected LineModelConverter(ConversionContext context) {
        super(context);
    }

    /**
     * @return ipso line
     */
    @Override
    public IpsoLine doConvert(Line line) {
        Equipments.ConnectionInfo info1 = Equipments.getConnectionInfoInBusBreakerView(line.getTerminal1());
        Equipments.ConnectionInfo info2 = Equipments.getConnectionInfoInBusBreakerView(line.getTerminal2());

        Bus bus1 = info1.getConnectionBus();
        Bus bus2 = info2.getConnectionBus();

        final IpsoNode ipsoNode1 = getContext().getMappingBetweenIidmIdAndIpsoEquipment().getIpsoNodeFor(bus1).get();
        final IpsoNode ipsoNode2 = getContext().getMappingBetweenIidmIdAndIpsoEquipment().getIpsoNodeFor(bus2).get();
        final String id = createIdFrom(ipsoNode1.getId(), ipsoNode2.getId());

        final float vnom = getVnom(line, TwoTerminalsConnectable.Side.ONE);
        final float vnomPow2 = (float) Math.pow(vnom, 2);
        float Rpu = (line.getR() * snref()) / vnomPow2;  //...total line resistance  [p.u.]
        float Xpu = (line.getX() * snref()) / vnomPow2;  //...total line reactance   [p.u.]
        float Gpu = (line.getG1() / snref()) * vnomPow2; //...semi shunt conductance [p.u.]
        float Bpu = (line.getB1() / snref()) * vnomPow2; //...semi shunt susceptance [p.u.]

        // x 100 to convert pu to percent of pu, in order to be compliant with PSA format (used by IPSO)
        Rpu = Rpu * 100.0f;
        Xpu = Xpu * 100.0f;
        Gpu = Gpu * 100.0f;
        Bpu = Bpu * 100.0f;

        final int world = getContext().getWorld();
        return new IpsoLine(
                id,
                line.getId(),
                ipsoNode1,
                ipsoNode2,
                info1.isConnected(),
                info2.isConnected(),
                Rpu,
                Xpu,
                Gpu,
                Bpu,
                findMaxCurrentPermanentLimitFor(line),
                line.getTerminal1().getI(),
                line.getTerminal2().getI(),
                world);
    }

    /**
     * Get Nominal Voltage one each side of TwoTerminalsConnectable (iidm)
     * @param connectable
     * @param side
     * @return Nominal Voltage [Kv]
     */
    private static float getVnom( TwoTerminalsConnectable connectable, TwoTerminalsConnectable.Side side ) {
        Iterable<Bus> buses;
        if (side == TwoTerminalsConnectable.Side.ONE )
        {
            buses = connectable.getTerminal1().getVoltageLevel().getBusBreakerView().getBuses();
        }
        else
        {
            buses = connectable.getTerminal2().getVoltageLevel().getBusBreakerView().getBuses();
        }

        Bus bus = buses.iterator().next();
        if ( bus == null ) throw new ITeslaException("getVnom cannot get bus of the same voltage level");
        return DataUtil.getSafeValueOf(bus.getVoltageLevel().getNominalV());
    }

    @Override
    protected ComponentType getComponentType() {
        return ComponentType.LINE;
    }

    @Override
    public Iterable<Line> gatherDataToConvertFrom(Network network) {
        return network.getLines();
    }

}
