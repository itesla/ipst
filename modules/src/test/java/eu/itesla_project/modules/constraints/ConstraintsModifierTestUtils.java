/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.constraints;

import com.powsybl.iidm.network.*;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationType;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class ConstraintsModifierTestUtils {

    public static final String VOLTAGE_LEVEL_1_ID = "vl1";
    public static final double HIGH_VOLTAGE_LIMIT = 300.0;
    public static final String VOLTAGE_LEVEL_2_ID = "vl2";
    public static final double LOW_VOLTAGE_LIMIT = 420.0;
    public static final String LINE_ID = "line1";
    public static final double CURRENT_LIMIT = 100.0;
    public static final double CURRENT_VALUE = 119.25631358010583;
    public static final double V = 380.0;
    private static final double Q = 55.0;
    private static final double P = 56.0;
    private static final Country COUNTRY = Country.FR;

    public static Network getNetwork() {
        Network n = NetworkFactory.create("test1", "test");
        Substation s1 = n.newSubstation()
                .setId("s1")
                .setCountry(COUNTRY)
                .add();
        VoltageLevel vl1 = s1.newVoltageLevel()
                .setId(VOLTAGE_LEVEL_1_ID)
                .setNominalV(V)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setHighVoltageLimit(HIGH_VOLTAGE_LIMIT)
                .setLowVoltageLimit(200.0)
                .add();
        Bus b1 = vl1.getBusBreakerView().newBus()
                .setId("b1")
                .add();
        b1.setV(V);
        Substation s2 = n.newSubstation()
                .setId("s2")
                .setCountry(COUNTRY)
                .add();
        VoltageLevel vl2 = s2.newVoltageLevel()
                .setId(VOLTAGE_LEVEL_2_ID)
                .setNominalV(V)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setHighVoltageLimit(550.0)
                .setLowVoltageLimit(LOW_VOLTAGE_LIMIT)
                .add();
        Bus b2 = vl2.getBusBreakerView().newBus()
                .setId("b2")
                .add();
        b2.setV(V);
        Line l1 = n.newLine()
                .setId(LINE_ID)
                .setVoltageLevel1(VOLTAGE_LEVEL_1_ID)
                .setBus1("b1")
                .setConnectableBus1("b1")
                .setVoltageLevel2(VOLTAGE_LEVEL_2_ID)
                .setBus2("b2")
                .setConnectableBus2("b2")
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();
        l1.newCurrentLimits1()
        .setPermanentLimit(CURRENT_LIMIT)
        .add();
        l1.newCurrentLimits2()
        .setPermanentLimit(CURRENT_LIMIT)
        .add();
        l1.getTerminal1().setP(P);
        l1.getTerminal1().setQ(Q);
        l1.getTerminal2().setP(P);
        l1.getTerminal2().setQ(Q);
        return n;
    }

    public static List<LimitViolation> getViolations() {
        List<LimitViolation> violations = new ArrayList<LimitViolation>();
        violations.add(new LimitViolation(LINE_ID, LimitViolationType.CURRENT, null, Integer.MAX_VALUE, CURRENT_LIMIT, 1.0f, CURRENT_VALUE, Branch.Side.ONE));
        violations.add(new LimitViolation(VOLTAGE_LEVEL_1_ID, LimitViolationType.HIGH_VOLTAGE, HIGH_VOLTAGE_LIMIT, 1.0f, V));
        violations.add(new LimitViolation(VOLTAGE_LEVEL_2_ID, LimitViolationType.LOW_VOLTAGE, LOW_VOLTAGE_LIMIT, 1.0f, V));
        return violations;
    }

}
