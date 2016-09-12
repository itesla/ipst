/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import eu.itesla_project.cta.model.*;

/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class IpsoProblemComponentFactory {

    public static final float DEFAULT_BRANCH_FLOW_MIN = 0.0f;

    public IpsoControlVariableProductionP createVariableProductionP(IpsoGenerator generator, LinkOption linkOption, float speed, int world) {
        return new IpsoControlVariableProductionP(generator, linkOption, speed, world);

    }

    public IpsoControlVariableProductionQ createVariableProductionQ(IpsoGenerator generator, LinkOption linkOption, float speed, int world) {
       return new IpsoControlVariableProductionQ(generator, linkOption, speed, world);

    }

    public IpsoControlVariableGeneratorStatism createVariableGeneratorStatism(IpsoGenerator generator, float voltageSetpoint, int world) {
        return new IpsoControlVariableGeneratorStatism(generator, voltageSetpoint, world);

    }

    public IpsoControlVariableBankStep createVariableBankStep(IpsoBank bank, float speed, int world) {
        return new IpsoControlVariableBankStep(bank, speed, world);
    }

    public IpsoControlVariable2WTransformerTap createVariable2WTransformerTap(IpsoTwoWindingsTransformer transformer, LinkOption linkOption, float speed, float epsilon, int world) {
        return new IpsoControlVariable2WTransformerTap(transformer, linkOption, speed, epsilon, world);
    }

    public IpsoConstraintLineFlowSide1 createConstraintLineFlowSide1(IpsoLine line, FlowUnit unit, float flowMin, float flowMax, int world) {
        return new IpsoConstraintLineFlowSide1(line, unit, flowMin, flowMax, world);
    }

    public IpsoConstraintLineFlowSide2 createConstraintLineFlowSide2(IpsoLine line, FlowUnit unit, float flowMin, float flowMax, int world) {
        return new IpsoConstraintLineFlowSide2(line, unit, flowMin, flowMax, world);
    }

    public IpsoConstraint2WTransformerFlow createConstraint2WTransformerFlow(IpsoTwoWindingsTransformer transformer, FlowUnit unit, float flowMin, float flowMax, int world) {
        return new IpsoConstraint2WTransformerFlow(transformer, unit, flowMin, flowMax, world);
    }

    public IpsoConstraintNodeAcVoltageBounds createConstraintNodeAcVoltageBounds(IpsoNode ipsoNode, VoltageUnit unit, float minVoltage, float maxVoltage, int world) {
        return new IpsoConstraintNodeAcVoltageBounds(ipsoNode, unit, minVoltage, maxVoltage, world);
    }

    public IpsoConstraintGeneratorPBounds createConstraintProductionPBounds(IpsoGenerator generator, float minActivePower, float maxActivePower, int world) {
        return new IpsoConstraintGeneratorPBounds(generator, minActivePower, maxActivePower, world);
    }

    public IpsoConstraintGeneratorQBounds createConstraintProductionQBounds(IpsoGenerator generator, float minReactivePower, float maxReactivePower, int world) {
        return new IpsoConstraintGeneratorQBounds(generator, minReactivePower, maxReactivePower, world);

    }

    public IpsoConstraintNodeAcAngleBounds createConstraintNodeAcAngleBounds(IpsoNode ipsoNode, float angleMin, float angleMax, int world) {
        return new IpsoConstraintNodeAcAngleBounds(ipsoNode, angleMin, angleMax, world);

    }

    public IpsoConstraint2WTransformerTapBounds createConstraint2WTfoTap(IpsoTwoWindingsTransformer transformer, int tapMin, int tapMax, int world) {
        return new IpsoConstraint2WTransformerTapBounds(transformer, tapMin, tapMax, world);
    }

    public IpsoConstraintBankStepBounds createConstraintBankStepBounds(IpsoBank ipsoBank, int stepMin, int stepMax, int world) {
        return new IpsoConstraintBankStepBounds(ipsoBank, stepMin, stepMax, world);
    }

    public <T extends AbstractIpsoConstraintLineFlow> T createConstraintLineFlow(IpsoLine line, FlowType side1, int world) {
        FlowUnit flowUnit = FlowUnit.AMPERE;
        if (side1 == FlowType.SIDE1) {
            return (T) new IpsoConstraintLineFlowSide1(line, flowUnit, DEFAULT_BRANCH_FLOW_MIN, line.getMaxCurrentPermanentLimit(), world);
        } else {
            return (T) new IpsoConstraintLineFlowSide2(line, flowUnit, DEFAULT_BRANCH_FLOW_MIN, line.getMaxCurrentPermanentLimit(), world);
        }
    }
}
