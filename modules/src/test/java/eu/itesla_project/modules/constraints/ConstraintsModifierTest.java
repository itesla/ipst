/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.constraints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.powsybl.iidm.network.*;
import org.junit.Before;
import org.junit.Test;

import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationType;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class ConstraintsModifierTest {

    private Network network;
    private List<LimitViolation> violations;
    private ConstraintsModifierConfig config;

    @Before
    public void setUp() throws Exception {
        network = ConstraintsModifierTestUtils.getNetwork();
        violations = ConstraintsModifierTestUtils.getViolations();
        Set<LimitViolationType> violationTypes = EnumSet.of(LimitViolationType.CURRENT,
                LimitViolationType.HIGH_VOLTAGE,
                LimitViolationType.LOW_VOLTAGE);
        config = new ConstraintsModifierConfig(ConstraintsModifierConfig.DEFAULT_COUNTRIES, violationTypes);
    }

    private void checkOriginalNetworkLimits() {
        Line line = network.getLine(ConstraintsModifierTestUtils.LINE_ID);
        assertEquals(line.getCurrentLimits1().getPermanentLimit(), ConstraintsModifierTestUtils.CURRENT_LIMIT, 0);
        VoltageLevel voltageLevel1 = network.getVoltageLevel(ConstraintsModifierTestUtils.VOLTAGE_LEVEL_1_ID);
        assertEquals(voltageLevel1.getHighVoltageLimit(), ConstraintsModifierTestUtils.HIGH_VOLTAGE_LIMIT, 0);
        VoltageLevel voltageLevel2 = network.getVoltageLevel(ConstraintsModifierTestUtils.VOLTAGE_LEVEL_2_ID);
        assertEquals(voltageLevel2.getLowVoltageLimit(), ConstraintsModifierTestUtils.LOW_VOLTAGE_LIMIT, 0);
    }

    private void checkModifiedNetworkLimits(float margin) {
        Line line = network.getLine(ConstraintsModifierTestUtils.LINE_ID);
        double newCurrentLimit = ConstraintsModifierTestUtils.CURRENT_VALUE * (1.0 + margin / 100.0);
        assertEquals(newCurrentLimit, line.getCurrentLimits1().getPermanentLimit(), 0.0);
        VoltageLevel voltageLevel1 = network.getVoltageLevel(ConstraintsModifierTestUtils.VOLTAGE_LEVEL_1_ID);
        double newHighVoltageLimit = ConstraintsModifierTestUtils.V * (1.0 + margin / 100.0);
        assertEquals(newHighVoltageLimit, voltageLevel1.getHighVoltageLimit(), 0.0);
        VoltageLevel voltageLevel2 = network.getVoltageLevel(ConstraintsModifierTestUtils.VOLTAGE_LEVEL_2_ID);
        double newLowVoltageLimit = ConstraintsModifierTestUtils.V * (1.0 - margin / 100.0);
        assertEquals(newLowVoltageLimit, voltageLevel2.getLowVoltageLimit(), 0.0);
    }

    @Test
    public void testNoMargin() throws Exception {
        checkOriginalNetworkLimits();

        ConstraintsModifier constraintsModifier = new ConstraintsModifier(network, config);
        constraintsModifier.looseConstraints(VariantManagerConstants.INITIAL_VARIANT_ID);

        checkModifiedNetworkLimits(0);
    }

    @Test
    public void testWithMargin() throws Exception {
        float margin = 3.0f;

        checkOriginalNetworkLimits();

        ConstraintsModifier constraintsModifier = new ConstraintsModifier(network, config);
        constraintsModifier.looseConstraints(VariantManagerConstants.INITIAL_VARIANT_ID, margin);

        checkModifiedNetworkLimits(margin);
    }

    @Test
    public void testWithViolationsNoMargin() throws Exception {
        checkOriginalNetworkLimits();

        ConstraintsModifier constraintsModifier = new ConstraintsModifier(network, config);
        constraintsModifier.looseConstraints(VariantManagerConstants.INITIAL_VARIANT_ID, violations);

        checkModifiedNetworkLimits(0);
    }

    @Test
    public void testWithViolationsAndMargin() throws Exception {
        float margin = 3.0f;

        checkOriginalNetworkLimits();

        ConstraintsModifier constraintsModifier = new ConstraintsModifier(network, config);
        constraintsModifier.looseConstraints(VariantManagerConstants.INITIAL_VARIANT_ID, violations, margin);

        checkModifiedNetworkLimits(margin);
    }

    @Test
    public void testWithViolationsAndMarginApplyBasecase() throws Exception {
        float margin = 3;

        checkOriginalNetworkLimits();

        String stateId = "0";
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, stateId);
        network.getVariantManager().setWorkingVariant(stateId);
        checkOriginalNetworkLimits();

        ConstraintsModifier constraintsModifier = new ConstraintsModifier(network, config);
        constraintsModifier.looseConstraints(stateId, violations, margin, true);

        checkModifiedNetworkLimits(margin);

        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);
        checkModifiedNetworkLimits(margin);

        network.getVariantManager().removeVariant(stateId);
    }

    @Test
    public void testWithMarginApplyBasecase() throws Exception {
        float margin = 3;

        checkOriginalNetworkLimits();

        String stateId = "0";
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, stateId);
        network.getVariantManager().setWorkingVariant(stateId);
        checkOriginalNetworkLimits();

        ConstraintsModifier constraintsModifier = new ConstraintsModifier(network, config);
        constraintsModifier.looseConstraints(stateId, margin, true);

        checkModifiedNetworkLimits(margin);

        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);
        checkModifiedNetworkLimits(margin);

        network.getVariantManager().removeVariant(stateId);
    }

    @Test
    public void testWithNullValues() throws Exception {
        ConstraintsModifier constraintsModifier = new ConstraintsModifier(network, config);
        try {
            constraintsModifier.looseConstraints(null, violations);
            fail();
        } catch (Throwable e) {

        }
        try {
            constraintsModifier.looseConstraints(VariantManagerConstants.INITIAL_VARIANT_ID, null);
            fail();
        } catch (Throwable e) {
        }
    }

    @Test
    public void testWithWrongState() throws Exception {
        ConstraintsModifier constraintsModifier = new ConstraintsModifier(network, config);
        try {
            constraintsModifier.looseConstraints("wrongState", violations);
            fail();
        } catch (Throwable e) {
        }
    }

}
