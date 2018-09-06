/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.mcla;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.LoadType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public final class NetworkUtils {

    private NetworkUtils() {
    }

    public static ArrayList<String> getRenewableGeneratorsIds(Network network) {
        Objects.requireNonNull(network, "network is null");
        ArrayList<String> generatorsIds = new ArrayList<String>();
        for (Generator generator : network.getGenerators()) {
            if (generator.getEnergySource().isIntermittent()) {
                generatorsIds.add(generator.getId());
            }
        }
        Collections.sort(generatorsIds);
        return generatorsIds;
    }

    public static ArrayList<String> getGeneratorsIds(Network network) {
        Objects.requireNonNull(network, "network is null");
        ArrayList<String> generatorsIds = new ArrayList<String>();
        for (Generator generator : network.getGenerators()) {
            generatorsIds.add(generator.getId());
        }
        Collections.sort(generatorsIds);
        return generatorsIds;
    }

    public static ArrayList<String> getConnectedGeneratorsIds(Network network) {
        Objects.requireNonNull(network, "network is null");
        ArrayList<String> generatorsIds = new ArrayList<String>();
        for (Generator generator : network.getGenerators()) {
            if (isConnected(generator)) {
                generatorsIds.add(generator.getId());
            }
        }
        Collections.sort(generatorsIds);
        return generatorsIds;
    }

    public static boolean isConnected(Generator generator) {
        Bus generatorBus = generator.getTerminal().getBusBreakerView().getBus();
        double voltage = getV(generator.getTerminal());
        if (generatorBus != null && !Double.isNaN(voltage)) {
            // generator is connected
            return true;
        }
        return false;
    }

    public static ArrayList<String> getLoadsIds(Network network) {
        Objects.requireNonNull(network, "network is null");
        ArrayList<String> loadsIds = new ArrayList<String>();
        for (Load load : network.getLoads()) {
            if (load.getLoadType() != LoadType.FICTITIOUS) {
                loadsIds.add(load.getId());
            }
        }
        Collections.sort(loadsIds);
        return loadsIds;
    }

    public static ArrayList<String> getConnectedLoadsIds(Network network) {
        Objects.requireNonNull(network, "network is null");
        ArrayList<String> loadsIds = new ArrayList<String>();
        for (Load load : network.getLoads()) {
            if (isConnected(load) && load.getLoadType() != LoadType.FICTITIOUS) {
                loadsIds.add(load.getId());
            }
        }
        Collections.sort(loadsIds);
        return loadsIds;
    }

    public static boolean isConnected(Load load) {
        Bus loadBus = load.getTerminal().getBusBreakerView().getBus();
        double voltage = getV(load.getTerminal());
        if (loadBus != null && !Double.isNaN(voltage)) {
            // load is connected
            return true;
        }
        return false;
    }

    private static double getV(Terminal t) {
        Bus b = t.getBusBreakerView().getBus();
        return b != null ? b.getV() : Double.NaN;
    }



}
