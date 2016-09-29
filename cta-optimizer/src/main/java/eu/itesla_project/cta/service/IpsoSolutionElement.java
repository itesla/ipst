/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoSolutionElement {

    private final IpsoEquipmentType equipmentType;
    private final String name;
    private final String attribute;
    private final float value;

    public IpsoSolutionElement(IpsoEquipmentType equipmentType, String name, String attribute, float value) {
        this.equipmentType = equipmentType;
        this.name = name;
        this.attribute = attribute;
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("TYPE:%s NAME:%s, ATTRIBUTE:%s  VALUE:%s", equipmentType.toString(), name, attribute, value);
    }
}
