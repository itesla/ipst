/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.cta.model.IpsoNode;
import eu.itesla_project.cta.model.IpsoNodeType;
import eu.itesla_project.cta.model.IpsoRegionType;
import eu.itesla_project.iidm.network.Bus;
import eu.itesla_project.iidm.network.DanglingLine;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.util.SV;

import static com.google.common.base.Preconditions.checkArgument;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class XnodeModelConverter extends AbstractNodeModelConverter<DanglingLine> {

    public XnodeModelConverter(ConversionContext context) {
        super(context);
    }

    @Override
    public IpsoNode doConvert(DanglingLine danglingLine) {
        checkArgument(danglingLine != null, "danglingLine must not be null");
        checkArgument(danglingLine.getTerminal() != null, "danglingLine.getTerminal() must not be null");
        checkArgument(danglingLine.getTerminal().getBusBreakerView() != null, "danglingLine.getTerminal().getBusBreakerView() must not be null");

        //checkArgument(danglingLine.getTerminal().getBusBreakerView().getBus()!= null, "danglingLine.getTerminal().getBusBreakerView().getBus() must not be null");
        if (danglingLine.getTerminal().getBusBreakerView().getBus() == null) {
            return null;
        } else {
            final String id = createIpsoId();
            final Bus bus = danglingLine.getTerminal().getBusBreakerView().getBus();

            // voltage of known bus of the dangling line
            final float voltage = bus.getV();
            // angle of known bus of the dangling line
            final float angle = bus.getAngle();

            final float p = danglingLine.getTerminal().getP();
            final float q = danglingLine.getTerminal().getQ();

            final float pn = danglingLine.getTerminal().getBusBreakerView().getBus().getP();
            final float qn = danglingLine.getTerminal().getBusBreakerView().getBus().getQ();

            final SV known = new SV(p, q, voltage, angle);
            final SV fictitious = known.otherSide(danglingLine);
            final float baseVoltage = findBaseVoltage(bus);

            return new IpsoNode(
                    id,
                    getIdOf(danglingLine),
                    findAreaFor(bus),
                    IpsoRegionType.EXTERNAL.getValue(),
                    baseVoltage,
                    fictitious.getU(),
                    fictitious.getA(),
                    IpsoNodeType.PQ,
                    fictitious.getP(),
                    fictitious.getQ(),
                    findLowVoltageLevel(bus, baseVoltage),
                    findHighVoltageLevel(bus, baseVoltage),
                    getContext().getWorld());
        }
    }

    @Override
    public Iterable<DanglingLine> gatherDataToConvertFrom(Network network) {
        return network.getDanglingLines();
    }

    @Override
    protected ComponentType getComponentType() {
        return ComponentType.XNODE;
    }

    /**
     * @return A fake id for fictive node created from danglingLine
     */
    @Override
    protected String getIdOf(DanglingLine danglingLine) {
        return IpsoConverterUtil.getFictiveBusIdFor(danglingLine);
    }
}
