/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.mcla;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.Terminal.BusBreakerView;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class NetworkUtilsTest {

    @Test
    public void test() {
        Bus gen1Bus = Mockito.mock(Bus.class);
        Mockito.when(gen1Bus.getV()).thenReturn(380.0);
        BusBreakerView gen1BusBreakerView = Mockito.mock(BusBreakerView.class);
        Mockito.when(gen1BusBreakerView.getBus()).thenReturn(gen1Bus);
        Terminal gen1Terminal = Mockito.mock(Terminal.class);
        Mockito.when(gen1Terminal.getBusBreakerView()).thenReturn(gen1BusBreakerView);
        Generator gen1 =  Mockito.mock(Generator.class);
        Mockito.when(gen1.getId()).thenReturn("gen1");
        Mockito.when(gen1.getTerminal()).thenReturn(gen1Terminal);
        assertTrue(NetworkUtils.isConnected(gen1));

        BusBreakerView gen2BusBreakerView = Mockito.mock(BusBreakerView.class);
        Mockito.when(gen2BusBreakerView.getBus()).thenReturn(null);
        Terminal gen2Terminal = Mockito.mock(Terminal.class);
        Mockito.when(gen2Terminal.getBusBreakerView()).thenReturn(gen2BusBreakerView);
        Generator gen2 =  Mockito.mock(Generator.class);
        Mockito.when(gen2.getId()).thenReturn("gen2");
        Mockito.when(gen2.getTerminal()).thenReturn(gen2Terminal);
        assertFalse(NetworkUtils.isConnected(gen2));

        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getId()).thenReturn("network");
        Mockito.when(network.getGenerators()).thenAnswer(dummy -> ImmutableSet.of(gen1, gen2));
        assertEquals(2, NetworkUtils.getGeneratorsIds(network).size(), 0);
        assertEquals(1, NetworkUtils.getConnectedGeneratorsIds(network).size(), 0);
        assertEquals("gen1", NetworkUtils.getConnectedGeneratorsIds(network).iterator().next());

        Mockito.when(gen1.getEnergySource()).thenReturn(EnergySource.SOLAR);
        Mockito.when(gen2.getEnergySource()).thenReturn(EnergySource.THERMAL);
        assertEquals(1, NetworkUtils.getRenewableGeneratorsIds(network).size(), 0);
        assertEquals("gen1", NetworkUtils.getRenewableGeneratorsIds(network).iterator().next());
        
        Bus load1Bus = Mockito.mock(Bus.class);
        Mockito.when(load1Bus.getV()).thenReturn(Double.NaN);
        BusBreakerView load1BusBreakerView = Mockito.mock(BusBreakerView.class);
        Mockito.when(load1BusBreakerView.getBus()).thenReturn(load1Bus);
        Terminal load1Terminal = Mockito.mock(Terminal.class);
        Mockito.when(load1Terminal.getBusBreakerView()).thenReturn(load1BusBreakerView);
        Load load1 =  Mockito.mock(Load.class);
        Mockito.when(load1.getId()).thenReturn("load1");
        Mockito.when(load1.getTerminal()).thenReturn(load1Terminal);
        assertFalse(NetworkUtils.isConnected(load1));

        Bus load2Bus = Mockito.mock(Bus.class);
        Mockito.when(load2Bus.getV()).thenReturn(380.0);
        BusBreakerView load2BusBreakerView = Mockito.mock(BusBreakerView.class);
        Mockito.when(load2BusBreakerView.getBus()).thenReturn(load2Bus);
        Terminal load2Terminal = Mockito.mock(Terminal.class);
        Mockito.when(load2Terminal.getBusBreakerView()).thenReturn(load2BusBreakerView);
        Load load2 =  Mockito.mock(Load.class);
        Mockito.when(load2.getId()).thenReturn("load2");
        Mockito.when(load2.getTerminal()).thenReturn(load2Terminal);
        assertTrue(NetworkUtils.isConnected(load2));

        Mockito.when(network.getLoads()).thenAnswer(dummy -> ImmutableSet.of(load1, load2));
        assertEquals(2, NetworkUtils.getLoadsIds(network).size(), 0);
        assertEquals(1, NetworkUtils.getConnectedLoadsIds(network).size(), 0);
        assertEquals("load2", NetworkUtils.getConnectedLoadsIds(network).iterator().next());
    }
}
