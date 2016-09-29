/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoTwoWindingsTransformer extends AbstractIpsoBranch{

    private final TransformerRegulationParameters regulationParameters;
    private final float maxCurrentPermanentLimit;
    private final float currentFlow;
    private final boolean connectedSide1;
    private final boolean connectedSide2;
    private final float cuLosses;
    private final float feLosses;
    private final float magnetizingCurrent;
    private final float saturationExponent;
    private final float rate;
    private final int nominalTap;
    private final int initialTap;
    private final int lowStep;
    private final int highStep;
    private final int stepSize;
    private final List<Integer> indexes;
    private final List<Float> voltages_side1;
    private final List<Float> voltages_side2;
    private final List<Float> phases;
    private final List<Float> uccs;
    private int stepLowStep = 0;
    private int highstep = 0;

    /**
     * constructor
     */
    public IpsoTwoWindingsTransformer(
            String id,
            String iidmId,
            IpsoNode ipsoNode1,
            IpsoNode ipsoNode2,
            boolean connectedSide1,
            boolean connectedSide2,
            float cuLosses,
            float feLosses,
            float magnetizingCurrent,
            float saturationExponent,
            float rate,
            int nominalTap,
            int initialTap,
            int lowStep,
            int highStep,
            int stepSize,
            List<Integer> indexes,
            List<Float> voltages_side1,
            List<Float> voltages_side2,
            List<Float> phases,
            List<Float> uccs,
            float maxCurrentPermanentLimit,
            float currentFlow,
            TransformerRegulationParameters regulationParameters,
            int world) {
        super(id, iidmId, world, ipsoNode1, ipsoNode2);
        checkArgument(regulationParameters != null, "regulationParameters must not be null");
        checkArgument(ipsoNode1 != null, "ipsoNode1 must not be null");
        checkArgument(ipsoNode2 != null, "ipsoNode2 must not be null");
        this.connectedSide1 = connectedSide1;
        this.connectedSide2 = connectedSide2;
        this.cuLosses = cuLosses;
        this.feLosses = feLosses;
        this.magnetizingCurrent = magnetizingCurrent;
        this.saturationExponent = saturationExponent;
        this.rate = rate;
        this.nominalTap = nominalTap;
        this.initialTap = initialTap;
        this.lowStep = lowStep;
        this.highStep = highStep;
        this.stepSize = stepSize;
        this.indexes = indexes;
        this.voltages_side1 = voltages_side1;
        this.voltages_side2 = voltages_side2;
        this.phases = phases;
        this.uccs = uccs;
        this.maxCurrentPermanentLimit = maxCurrentPermanentLimit;
        this.currentFlow = currentFlow;
        this.regulationParameters = regulationParameters;
    }

    public int getInitialTap() {
        return initialTap;
    }

    public List<Float> getUccs() { return uccs; }

    public float getCuLosses() { return cuLosses; }

    public float getFeLosses() { return feLosses; }

    public float getMagnetizingCurrent() { return magnetizingCurrent; }

    public int getLowStep() {
        return lowStep;
    }

    public int getHighStep() {
        return highStep;
    }

    public TransformerRegulationParameters getRegulationParameters() {
        return regulationParameters;
    }

    public boolean isRegulating() {
        return regulationParameters.getRegulationType().isRegulating();
    }

    public boolean isRatioTapChanger() {
        return regulationParameters.getRegulationType().isVoltageRegulationType();
    }

    public boolean isPhaseTapChanger() {
        return regulationParameters.getRegulationType().isFlowRegulationType();
    }

    public boolean hasMoreThanOneStep() {
        return stepSize > 1;
    }

    public boolean isConnectedOnBothSides() {
        return connectedSide1 && connectedSide2;
    }

    public float getMaxCurrentPermanentLimit() {
        return maxCurrentPermanentLimit;
    }

    public float getCurrentFlow() {
        return currentFlow;
    }

    public List<Integer> getIndexes() {
        return indexes;
    }

    public List<Float> getVoltages_side1() {
        return voltages_side1;
    }

    public List<Float> getVoltages_side2() {
        return voltages_side2;
    }

    public List<Float> getPhases() {
        return phases;
    }

    @Override
    public List<String> getOrderedHeaders() {
        return Lists.newArrayList(
                NAME,
                "NODE_NAME_1",
                "NODE_NAME_2",
                "CONNECTED_SIDE_1",
                "CONNECTED_SIDE_2",
                "CU_LOSSES",
                "FE_LOSSES",
                "MAGNETIZING_CURRENT",
                "SATURATION_EXPONENT",
                "NOMINAL_POWER",
                "NOMINAL_TAP",
                "INITIAL_TAP",
                "CONTROLLED_SIDE",
                "VOLTAGE_SETPOINT",
                WORLD,
                "STEP_NUMBER",
                "STEP_INDEXES",
                "STEP_VOLTAGES_SIDE1",
                "STEP_VOLTAGES_SIDE2",
                "STEP_DEPHASES",
                "STEP_UCCS"
                );
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(
                getId(),
                ipsoNode1.getId(),
                ipsoNode2.getId(),
                getConnectionCodeFor(connectedSide1),
                getConnectionCodeFor(connectedSide2),
                cuLosses,
                feLosses,
                magnetizingCurrent,
                saturationExponent,
                rate,
                nominalTap,
                initialTap,
                regulationParameters.getControlledSide(),
                regulationParameters.getSetpoint(),
                getWorld(),
                stepSize > 0 ? stepSize : null,
                stepSize > 0 ? indexes : null,
                stepSize > 0 ? voltages_side1 : null,
                stepSize > 0 ? voltages_side2 : null,
                stepSize > 0 ? phases : null,
                stepSize > 0 ? uccs : null
        );

    }

    public Optional<IpsoNode> getRegulatedNode() {
        return Optional.ofNullable(this.getRegulationParameters())
                .map(TransformerRegulationParameters::getRegulatedNode);
    }

    public boolean hasRegulatingNode() {
        return getRegulatedNode().isPresent();
    }

    @Override
    public boolean isConnected() {
        return connectedSide1 && connectedSide2;
    }
}
