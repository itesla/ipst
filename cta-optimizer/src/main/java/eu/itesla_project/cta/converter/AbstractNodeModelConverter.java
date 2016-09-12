/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.cta.model.DataUtil;
import eu.itesla_project.cta.model.IpsoNode;
import eu.itesla_project.cta.model.IpsoNodeType;
import eu.itesla_project.iidm.network.Bus;
import eu.itesla_project.iidm.network.Identifiable;
import eu.itesla_project.iidm.network.VoltageLevel;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Float.isNaN;
import static java.util.Optional.of;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 *
 * Abstract class to createAmplModelFrom iidm identifiable to ipso node
 */
abstract class AbstractNodeModelConverter<F extends Identifiable> extends AbstractModelConverter<F, IpsoNode> {

    protected static final String EXTERNAL = "EXTERNAL";
    protected static final float DEFAULT_LOW_VOLTAGE_LEVEL = 0.5f;
    protected static final float DEFAULT_HIGH_VOLTAGE_LEVEL = 1.5f;

    protected String getIidmId(F identifiable) {
        return identifiable.getId();
    }

    /**
     * constructor
     * @param context of the conversion
     */
    protected AbstractNodeModelConverter(ConversionContext context) {
        super(context);
    }

    protected IpsoNode convertBus(String iidmId, Bus bus) {

        String id = createIpsoId();

        // getValue area as the id of enumeration constant (EXTERNAL: "FR")
        String area = findAreaFor(bus);
        // complete area with blank in order to have for 8 characters for macro-region
        String area8 = StringUtils.rightPad(area, 8);

        final float baseVoltage = findBaseVoltage(bus);
        final float minVoltageLimit = findLowVoltageLevel(bus, baseVoltage); // pu
        final float maxVoltageLimit = findHighVoltageLevel(bus, baseVoltage); // pu

        // Set PQ by default (PV or SB nodes will be determined latter)
        IpsoNodeType nodeType = IpsoNodeType.PQ;

        return new IpsoNode(
                id,
                iidmId,
                area,
                area8,
                baseVoltage,
                bus == null ? 0.f : bus.getV(),
                bus == null ? 0.f : bus.getAngle(),
                nodeType,
                bus == null ? 0.f : bus.getP(),
                bus == null ? 0.f : bus.getQ(),
                minVoltageLimit,
                maxVoltageLimit,
                getContext().getWorld());
    }

    protected String findAreaFor(Bus bus) {
        return of(bus)
                .map(b -> b.getVoltageLevel().getSubstation().getCountry().name())
                .orElse(EXTERNAL);
    }

    protected float findBaseVoltage(Bus bus) {
        checkArgument(bus != null, "bus must not be null");
        Optional<VoltageLevel> voltageLevel = getVoltageLevelOf(bus);
        if (voltageLevel.isPresent()) {
            return voltageLevel.get().getNominalV();
        }
        else {
            return Float.NaN;
        }
    }

    private Optional<VoltageLevel>  getVoltageLevelOf(Bus bus) {
        checkArgument(bus != null, "bus must not be null");
        return  Optional.ofNullable(bus.getVoltageLevel());
    }

    protected float findLowVoltageLevel(Bus bus, float baseVoltage) {
        checkArgument(bus != null, "bus must not be null");
        if (isNaN(baseVoltage)) {
            return DEFAULT_LOW_VOLTAGE_LEVEL;
        } else {
            Optional<VoltageLevel> voltageLevel = getVoltageLevelOf(bus);
            if (voltageLevel.isPresent()){
                return DataUtil.getSafeValueOf(voltageLevel.get().getLowVoltageLimit() / baseVoltage, DEFAULT_LOW_VOLTAGE_LEVEL);
            }
            else {
                return DEFAULT_LOW_VOLTAGE_LEVEL;
            }
        }
    }

    protected float findHighVoltageLevel(Bus bus, float baseVoltage) {
        checkArgument(bus != null, "bus must not be null");
        if (isNaN(baseVoltage)) {
            return DEFAULT_HIGH_VOLTAGE_LEVEL;
        } else {
            Optional<VoltageLevel> voltageLevel = getVoltageLevelOf(bus);
            if (voltageLevel.isPresent()){
                return DataUtil.getSafeValueOf(voltageLevel.get().getHighVoltageLimit() / baseVoltage, DEFAULT_HIGH_VOLTAGE_LEVEL);
            }
            else {
                return DEFAULT_HIGH_VOLTAGE_LEVEL;
            }
        }
    }
}
