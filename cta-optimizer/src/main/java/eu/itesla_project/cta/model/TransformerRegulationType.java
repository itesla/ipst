/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public enum TransformerRegulationType {
    NO(0),
    VOLTAGE_SIDE_1(1),
    VOLTAGE_SIDE_2(2),
    ACTIVE_FLUX_1(1),
    ACTIVE_FLUX_2(2);


    private final int sideValue;

    TransformerRegulationType(int sideValue) {
        this.sideValue = sideValue;
    }

    public boolean isVoltageRegulationType() {
        return this == VOLTAGE_SIDE_1 || this == VOLTAGE_SIDE_2;
    }
    public boolean isFlowRegulationType() {
        return this == ACTIVE_FLUX_1 || this == ACTIVE_FLUX_2;
    }

    public boolean isRegulating() {
        return this != NO;
    }

    public int getSideValue() {
        return sideValue;
    }
}