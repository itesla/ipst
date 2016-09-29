/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.iidm.network.Bus;
import eu.itesla_project.iidm.network.Equipments;
import eu.itesla_project.iidm.network.Generator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
final class SpecialNodeFinder {

    private Bus slackBus;
    private Generator slackBusGenerator;
    private int slackBusIndex;
    private int busCounter;
    private Set<Integer> pvNodeIndexes;

    /**
     * constructor
     */
    public SpecialNodeFinder() {
        resetFinder();
    }

    /**
     * Reinitialize finder
     */
    public void resetFinder() {
        slackBus = null;
        slackBusGenerator = null;
        slackBusIndex = -1;
        busCounter = 0;
        pvNodeIndexes = new HashSet<>();
    }

    /**
     * Browses and determines the bus which is slackbus or PV node
     */
    public void browse(Bus bus) {

        if (atLeastOneGeneratorFor(bus)) {

            for (Generator generator : bus.getGenerators()) {

                // a regulated generator
                if (generator.isVoltageRegulatorOn()) {

                    if (areConnected(generator, bus))
                    {
                        storeBiggest(generator, bus);

                        storePvNodesFor(generator);
                    }
                }
            }
        }
        busCounter++;
    }

    private boolean atLeastOneGeneratorFor(Bus bus) {
        return bus.getGenerators() != null;
    }

    private boolean areConnected(Generator generator, Bus CandidateBus) {
        Equipments.ConnectionInfo info = Equipments.getConnectionInfoInBusBreakerView(generator.getTerminal());
        // if the generator is connected to the bus
        if (info.isConnected() ) {
            //...Assure the candidate bus is the real bus and not an aggregated bus
            return  info.getConnectionBus().getId().equals(CandidateBus.getId());
        } else {
            return false;
        }
    }

    /**
     * get index of slackbus among all browsed buses
     * @return
     */
    public int getSlackBusIndex() {
        return slackBusIndex;
    }

    public boolean hasSlackBusFound() {
        return slackBusIndex >= 0;
    }

    /**
     * get the slackbus chosen among all browsed buses
     * @return
     */
    public Bus getSlackBus() {
        return slackBus;
    }

    /**
     *
     * @return the bigger generator connected to the slackbus
     */
    public Generator getSlackBusGenerator() {
        return slackBusGenerator;
    }

    /**
     * Get PV node indexes among all browsed buses
     * @return
     */
    public Set<Integer> getPvNodeIndexes() {
        return Collections.unmodifiableSet(pvNodeIndexes);
    }

    /**
     * Store biggest generator (Pmax) and his connection bus as slackbus
     */
    private void storeBiggest(Generator generator, Bus bus) {
        if (slackBusGenerator == null || isBiggestGenerator(generator)) {
            slackBus = bus;
            slackBusGenerator = generator;
            slackBusIndex = this.busCounter;
        }
    }

    private boolean isBiggestGenerator(Generator generator) {
        return generator.getMaxP() > slackBusGenerator.getMaxP();
    }

    private void storePvNodesFor(Generator generator) {

        // at least on regulator in V Regulator is enough
        // to define the connected node as PV
        if (generator.isVoltageRegulatorOn()) {
                pvNodeIndexes.add(this.busCounter);
        }
    }
}
