/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public enum VoltageUnit {

    Voltage("kV"),
    PU("pu");

    private String unit;

    VoltageUnit(String unit) {
        this.unit = unit;
    }

    public String getUnit() {
        return unit;
    }

}
