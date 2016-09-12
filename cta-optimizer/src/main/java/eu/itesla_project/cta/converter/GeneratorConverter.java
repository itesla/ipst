/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.cta.model.DataUtil;
import eu.itesla_project.cta.model.IpsoGenerator;
import eu.itesla_project.cta.model.IpsoNode;
import eu.itesla_project.iidm.network.*;
import eu.itesla_project.iidm.network.util.Identifiables;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class GeneratorConverter extends AbstractModelConverter<Generator, IpsoGenerator>{
    private static final int REACTIVE_POWER_LOWER_LIMIT = -9999;
    private static final int REACTIVE_POWER_UPPER_LIMIT = 9999;

    private int generatorCounter = 0;
    private int biggestGeneratorIndex = 0;
    private float biggestActivePower = 0f;

    GeneratorConverter(ConversionContext context) {
        super(context);
    }

    /**
     * Convert iidm generator to Ipso generator .
     * Moreover, the conversion performs the research of
     * macro region as internal or external and de type PV, SB or PQ for nodes
     *
     * @param network
     * @return list of node for ipso
     */
    @Override
    public List<IpsoGenerator> convert(Network network) {
        checkArgument(network != null, "network cannto be null");
        List<IpsoGenerator> ipsoGenerators = new ArrayList<>();

        for(Generator generator : Identifiables.sort(gatherDataToConvertFrom(network)) ) {
            identifyBiggestGenerator(generator);

            // createAmplModelFrom it to Ipso generator
            final IpsoGenerator ipsoGenerator = this.doConvert(generator);
            ipsoGenerators.add(ipsoGenerator);
            addToDictionary(generator, ipsoGenerator);

            generatorCounter++;
        }

        defineBiggestGenerator(ipsoGenerators);
        return ipsoGenerators;
    }

    private void identifyBiggestGenerator(Generator generator) {
        if ( isConnected(generator)) {
            if (this.biggestActivePower < DataUtil.getSafeValueOf(generator.getTargetP())) {
                this.biggestActivePower = generator.getTargetP();
                this.biggestGeneratorIndex = generatorCounter;
            }
        }
    }

    private boolean isConnected(Generator generator) {
        Equipments.ConnectionInfo info = Equipments.getConnectionInfoInBusBreakerView(generator.getTerminal());
        return info.isConnected();
    }

    private void defineBiggestGenerator(List<IpsoGenerator> ipsoGenerators) {
        ipsoGenerators.get(biggestGeneratorIndex).setBiggest(true);
    }

    @Override
    protected IpsoGenerator doConvert(Generator generator) {
        final String id = createIpsoId();

        Equipments.ConnectionInfo connectionInfo =
                Equipments.getConnectionInfoInBusBreakerView(generator.getTerminal());
        Bus bus = connectionInfo.getConnectionBus();
        final IpsoNode ipsoNode = getContext().getMappingBetweenIidmIdAndIpsoEquipment().getIpsoNodeFor(bus).get();

        final float activePower = -1.0f * generator.getTerminal().getP();   //...active power [MW]
        final float reactivePower = -1.0f * generator.getTerminal().getQ(); //...reactive power [Mvar]
        final float minActivePower  = generator.getMinP();  //...minimum active power [MW]
        final float maxActivePower  = generator.getMaxP();  //...maximum active power [MW]

        ReactiveLimits reactiveLimits = generator.getReactiveLimits();
        //...minimum reactive power [Mvar]
        final float minReactivePower = getContext().isNoGeneratorMinMaxQ() || reactiveLimits == null
                ? REACTIVE_POWER_LOWER_LIMIT : reactiveLimits.getMinQ(activePower);
        //...maximum reactive power [Mvar]
        final float maxReactivePower = getContext().isNoGeneratorMinMaxQ() || reactiveLimits == null
                ? REACTIVE_POWER_UPPER_LIMIT  : reactiveLimits.getMaxQ(activePower);

        final double nominalPower = Math.sqrt(Math.pow(activePower, 2) + Math.pow(reactivePower, 2));
        final float nominalVoltage = bus.getVoltageLevel().getNominalV();
        final float setpointVoltage = generator.isVoltageRegulatorOn() ? DataUtil.getSafeValueOf(generator.getTargetV()) : 0f;

        return new IpsoGenerator(
                id,
                generator.getId(),
                ipsoNode,
                connectionInfo.isConnected(),
                generator.isVoltageRegulatorOn(),
                minActivePower,
                maxActivePower,
                minReactivePower,
                maxReactivePower,
                (float)nominalPower,
                activePower,
                reactivePower,
                nominalVoltage,
                setpointVoltage,
                getContext().getWorld());
    }

    @Override
    public Iterable<Generator> gatherDataToConvertFrom(Network network) {
        return network.getGenerators();
    }

    @Override
    protected ComponentType getComponentType() {
        return ComponentType.GENERATOR;
    }

}
