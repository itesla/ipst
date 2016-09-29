/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import eu.itesla_project.cta.model.IpsoComponent;
import eu.itesla_project.cta.model.IpsoEquipment;
import eu.itesla_project.cta.model.IpsoOneConnectionEquipment;
import eu.itesla_project.cta.model.ValidationType;

import java.util.List;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoInvalidComponent<T extends IpsoEquipment> extends IpsoComponent{
    protected static final String CONNECTED_TO = " connected to ";
    protected static final String INVALID_COMPONENT = "Invalid ";
    protected static final String CAUSED_BY = " caused by ";
    private final Class<T> ipsoComponentClass;
    private final float unvalidatedValue;
    private ValidationType validationType;
    private IpsoEquipment ipsoEquipment;
    private String id;

    /**
     * Constructor
     */
    public IpsoInvalidComponent(IpsoEquipment ipsoEquipment, ValidationType validationType, Class<T> ipsoComponentClass, float unvalidatedValue) {
        super(IpsoIdUtil.getNextUniqueId(), 0);
        this.ipsoEquipment = ipsoEquipment;
        this.validationType = validationType;
        this.ipsoComponentClass = ipsoComponentClass;
        this.unvalidatedValue = unvalidatedValue;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append(INVALID_COMPONENT)
                .append(ipsoComponentClass.getSimpleName())
                .append(getEquipmentDescription())
                .append(findConnectedEquipmentDescription())
                .append(CAUSED_BY)
                .append(validationType.name())
                .append(findValidationValueDescription())
                .toString();
    }

    public String getEquipmentDescription() {
        return String.format(" for %s %s%s", this.ipsoEquipment.getClass().getSimpleName(), ipsoEquipment.getId(), getIidmIdDescriptionFor(ipsoEquipment));
    }

    private String getIidmIdDescriptionFor(IpsoEquipment ipsoEquipment) {
        return String.format(" (%s) ", ipsoEquipment.getIidmId());
    }

    private String findConnectedEquipmentDescription() {
        if (ipsoEquipment instanceof IpsoOneConnectionEquipment) {
            IpsoOneConnectionEquipment oneConnectionEquipment = (IpsoOneConnectionEquipment)ipsoEquipment;
            return new StringBuilder(CONNECTED_TO)
                    .append(oneConnectionEquipment.getConnectedNode().getId())
                    .append(getIidmIdDescriptionFor(oneConnectionEquipment.getConnectedNode())).toString();

        } else {
            return "";
        }
    }

    private String findValidationValueDescription() {
        return String.format(" (%f) ", unvalidatedValue);
    }

    @Override
    public List<Object> getOrderedValues() {
        return null;
    }

    @Override
    public List<String> getOrderedHeaders() {
        return null;
    }
}
