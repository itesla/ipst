/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.actions_contingencies.xml.test;

import eu.itesla_project.iidm.network.Bus;
import eu.itesla_project.iidm.network.Country;
import eu.itesla_project.iidm.network.Line;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.NetworkFactory;
import eu.itesla_project.iidm.network.Substation;
import eu.itesla_project.iidm.network.TopologyKind;
import eu.itesla_project.iidm.network.VoltageLevel;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class ACXmlClientTestUtils {

    public static Network getNetwork() {
        Network n = NetworkFactory.create("test1", "test");

        Substation s1 = n.newSubstation()
                .setId("S1")
                .setCountry(Country.FR)
                .add();
        VoltageLevel vl1 = s1.newVoltageLevel()
                .setId("VL1")
                .setNominalV(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus b1 = vl1.getBusBreakerView().newBus()
                .setId("B1")
                .add();

        Substation s2 = n.newSubstation()
                .setId("S2")
                .setCountry(Country.FR)
                .add();
        VoltageLevel vl2 = s2.newVoltageLevel()
                .setId("VL2")
                .setNominalV(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus b2 = vl2.getBusBreakerView().newBus()
                .setId("B2")
                .add();

        Substation s3 = n.newSubstation()
                .setId("S3")
                .setCountry(Country.FR)
                .add();
        VoltageLevel vl3 = s3.newVoltageLevel()
                .setId("VL3")
                .setNominalV(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus b3 = vl3.getBusBreakerView().newBus()
                .setId("B3")
                .add();

        Line l1 = n.newLine()
                .setId("LINE1_ACLS")
                .setVoltageLevel1("VL1")
                .setBus1("B1")
                .setConnectableBus1("B1")
                .setVoltageLevel2("VL2")
                .setBus2("B2")
                .setConnectableBus2("B2")
                .setR(0)
                .setX(0)
                .setG1(0)
                .setG2(0)
                .setB1(0)
                .setB2(0)
                .add();

        Line l2 = n.newLine()
                .setId("LINE2_ACLS")
                .setVoltageLevel1("VL2")
                .setBus1("B2")
                .setConnectableBus1("B2")
                .setVoltageLevel2("VL3")
                .setBus2("B3")
                .setConnectableBus2("B3")
                .setR(0)
                .setX(0)
                .setG1(0)
                .setG2(0)
                .setB1(0)
                .setB2(0)
                .add();

        return n;
    }

}
