/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public final class TransformerRegulationParameters {
    private final IpsoNode regulatedNode;
    private final int controlledSide;
    private final float setpoint;
    private final TransformerRegulationType regulationType;
    private float currentStepPosition;

    public TransformerRegulationParameters(TransformerRegulationType regulationType, float setpoint, int controlledSide, int currentStepPosition, IpsoNode regulatedNode) {
        this.regulationType = regulationType;
        this.setpoint = setpoint;
        this.controlledSide = controlledSide;
        this.currentStepPosition = currentStepPosition;
        this.regulatedNode = regulatedNode;
    }

    public int getControlledSide() {
        return controlledSide;
    }

    /**
     * @return the value of setpoint. With:
     * - value in pu if regulationType is "voltage"
     * - value in Ampere if regulationType is "ACTIVE_FLUX_x"
     */
    public float getSetpoint() {
        return setpoint;
    }

    public boolean hasSetpointDefined() {
        return !Float.isNaN(setpoint);
    }

    public TransformerRegulationType getRegulationType() {
        return regulationType;
    }

    public IpsoNode getRegulatedNode() {
        return regulatedNode;
    }

    public float getCurrentStepPosition() {
        return currentStepPosition;
    }
}
